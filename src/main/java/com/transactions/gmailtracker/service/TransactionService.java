package com.transactions.gmailtracker.service;

import com.transactions.gmailtracker.dto.*;
import com.transactions.gmailtracker.entity.EmailData;
import com.transactions.gmailtracker.entity.MonthlySummary;
import com.transactions.gmailtracker.mapper.TransactionMapper;
import com.transactions.gmailtracker.repository.MonthlySummaryRepository;
import com.transactions.gmailtracker.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final MonthlySummaryRepository monthlySummaryRepository;

    public TransactionResponseDTO getRecentTransaction(int limit, int offset){
        Pageable pagable = PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "transactionTime"));
        Page<EmailData> page = transactionRepository.findAll(pagable);

        LocalDateTime startDate = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endDate = LocalDateTime.now();

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
                .totalIncome(transactionRepository.sumByTypeAndDateRange("CREDIT" , startDate , endDate))
                .totalExpenses(transactionRepository.sumByTypeAndDateRange("DEBIT", startDate, endDate))
                .totalTransaction(transactionRepository.countByDateRange(startDate, endDate))
                .build();

        return TransactionResponseDTO.builder()
                .transactions(transactionDTOS)
                .paginationDTO(paginationDTO)
                .aggregatesDTO(aggregatesDTO)
                .build();

    }

    public void generateMonthlySummary(LocalDateTime start, LocalDate end) {
        LocalDateTime startDate = start;
        LocalDateTime endDate = end.atTime(23, 59, 57);

        BigDecimal totalSpent = transactionRepository.sumByTypeAndDateRange("DEBIT", startDate , endDate);
        BigDecimal totalReceived = transactionRepository.sumByTypeAndDateRange("CREDIT", startDate , endDate);
        Long transactionCount = transactionRepository.countByDateRange(startDate , endDate);

        log.info("===== Monthly Summary {} to {} =====" , start , end);
        log.info("Total expense   : ${}", totalSpent);
        log.info("Total Income    : ${}", totalReceived);
        log.info("Total Transaction : {}",transactionCount);

        MonthlySummaryDTO monthlySummaryDTO = MonthlySummaryDTO.builder()
                .totalReceived(totalReceived)
                .totalSpent(totalSpent)
                .yearMonth(YearMonth.from(startDate))
                .transactionCount(transactionCount)
                .lastComputeTime(LocalDateTime.now())
                .build();

        MonthlySummary monthlySummary = transactionMapper.toEntity(monthlySummaryDTO);
        monthlySummaryRepository.save(monthlySummary);
    }

    public void updateTransactionCategory(long id, int categoryId) {
        EmailData transaction = transactionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Transaction Not found"));
        transaction.setCategory(categoryId);
    }
}
