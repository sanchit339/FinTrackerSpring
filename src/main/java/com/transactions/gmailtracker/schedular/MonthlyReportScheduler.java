package com.transactions.gmailtracker.schedular;

import com.transactions.gmailtracker.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Slf4j
@Component
public class MonthlyReportScheduler {
    @Autowired
    private TransactionService transactionService;


    @Scheduled(cron = "0 59 23 L * *")
    public void generateMonthlySummary(){
        log.info("Generating monthly summary...");
        YearMonth month = YearMonth.now();

        transactionService.generateMonthlySummary(
                    month.atDay(1).atStartOfDay(),
                    month.atEndOfMonth()
        );

        transactionService.generateMonthlySummaryCategory(
                month.atDay(1).atStartOfDay(),
                month.atEndOfMonth()
        );

        log.info("Monthly summary generation complete.");
    }
}
