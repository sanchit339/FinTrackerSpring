package com.transactions.gmailtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TransactionDTO {
    private Long id;
    private Double amount;
    private String upiId;
    private String recipient;
    private Date date;
    private String bankAcc;
    private String time;
    private String categoryName;
}
