package com.transactions.gmailtracker.controller;

import com.transactions.gmailtracker.dto.TransactionResponseDTO;
import com.transactions.gmailtracker.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions/")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/recent")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset){
        TransactionResponseDTO response = transactionService.getRecentTransaction(limit, offset);
        return  ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/category")
    public void updateCategory(@PathVariable int id, @RequestParam int categoryId){
        try{
            transactionService.updateTransactionCategory(id , categoryId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
