package com.transactions.gmailtracker.repository;

import java.math.BigDecimal;

public interface CategorySpendProjection {
    Integer getCategory();       // Matches "AS category" in the query
    BigDecimal getTotalSpent();  // Matches "AS totalSpent" in the query
}
