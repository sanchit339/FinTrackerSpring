package com.transactions.gmailtracker.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryDTO {
    private YearMonth yearMonth;
    private BigDecimal totalReceived;
    private  BigDecimal totalSpent;
    private Long transactionCount;
    private LocalDateTime lastComputeTime;
    private Map<Integer, BigDecimal> categorySpendings;
}
