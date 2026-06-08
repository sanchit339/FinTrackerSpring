package com.transactions.gmailtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AggregatesDTO {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
}
