package com.redmath.redbank.transaction;

import com.redmath.redbank.transaction.dto.BankTransactionDto;
import com.redmath.redbank.transaction.request.DepositRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
      Authentication authentication,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<BankTransaction> transactions = bankTransactionService.getTransactionsForUser(
        authentication.getName(), pageable);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }

  @PostMapping("/transfers")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public ResponseEntity<BankTransactionDto> createTransfer(
      Authentication authentication,
      @Valid @RequestBody TransferRequest request) {
    BankTransaction transaction = bankTransactionService.transfer(authentication.getName(),
        request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @PostMapping("/deposits")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<BankTransactionDto> createDeposit(
      @Valid @RequestBody DepositRequest request) {
    BankTransaction transaction = bankTransactionService.deposit(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @PostMapping("/withdrawals")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public ResponseEntity<BankTransactionDto> createWithdrawal(
      @Valid @RequestBody WithdrawalRequest request) {
    BankTransaction transaction = bankTransactionService.withdraw(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }
}
