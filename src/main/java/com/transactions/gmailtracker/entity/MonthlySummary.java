package com.transactions.gmailtracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "monthly_summary")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummary {
    @Id
    @GeneratedValue
    @Column(unique = true, nullable = false)
    private long id;

    @Column(name = "year_month", unique = true)
    private YearMonth yearMonth;

    @Column(name = "total_received")
    private BigDecimal totalReceived;

    @Column(name = "total_spent")
    private  BigDecimal totalSpent;

    @Column(name = "transaction_count")
    private int transactionCount;

    @Column(name = "updated_at")
    private LocalDateTime lastComputeTime;

    @ElementCollection
    @CollectionTable(name = "monthly_summary_categories", joinColumns = @JoinColumn(name = "monthly_summary_id"))
    @MapKeyColumn(name = "category_id")
    @Column(name = "total_spent")
    private Map<Integer, BigDecimal> categorySpendings = new HashMap<>();

}
