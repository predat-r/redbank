package com.redmath.redbank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/me/transactions")
public class BankTransactionController {

    private final BankTransactionService bankTransactionService;

    public BankTransactionController(BankTransactionService bankTransactionService) {
        this.bankTransactionService = bankTransactionService;
    }

    @GetMapping
    public ResponseEntity<Page<BankTransactionDto>> getMyTransactions(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Page<BankTransaction> transactions = bankTransactionService.getTransactionsForUser(
                authentication.getName(), page, size);
        return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
    }
}
