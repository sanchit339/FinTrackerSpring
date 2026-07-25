package com.transactions.gmailtracker.mapper;

import com.transactions.gmailtracker.dto.MonthlySummaryDTO;
import com.transactions.gmailtracker.dto.TransactionDTO;
import com.transactions.gmailtracker.entity.EmailData;
import com.transactions.gmailtracker.entity.MonthlySummary;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "messageId", ignore = true)
    EmailData toEntity(TransactionDTO transactionDTO);

    @Mapping(source = "messageId", target = "id")
    @Mapping(target = "date", ignore = true)
    @Mapping(target = "time", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    TransactionDTO toDto(EmailData emailData);

    @AfterMapping
    default void fillDateAndTime(EmailData emailData, @MappingTarget TransactionDTO.TransactionDTOBuilder dto) {
        LocalDateTime transactionTime = emailData.getTransactionTime();
        if (transactionTime == null) {
            return;
        }
        dto.date(transactionTime.toLocalDate());
        dto.time(transactionTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)));
    }

    MonthlySummary toEntity(MonthlySummaryDTO monthlySummaryDTO);
}
