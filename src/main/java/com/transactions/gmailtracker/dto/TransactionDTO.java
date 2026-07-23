package com.transactions.gmailtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TransactionDTO {
    private Long id;
    private Double amount;
    private String upiId;
    private String recipient;
    private LocalDate date;
    private String bankAcc;
    private String time;
    private LocalDateTime transactionTime;
    private String categoryName;
    private String type;
}
