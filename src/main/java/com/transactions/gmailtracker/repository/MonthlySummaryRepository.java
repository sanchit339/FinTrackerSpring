package com.transactions.gmailtracker.repository;

import com.transactions.gmailtracker.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, Long> {
    Optional<MonthlySummary> findByYearMonth(YearMonth yearMonth);
}
