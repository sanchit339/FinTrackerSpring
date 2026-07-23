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

    public List<TransactionDTO> fetchEmailsSince(String accessToken, String userId, String sinceDate, int maxResults) throws Exception{
        try {
            Gmail gmail = this.buildGmailClient(accessToken);
            LocalDate adjustDate = LocalDate.parse(sinceDate).minusDays(1);
            String dateStr = adjustDate.toString().replace("-", "/");

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
        TransactionDTO transactionDTO = new TransactionDTO();

        // Amount
        Matcher amount = Pattern.compile("Rs\\.([\\d,]+\\.\\d{2})").matcher(emailBody);
        if (amount.find()) transactionDTO.setAmount(Double.valueOf(amount.group(1).replace(",", "")));

        // Account ending
        Matcher account = Pattern.compile("account ending (\\d+)").matcher(emailBody);
        if (account.find()) transactionDTO.setBankAcc(account.group(1));

        // VPA (UPI ID)
        Matcher vpa = Pattern.compile("towards VPA ([\\w.\\-@]+)").matcher(emailBody);
        if (vpa.find()) transactionDTO.setUpiId(vpa.group(1));

        // Recipient name
        Matcher name = Pattern.compile("\\(([A-Z ]+)\\)").matcher(emailBody);
        if (name.find()) transactionDTO.setRecipient(name.group(1));

        // Type of Transaction: CREDIT / DEBIT (from email body words)
        Matcher type = Pattern.compile("(?i)\\b(credited|debited)\\b").matcher(emailBody);
        if (type.find()) {
            String matched = type.group(1).toLowerCase(Locale.ENGLISH);
            transactionDTO.setType(matched.equals("credited") ? "CREDIT" : "DEBIT");
        }

        // Transaction time comes from Gmail message (not email body text)
        if (transactionTime != null) {
            transactionDTO.setTransactionTime(transactionTime);
            transactionDTO.setDate(transactionTime.toLocalDate());
            transactionDTO.setTime(transactionTime.format(
                    DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)));
        }

        return transactionDTO;
    }
}
