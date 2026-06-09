package com.transactions.gmailtracker.controller;

import com.transactions.gmailtracker.dto.TransactionResponseDTO;
import com.transactions.gmailtracker.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/banking")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionResponseDTO> getTransaction(){
        TransactionResponseDTO response = transactionService.getTransaction();
        return  ResponseEntity.ok(response);
    }

}
