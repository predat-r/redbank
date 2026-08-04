package com.redmath.redbank.transaction.admin;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.dto.BankTransactionDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/transactions")
@SecurityRequirement(name = "bearerAuth")
public class AdminBankTransactionController {

  private final BankTransactionService bankTransactionService;

  public AdminBankTransactionController(BankTransactionService bankTransactionService) {
    this.bankTransactionService = bankTransactionService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<BankTransactionDto>> getAllTransactions(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {
    Page<BankTransaction> transactions = bankTransactionService.getAllTransactions(page, size);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }
}
