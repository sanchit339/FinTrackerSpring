package com.transactions.gmailtracker.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table( name = "emails")
public class EmailData {
    @Id
    @GeneratedValue
    @Column(name = "message_id", unique = true, nullable = false)
    private Long messageId;

    private Integer amount;

    @Column(name = "upi_id")
    private String upiId;
    private String recipient;
    private Date date;

    @Column(name = "bank_acc")
    private String bankAcc;
}
