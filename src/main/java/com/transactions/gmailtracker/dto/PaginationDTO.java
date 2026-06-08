package com.transactions.gmailtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PaginationDTO {
    private Integer total;
    private Integer limit;
    private Integer offset;
    private boolean hasMore;
}
