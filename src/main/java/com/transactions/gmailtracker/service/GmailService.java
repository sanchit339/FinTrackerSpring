package com.transactions.gmailtracker.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.entity.EmailData;
import com.transactions.gmailtracker.mapper.TransactionMapper;
import com.transactions.gmailtracker.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
public class GmailService {
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionMapper transactionMapper;

    private final ExecutorService gmailExecutor = Executors.newFixedThreadPool(10);

    private Gmail buildGmailClient(String accessToken) throws Exception{
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
                )
                .setApplicationName("FinTracker").build();
    }

    public List<TransactionDTO> fetchEmailsSince(String accessToken, String userId, LocalDate sinceDate, int maxResults) throws Exception{
        try {
            Gmail gmail = this.buildGmailClient(accessToken);
            String dateStr = sinceDate.toString().replace("-", "/");

            String query = "from:alerts@hdfcbank.bank.in after:" + dateStr;

            ListMessagesResponse response = gmail.users().messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults((long) maxResults)
                    .execute();
            List<Message> messages = response.getMessages();
            if (messages == null) return new ArrayList<>();

            //replaced the parallel-stream to stream to avoid 429 in the free tier
            List<TransactionDTO> emails = messages.stream()
                    .map(e -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    Message message = gmail.users().messages()
                                            .get(userId, e.getId())
                                            .setFormat("full")
                                            .execute();

                                    Instant receivedAt = Instant.ofEpochMilli(message.getInternalDate());
                                    // Gmail received time in IST — this is the transaction time we persist
                                    var transactionTime = receivedAt
                                            .atZone(ZoneId.of("Asia/Kolkata"))
                                            .toLocalDateTime();
                                    return parseTransaction(extractBody(message.getPayload()), transactionTime);
                                } catch (IOException ex) {
                                    log.error("Failed to fetch message ID: " + e.getId(), ex);
                                    return null;
                                }
                            }, gmailExecutor))

                            .collect(Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    emailList-> emailList.stream()
                                            .map(CompletableFuture::join)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList())

                            ));

            List<EmailData> entities;

            entities = emails.stream()
                    .map(transactionMapper::toEntity)
                    .collect(Collectors.toList());

            transactionRepository.saveAll(entities);
            return emails;
        } catch (Exception e) {
            log.error("Runtime Error  : {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    public String extractBody(MessagePart payLoad){
        if(payLoad.getBody() != null && payLoad.getBody().getData() != null){
            return decode(payLoad.getBody().getData());
        }

        if(payLoad.getParts() != null){
            String htmlFallback = null;
            for (MessagePart part : payLoad.getParts()) {
                if ("text/plain".equals(part.getMimeType()) && part.getBody().getData() != null) {
                    return decode(part.getBody().getData()); // prefer plain text
                }
                if ("text/html".equals(part.getMimeType()) && part.getBody().getData() != null) {
                    htmlFallback = decode(part.getBody().getData()); // fallback
                }
            }
            if (htmlFallback != null) return stripHtml(htmlFallback);
        }
        return "";
    }


    private String decode(String data) {
        return new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);
    }
    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public TransactionDTO parseTransaction(String emailBody, LocalDateTime transactionTime) {
        return parseTransaction(emailBody, null, transactionTime);
    }

    public TransactionDTO parseTransaction(String emailBody, String subject, LocalDateTime transactionTime) {
        TransactionDTO transactionDTO = new TransactionDTO();
        if (emailBody == null || emailBody.isBlank()) {
            return transactionDTO;
        }
        if (subject == null) subject = "";

        // ── 1. Amount: Rs. 123.00 | Rs.INR 1,234.56 | Rs 123 | INR 123 | ₹123 (try body then subject) ──
        Pattern amountPat = Pattern.compile("(?i)(?:Rs\\.?|INR|₹)\\s*(?:INR\\s*)?([,\\d]+\\.?\\d*)");
        Matcher amountMatcher = amountPat.matcher(emailBody);
        if (!amountMatcher.find()) amountMatcher = amountPat.matcher(subject);
        if (amountMatcher.find()) {
            try {
                transactionDTO.setAmount(Double.parseDouble(amountMatcher.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) {}
        }

        // ── 2. Type: debited | credited | added | withdrawn | deducted | paid | sent | received (body then subject) ──
        Pattern typePat = Pattern.compile("(?i)(debited|credited|added|withdrawn|deducted|paid|sent|received)");
        Matcher typeMatcher = typePat.matcher(emailBody);
        if (!typeMatcher.find()) typeMatcher = typePat.matcher(subject);
        if (typeMatcher.find()) {
            String matched = typeMatcher.group(1).toLowerCase(Locale.ENGLISH);
            switch (matched) {
                case "debited": case "withdrawn": case "deducted": case "paid": case "sent":
                    transactionDTO.setType("DEBIT"); break;
                case "credited": case "added": case "received":
                    transactionDTO.setType("CREDIT"); break;
            }
        }

        // ── 3. Account: "account ending 9791" | "a/c ending in 9791" | "a/c **9791" (body then subject) ──
        Pattern accountPat = Pattern.compile("(?i)(?:account|a/c)\\s+(?:ending\\s+in\\s+|ending\\s+)?([A-Z0-9*X]+)");
        Matcher accountMatcher = accountPat.matcher(emailBody);
        if (!accountMatcher.find()) accountMatcher = accountPat.matcher(subject);
        if (accountMatcher.find()) {
            transactionDTO.setBankAcc(accountMatcher.group(1));
        }

        // ── 4. Recipient / Sender — TS checks recipient FIRST, then sender ──
        //    TS recipient: /(?:to|towards)\s+(?:VPA\s+)?(.+?)(?=\s+on\s+\d)/i
        //    TS sender:    /Sender:\s+([^(\r\n]+?)\s*\(VPA:\s*([^)]+)\)/i
        Matcher recipientMatcher = Pattern.compile("(?i)(?:to|towards)\\s+(?:VPA\\s+)?(.+?)(?=\\s+on\\s+\\d)")
                .matcher(emailBody);
        Matcher senderMatcher = Pattern.compile("(?i)Sender:\\s+([^(\\r\\n]+?)\\s*\\(VPA:\\s*([^)]+)\\)")
                .matcher(emailBody);

        if (recipientMatcher.find()) {
            // Debit format: "to/towards VPA gpay@okaxis on 09-01-26"
            String recipient = recipientMatcher.group(1).trim();
            transactionDTO.setRecipient(recipient);
            if (recipient.contains("@")) {
                transactionDTO.setUpiId(recipient);    // VPA is the recipient string itself
            }
        } else if (senderMatcher.find()) {
            // Credit format: "Sender: JOHN DOE (VPA: john@upi)"
            String senderName = senderMatcher.group(1).trim();
            String senderVpa  = senderMatcher.group(2).trim();
            transactionDTO.setRecipient(senderName);
            transactionDTO.setUpiId(senderVpa);
        }
        // (No parentheses-name fallback in TS — removed to stay aligned)

        // ── 5. Balance: "Avl Bal: Rs. 1,234.56" | "Available balance: Rs 500" ──
        //    TS: /(?:Avl\s+Bal|Available\s+balance)[:\s]*Rs\.?\s*(?:INR\s*)?([,\d]+\.?\d*)/i
        Matcher balanceMatcher = Pattern.compile(
                "(?i)(?:Avl\\s+Bal|Available\\s+balance)[:\\s]*Rs\\.?\\s*(?:INR\\s*)?([,\\d]+\\.?\\d*)")
                .matcher(emailBody);
        if (balanceMatcher.find()) {
            try {
                // Note: TransactionDTO has no balance field — store in upiId is wrong.
                // This is captured for parity; wire to a DTO field when you add one.
                // transactionDTO.setBalance(Double.parseDouble(balanceMatcher.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) {}
        }

        // ── 6. Transaction Date — 6 patterns matching TS exactly ──
        //    TS splits `-` and `/` into separate patterns (6 total):
        //      on DD-MM-YY(YY), on DD/MM/YY(YY)
        //      dated? DD-MM-YY(YY), dated? DD/MM/YY(YY)
        //      [Dd]ate[:\s] DD-MM-YY(YY), [Dd]ate[:\s] DD/MM/YY(YY)
        String[] dateRegexes = {
            "(?i)on\\s+(\\d{2})-(\\d{2})-(\\d{2,4})",
            "(?i)on\\s+(\\d{2})/(\\d{2})/(\\d{2,4})",
            "(?i)dated?\\s+(\\d{2})-(\\d{2})-(\\d{2,4})",
            "(?i)dated?\\s+(\\d{2})/(\\d{2})/(\\d{2,4})",
            "[Dd]ate[:\\s]+(\\d{2})-(\\d{2})-(\\d{2,4})",
            "[Dd]ate[:\\s]+(\\d{2})/(\\d{2})/(\\d{2,4})"
        };

        LocalDate bodyDate = null;
        for (String regex : dateRegexes) {
            Matcher dateMatcher = Pattern.compile(regex).matcher(emailBody);
            if (dateMatcher.find()) {
                try {
                    int day   = Integer.parseInt(dateMatcher.group(1));
                    int month = Integer.parseInt(dateMatcher.group(2));
                    int year  = Integer.parseInt(dateMatcher.group(3));
                    if (year < 100) {
                        year = (year < 50) ? 2000 + year : 1900 + year;
                    }
                    bodyDate = LocalDate.of(year, month, day);
                    break;
                } catch (Exception ignored) {}
            }
        }

        // ── 7. Final timestamp — IST-based combination (mirrors TS +05:30 approach) ──
        //    TS: combines body date with IST time-parts of receivedAt, then parses as +05:30
        //    Java: transactionTime is already in IST (converted via ZoneId.of("Asia/Kolkata") in fetchEmailsSince)
        //    So we simply replace the date component with the body date when found.
        LocalDateTime finalTransactionTime = transactionTime;
        if (bodyDate != null && transactionTime != null) {
            finalTransactionTime = LocalDateTime.of(bodyDate, transactionTime.toLocalTime());
        }
        // Fallback: use raw Gmail received time (already IST) — mirrors TS fallback to receivedAt

        if (finalTransactionTime != null) {
            transactionDTO.setTransactionTime(finalTransactionTime);
            transactionDTO.setDate(finalTransactionTime.toLocalDate());
            transactionDTO.setTime(finalTransactionTime.format(
                    DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)));
        }

        return transactionDTO;
    }
}

