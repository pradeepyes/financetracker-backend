package com.pradeep.financetracker.controller;

import com.pradeep.financetracker.dto.BudgetRequest;
import com.pradeep.financetracker.dto.BudgetResponse;
import com.pradeep.financetracker.model.Budget;
import com.pradeep.financetracker.model.Transaction;
import com.pradeep.financetracker.model.User;
import com.pradeep.financetracker.repository.BudgetRepository;
import com.pradeep.financetracker.repository.TransactionRepository;
import com.pradeep.financetracker.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Calculates how much has actually been spent in a category for a given month
    private BigDecimal calculateSpent(Long userId, String category, String month) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        return transactions.stream()
                .filter(t -> t.getType().equals("EXPENSE"))
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .filter(t -> t.getDate().toString().startsWith(month)) // date starts with "2026-07"
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BudgetResponse toResponse(Budget budget, Long userId) {
        BigDecimal spent = calculateSpent(userId, budget.getCategory(), budget.getMonth());
        boolean overBudget = spent.compareTo(budget.getLimitAmount()) > 0;

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory(),
                budget.getLimitAmount(),
                budget.getMonth(),
                spent,
                overBudget
        );
    }

    // CREATE a budget
    @PostMapping
    public ResponseEntity<?> createBudget(@Valid @RequestBody BudgetRequest request,
                                           Authentication authentication) {
        User user = getCurrentUser(authentication);

        // Prevent duplicate budgets for the same category+month
        boolean exists = budgetRepository
                .findByUserIdAndCategoryAndMonth(user.getId(), request.getCategory(), request.getMonth())
                .isPresent();

        if (exists) {
            return ResponseEntity.badRequest().body("A budget for this category and month already exists");
        }

        Budget budget = new Budget();
        budget.setCategory(request.getCategory());
        budget.setLimitAmount(request.getLimitAmount());
        budget.setMonth(request.getMonth());
        budget.setUser(user);

        Budget saved = budgetRepository.save(budget);

        return ResponseEntity.ok(toResponse(saved, user.getId()));
    }

    // GET all budgets for a given month, with live spend calculation
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(@RequestParam String month,
                                                             Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<BudgetResponse> budgets = budgetRepository.findByUserIdAndMonth(user.getId(), month)
                .stream()
                .map(b -> toResponse(b, user.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(budgets);
    }

    // UPDATE a budget's limit
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBudget(@PathVariable Long id,
                                           @Valid @RequestBody BudgetRequest request,
                                           Authentication authentication) {
        User user = getCurrentUser(authentication);

        Budget budget = budgetRepository.findById(id).orElse(null);

        if (budget == null) {
            return ResponseEntity.status(404).body("Budget not found");
        }
        if (!budget.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You don't have permission to edit this budget");
        }

        budget.setLimitAmount(request.getLimitAmount());
        Budget updated = budgetRepository.save(budget);

        return ResponseEntity.ok(toResponse(updated, user.getId()));
    }

    // DELETE a budget
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Budget budget = budgetRepository.findById(id).orElse(null);

        if (budget == null) {
            return ResponseEntity.status(404).body("Budget not found");
        }
        if (!budget.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You don't have permission to delete this budget");
        }

        budgetRepository.delete(budget);

        return ResponseEntity.ok("Budget deleted successfully");
    }
}