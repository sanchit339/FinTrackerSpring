package com.transactions.gmailtracker.mapper;

import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.entity.EmailData;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    EmailData toEntity(TransactionDTO transactionDTO);
    TransactionDTO toDto(EmailData emailData);
}
