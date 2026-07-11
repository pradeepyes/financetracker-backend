package com.pradeep.financetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String type;
    private String category;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
}