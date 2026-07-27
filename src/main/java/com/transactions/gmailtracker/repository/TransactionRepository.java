package com.transactions.gmailtracker.repository;

import com.transactions.gmailtracker.entity.EmailData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<EmailData, Long> {
    Page<EmailData> findAllByOrderByTransactionTimeDesc(Pageable pageable);

    // For Monthly Summary
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EmailData e WHERE e.type = :type AND e.transactionTime BETWEEN :start and :end")
    BigDecimal sumByTypeAndDateRange(
            @Param("type") String debit,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // For Monthly Summary
    @Query("SELECT COUNT(e) FROM EmailData e WHERE e.transactionTime BETWEEN :start and :end")
    long countByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // For Base Query On HomePage
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EmailData e WHERE e.type = :type")
    BigDecimal sumByType(@Param("type") String type);


    @Query("SELECT e.category AS category, COALESCE(SUM(e.amount),0) AS totalSpent FROM EmailData e WHERE e.type = :type AND e.transactionTime BETWEEN :start AND :end GROUP BY e.category")
    List<CategorySpendProjection> sumByCategoryAndDateRange(
            @Param("type") String debit,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
