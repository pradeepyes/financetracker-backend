package com.pradeep.financetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BudgetResponse {
    private Long id;
    private String category;
    private BigDecimal limitAmount;
    private String month;
    private BigDecimal spentAmount;   // calculated: sum of expenses in that category/month
    private boolean overBudget;       // calculated: spentAmount > limitAmount
}