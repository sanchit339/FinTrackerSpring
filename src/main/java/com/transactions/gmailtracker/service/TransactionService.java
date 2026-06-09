package com.transactions.gmailtracker.service;

import com.transactions.gmailtracker.dto.TransactionResponseDTO;
import com.transactions.gmailtracker.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionResponseDTO getTransaction(){

        return transactionResponseDTO;
    }

}
