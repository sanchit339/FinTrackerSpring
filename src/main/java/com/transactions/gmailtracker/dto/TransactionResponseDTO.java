package com.transactions.gmailtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    private List<TransactionDTO> transactions;
    private  PaginationDTO paginationDTO;
    private AggregatesDTO aggregatesDTO;
}
