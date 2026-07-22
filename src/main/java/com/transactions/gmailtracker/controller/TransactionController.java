package com.transactions.gmailtracker.controller;

import com.transactions.gmailtracker.dto.TransactionResponseDTO;
import com.transactions.gmailtracker.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banking")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/transactions")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset){
        TransactionResponseDTO response = transactionService.getRecentTransaction(limit, offset);
        return  ResponseEntity.ok(response);
    }
}
