package com.transactions.gmailtracker.service;

import com.transactions.gmailtracker.dto.AggregatesDTO;
import com.transactions.gmailtracker.dto.PaginationDTO;
import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.dto.TransactionResponseDTO;
import com.transactions.gmailtracker.entity.EmailData;
import com.transactions.gmailtracker.mapper.TransactionMapper;
import com.transactions.gmailtracker.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    @Autowired
    private final TransactionRepository transactionRepository;

    @Autowired
    private final TransactionMapper transactionMapper;

    public TransactionResponseDTO getRecentTransaction(int limit, int offset){
        Pageable pagable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC,"date"));
        Page<EmailData> page = transactionRepository.findAll(pagable);

        List<TransactionDTO> transactionDTOS = page.getContent().stream()
                .map(transactionMapper::toDto)
                .toList();

        PaginationDTO paginationDTO = PaginationDTO.builder()
                .total((int)page.getTotalElements())
                .limit(limit)
                .offset(offset)
                .hasMore(page.hasNext())
                .build();

        AggregatesDTO aggregatesDTO = AggregatesDTO.builder()
                .totalIncome(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .build();

        return TransactionResponseDTO.builder()
                .transactions(transactionDTOS)
                .paginationDTO(paginationDTO)
                .aggregatesDTO(aggregatesDTO)
                .build();

    }
}
