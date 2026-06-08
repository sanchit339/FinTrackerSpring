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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GmailService {

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
            String dateStr = adjustDate.toString().replace("-" , "/");

            String query = "from:alerts@hdfcbank.bank.in after:"+dateStr;

            ListMessagesResponse response = gmail.users().messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults((long) maxResults)
                    .execute();

            List<Message> messages = response.getMessages();
            if(messages == null) return new ArrayList<>();

            List<TransactionDTO> emails = messages.parallelStream()
                    .map(e -> {
                        try {
                            Message message = gmail.users().messages()
                                    .get(userId, e.getId())
                                    .setFormat("full")
                                    .execute();

                            Instant receivedAt = Instant.ofEpochMilli(message.getInternalDate());

                            String time = receivedAt.atZone(ZoneId.of("Asia/Kolkata"))
                                    .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

                            TransactionDTO emailDataDTO = parseTransaction(extractBody(message.getPayload()), time);

//                            parsed.put("time", time);

                            return emailDataDTO;
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .collect(Collectors.toList());

            return emails;
        } catch (Exception e) {
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

    public TransactionDTO parseTransaction(String emailBody, String time) {
        TransactionDTO transactionDTO = new TransactionDTO();

        // Amount
        Matcher amount = Pattern.compile("Rs\\.([\\d,]+\\.\\d{2})").matcher(emailBody);
        if (amount.find()) transactionDTO.setAmount(Double.valueOf(amount.group(1)));

        // Account ending
        Matcher account = Pattern.compile("account ending (\\d+)").matcher(emailBody);
        if (account.find()) transactionDTO.setBankAcc(account.group(1));

        // VPA (UPI ID)
        Matcher vpa = Pattern.compile("towards VPA ([\\w.\\-@]+)").matcher(emailBody);
        if (vpa.find()) transactionDTO.setUpiId(vpa.group(1));

        // Recipient name
        Matcher name = Pattern.compile("\\(([A-Z ]+)\\)").matcher(emailBody);
        if (name.find()) transactionDTO.setRecipient(name.group(1));

        // Date
        Matcher date = Pattern.compile("on (\\d{2}-\\d{2}-\\d{2})").matcher(emailBody);
        if (date.find()) transactionDTO.setDate(null);

        transactionDTO.setTime(time);

//        // UPI ref
//        Matcher ref = Pattern.compile("reference no\\.:\\s*(\\d+)").matcher(emailBody);
//        if (ref.find()) result.put("upiRef", ref.group(1));

        return transactionDTO;
    }
}
