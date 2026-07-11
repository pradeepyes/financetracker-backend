package com.pradeep.financetracker.controller;

import com.pradeep.financetracker.dto.TransactionRequest;
import com.pradeep.financetracker.dto.TransactionResponse;
import com.pradeep.financetracker.model.Transaction;
import com.pradeep.financetracker.model.User;
import com.pradeep.financetracker.repository.TransactionRepository;
import com.pradeep.financetracker.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper: get the currently logged-in User entity from the JWT-authenticated request
    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getType(), t.getCategory(),
                t.getAmount(), t.getDate(), t.getDescription()
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionRequest request,
                                                Authentication authentication) {
        User user = getCurrentUser(authentication);

        Transaction transaction = new Transaction();
        transaction.setType(request.getType().toUpperCase());
        transaction.setCategory(request.getCategory());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        transaction.setUser(user);

        Transaction saved = transactionRepository.save(transaction);

        return ResponseEntity.ok(toResponse(saved));
    }

    // READ ALL (only this user's transactions)
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(Authentication authentication) {
        User user = getCurrentUser(authentication);

        List<TransactionResponse> transactions = transactionRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactions);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable Long id,
                                                @Valid @RequestBody TransactionRequest request,
                                                Authentication authentication) {
        User user = getCurrentUser(authentication);

        Transaction transaction = transactionRepository.findById(id)
                .orElse(null);

        if (transaction == null) {
            return ResponseEntity.status(404).body("Transaction not found");
        }

        // Security check: make sure this transaction belongs to the logged-in user
        if (!transaction.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You don't have permission to edit this transaction");
        }

        transaction.setType(request.getType().toUpperCase());
        transaction.setCategory(request.getCategory());
        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());

        Transaction updated = transactionRepository.save(transaction);

        return ResponseEntity.ok(toResponse(updated));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);

        Transaction transaction = transactionRepository.findById(id)
                .orElse(null);

        if (transaction == null) {
            return ResponseEntity.status(404).body("Transaction not found");
        }

        if (!transaction.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You don't have permission to delete this transaction");
        }

        transactionRepository.delete(transaction);

        return ResponseEntity.ok("Transaction deleted successfully");
    }
}