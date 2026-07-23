package com.transactions.gmailtracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailData {
    @Id
    @GeneratedValue
    @Column(name = "message_id", unique = true, nullable = false)
    private Long messageId;

    private Integer amount;

    @Column(name = "upi_id")
    private String upiId;
    private String recipient;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "bank_acc")
    private String bankAcc;

    private String type;
}
