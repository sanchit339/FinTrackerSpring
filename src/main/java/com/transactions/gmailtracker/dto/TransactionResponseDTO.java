package com.transactions.gmailtracker.dto;

import java.util.List;

public class TransactionResponseDTO {
    private List<TransactionDTO> transactions;
    private  PaginationDTO paginationDTO;
    private AggregatesDTO aggregatesDTO;
}
