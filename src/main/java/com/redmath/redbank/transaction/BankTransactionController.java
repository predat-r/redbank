package com.redmath.redbank.transaction;

import com.redmath.redbank.transaction.dto.BankTransactionDto;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/me")
public class BankTransactionController {

  private final BankTransactionService bankTransactionService;

  public BankTransactionController(BankTransactionService bankTransactionService) {
    this.bankTransactionService = bankTransactionService;
  }

  @GetMapping("/transactions")
  public ResponseEntity<Page<BankTransactionDto>> getMyTransactions(
      Authentication authentication,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    Page<BankTransaction> transactions = bankTransactionService.getTransactionsForUser(
        authentication.getName(), page, size);
    return ResponseEntity.ok(transactions.map(BankTransactionDto::from));
  }

  @PostMapping("/transfers")
  public ResponseEntity<BankTransactionDto> createTransfer(
      Authentication authentication,
      @Valid @RequestBody TransferRequest request) {
    BankTransaction transaction = bankTransactionService.transfer(authentication.getName(),
        request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @PostMapping("/deposits")
  public ResponseEntity<BankTransactionDto> createDeposit(
      @Valid @RequestBody DepositRequest request) {
    BankTransaction transaction = bankTransactionService.deposit(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

  @PostMapping("/withdrawals")
  public ResponseEntity<BankTransactionDto> createWithdrawal(
      @Valid @RequestBody WithdrawalRequest request) {
    BankTransaction transaction = bankTransactionService.withdraw(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(BankTransactionDto.from(transaction));
  }

}
