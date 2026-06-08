package com.transactions.gmailtracker.controller;


import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.service.GmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EmailController {
    @Autowired
    private GmailService gmailService;

    @Autowired
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Autowired
    private MessageSource messageSource;

    @GetMapping("/emails")
    public ResponseEntity<String> getEmails(Authentication authentication) throws Exception {
        try {
            OAuth2User principal = (OAuth2User) authentication.getPrincipal();
            String userIdStr = principal.getAttribute("sub");

            OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient("google", authentication.getName());
            String accessToken = client.getAccessToken().getTokenValue();

            long start = System.currentTimeMillis();

            List<TransactionDTO> emails = gmailService.fetchEmailsSince(accessToken, userIdStr, "2026-05-02", 20);


            emails.forEach(e ->  {
                System.out.println(e);
            });

            System.out.println("Total time taken : - " + (System.currentTimeMillis() -  start));

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}
