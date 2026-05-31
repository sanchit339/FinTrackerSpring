package com.transactions.gmailtracker.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

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

    public List<Message> fetchEmailsSince(String accessToken, String userId, String sinceDate, int maxResults) throws Exception{
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
            return messages;
//            messages.forEach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
