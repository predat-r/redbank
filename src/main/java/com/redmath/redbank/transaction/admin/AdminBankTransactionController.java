package com.redmath.redbank.transaction.admin;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.dto.AdminBankTransactionDetailDto;
import com.redmath.redbank.transaction.dto.BankTransactionDto;
import com.redmath.redbank.transaction.request.DepositRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminBankTransactionController {

  private final BankTransactionService bankTransactionService;

  public AdminBankTransactionController(BankTransactionService bankTransactionService) {
    this.bankTransactionService = bankTransactionService;
  }

  @GetMapping("/transactions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<BankTransactionDto>> getAllTransactions(
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BankTransaction> transactions = bankTransactionService.getAllTransactions(pageable);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }

  @PostMapping("/deposits")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BankTransactionDto> createDeposit(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody DepositRequest request) {
    BankTransaction transaction = bankTransactionService.deposit(extractUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @GetMapping("/accounts/{accountNumber}/transactions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<BankTransactionDto>> getTransactionsByAccountNumber(
      @PathVariable String accountNumber,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BankTransaction> transactions = bankTransactionService.getTransactionsByAccountNumber(
        accountNumber, pageable);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }

  @GetMapping("/transactions/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminBankTransactionDetailDto> getTransactionById(@PathVariable Long id) {
    BankTransaction transaction = bankTransactionService.getTransactionById(id);
    return ResponseEntity.ok(AdminBankTransactionDetailDto.from(transaction));
  }

  @GetMapping("/transactions/reference/{reference}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminBankTransactionDetailDto> getTransactionByReference(
      @PathVariable String reference) {
    BankTransaction transaction = bankTransactionService.getTransactionByReference(reference);
    return ResponseEntity.ok(AdminBankTransactionDetailDto.from(transaction));
  }

  private Long extractUserId(Jwt jwt) {
    Number userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }
    return userId.longValue();
  }
}
