package com.transactions.gmailtracker.controller;


import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.service.GmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/gmail")
public class EmailController {
    @Autowired
    private GmailService gmailService;

    @Autowired
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    private String[] getAccessToken(Authentication authentication) {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String userIdStr = principal.getAttribute("sub");

        OAuth2AuthorizedClient client = oAuth2AuthorizedClientService.loadAuthorizedClient("google", authentication.getName());
        String accessToken = client.getAccessToken().getTokenValue();

        return new String[]{userIdStr, accessToken};
    }



    @GetMapping("/sync")
    public ResponseEntity<String> getEmails(Authentication authentication, @RequestParam(required = false) LocalDate date) throws Exception {
        if(date == null){
            date = LocalDate.now();
        }
        try {
            String[] gmailClient = getAccessToken(authentication);

            long start = System.currentTimeMillis();

            List<TransactionDTO> emails = gmailService.fetchEmailsSince(gmailClient[1], gmailClient[0] , date, 20);

            emails.forEach(System.out::println);

            System.out.println("Total time taken : - " + (System.currentTimeMillis() -  start));

        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    @GetMapping("/sync-all")
    public ResponseEntity<String> syncAllEmails(Authentication authentication){
        try{
            String[] gmailClient = getAccessToken(authentication);
            LocalDate start = YearMonth.now().atDay(1);
            long startTime = System.currentTimeMillis();

            List<TransactionDTO> emails = gmailService.fetchEmailsSince(gmailClient[1], gmailClient[0], start, 100);

            emails.forEach(System.out::println);
            System.out.println("Total time taken : - " + (System.currentTimeMillis() -  startTime));

        } catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

}
