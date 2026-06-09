package com.transactions.gmailtracker.repository;

import com.transactions.gmailtracker.entity.EmailData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<EmailData, Long>{
}
