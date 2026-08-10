package com.redmath.redbank.transaction;

import com.redmath.redbank.common.idempotency.RequireIdempotency;
import com.redmath.redbank.transaction.dto.BankTransactionDetailDto;
import com.redmath.redbank.transaction.dto.BankTransactionDto;
import com.redmath.redbank.transaction.request.TransactionFilterRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/me")
public class BankTransactionController {

  private final BankTransactionService bankTransactionService;

  public BankTransactionController(BankTransactionService bankTransactionService) {
    this.bankTransactionService = bankTransactionService;
  }

  @GetMapping("/transactions")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public ResponseEntity<Page<BankTransactionDto>> getMyTransactions(
      @AuthenticationPrincipal Jwt jwt,
      @ModelAttribute TransactionFilterRequest filter,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Long userId = extractUserId(jwt);
    Page<BankTransaction> transactions = bankTransactionService.getTransactionsForUser(
        userId, filter.getAccountNumber(), filter.getType(), filter.getStatus(),
        filter.getCategory(), filter.getAnomalyFlag(), filter.getFromDate(), filter.getToDate(), pageable);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }

  @GetMapping("/transactions/{id}")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public ResponseEntity<BankTransactionDetailDto> getMyTransactionById(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id) {

    Long userId = extractUserId(jwt);
    BankTransaction transaction = bankTransactionService.getUserTransactionById(userId, id);
    return ResponseEntity.ok(BankTransactionDetailDto.fromDetail(transaction));
  }

  @PostMapping("/transfers")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @RequireIdempotency(required = false)
  public ResponseEntity<BankTransactionDto> createTransfer(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody TransferRequest request) {

    Long userId = extractUserId(jwt);
    BankTransaction transaction = bankTransactionService.transfer(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @PostMapping("/withdrawals")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @RequireIdempotency(required = false)
  public ResponseEntity<BankTransactionDto> createWithdrawal(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody WithdrawalRequest request) {

    Long userId = extractUserId(jwt);
    BankTransaction transaction = bankTransactionService.withdraw(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  private Long extractUserId(Jwt jwt) {
    Number userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }
    return userId.longValue();
  }
}
