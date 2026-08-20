package com.redmath.redbank.transaction.admin;

import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.common.idempotency.RequireIdempotency;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.dto.AdminBankTransactionDetailDto;
import com.redmath.redbank.transaction.dto.AdminBankTransactionDto;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.RejectTransactionRequest;
import com.redmath.redbank.transaction.request.TransactionFilterRequest;
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
@RequestMapping("/api/admin")
public class AdminBankTransactionController {

  private final BankTransactionService bankTransactionService;
  private final AuditService auditService;

  public AdminBankTransactionController(BankTransactionService bankTransactionService,
      AuditService auditService) {
    this.bankTransactionService = bankTransactionService;
    this.auditService = auditService;
  }

  @GetMapping("/transactions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AdminBankTransactionDto>> getAllTransactions(
      @ModelAttribute TransactionFilterRequest filter,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BankTransaction> transactions = bankTransactionService.getAllTransactions(
        filter.getReference(), filter.getAccountNumber(), filter.getType(), filter.getStatus(),
        filter.getCategory(), filter.getAnomalyFlag(), filter.getFromDate(), filter.getToDate(),
        pageable);
    return ResponseEntity.ok(transactions.map(AdminBankTransactionDto::from));
  }

  @PostMapping("/deposits")
  @PreAuthorize("hasRole('ADMIN')")
  @RequireIdempotency(required = false)
  public ResponseEntity<AdminBankTransactionDto> createDeposit(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody DepositRequest request) {
    BankTransaction transaction = bankTransactionService.deposit(extractUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(AdminBankTransactionDto.from(transaction));
  }

  @GetMapping("/accounts/{accountNumber}/transactions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<AdminBankTransactionDto>> getTransactionsByAccountNumber(
      @PathVariable String accountNumber,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<BankTransaction> transactions = bankTransactionService.getTransactionsByAccountNumber(
        accountNumber, pageable);
    return ResponseEntity.ok(transactions.map(AdminBankTransactionDto::from));
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

  @PostMapping("/transactions/{id}/approve")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminBankTransactionDto> approveTransaction(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id) {
    Long adminUserId = extractUserId(jwt);
    BankTransaction completed = bankTransactionService.completePendingTransaction(id);
    auditService.recordAuditLog(adminUserId, AuditAction.TRANSACTION_APPROVED,
        AuditTargetType.TRANSACTION, id.toString(), null);
    return ResponseEntity.ok(AdminBankTransactionDto.from(completed));
  }

  @PostMapping("/transactions/{id}/reject")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminBankTransactionDto> rejectTransaction(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @RequestBody(required = false) RejectTransactionRequest request) {
    Long adminUserId = extractUserId(jwt);
    String reason = request != null && request.getReason() != null
        ? request.getReason() : "Rejected by admin";
    BankTransaction reversal = bankTransactionService.reverseTransaction(adminUserId, id, reason);
    auditService.recordAuditLog(adminUserId, AuditAction.TRANSACTION_REJECTED,
        AuditTargetType.TRANSACTION, id.toString(), reason);
    return ResponseEntity.ok(AdminBankTransactionDto.from(reversal));
  }

  private Long extractUserId(Jwt jwt) {
    Number userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }
    return userId.longValue();
  }
}

