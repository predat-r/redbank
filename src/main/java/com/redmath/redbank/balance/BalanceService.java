package com.redmath.redbank.balance;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.common.exception.InsufficientFundsException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {

  private final BalanceRepository balanceRepository;
  private final AccountHolderRepository accountHolderRepository;

  public BalanceService(BalanceRepository balanceRepository,
      AccountHolderRepository accountHolderRepository) {
    this.balanceRepository = balanceRepository;
    this.accountHolderRepository = accountHolderRepository;
  }

  public Balance getLatestBalanceByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    return balanceRepository.getLatestBalanceByUserId(userId)
        .orElseGet(this::newZeroBalanceEntry);
  }

  public Balance getBalanceByTransactionId(Long transactionId) {
    if (transactionId == null) {
      throw new IllegalArgumentException("Transaction id is required");
    }
    return balanceRepository.getBalanceByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Balance not found for transaction id: " + transactionId));
  }

  @Transactional
  public void recordLedgerEntry(AccountHolder accountHolder,
      BankTransaction transaction,
      BalanceIndicator indicator) {
    if (accountHolder == null || accountHolder.getId() == null) {
      throw new IllegalArgumentException("Account holder is required");
    }
    if (transaction == null || transaction.getAmount() == null) {
      throw new IllegalArgumentException("Transaction with amount is required");
    }
    if (indicator == null) {
      throw new IllegalArgumentException("Balance indicator is required");
    }
    if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Transaction amount must be positive");
    }

    // Acquire pessimistic write lock on the account holder row to serialize concurrent
    // balance mutations. This prevents Read-Modify-Write race conditions where two
    // threads read the same previousRunningBalance and overwrite each other's updates.
    // Callers that already hold this lock (e.g. transfer, deposit) simply re-acquire
    // within the same transaction (no-op). Callers that don't (e.g. completePendingTransaction)
    // are now protected as well.
    accountHolderRepository.findByIdWithLock(accountHolder.getId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Account holder not found: " + accountHolder.getId()));

    BigDecimal previousRunningBalance = balanceRepository.getLatestBalanceByAccountHolderId(
            accountHolder.getId())
        .map(Balance::getRunningBalance)
        .orElse(BigDecimal.ZERO);

    if (indicator == BalanceIndicator.DEBIT
        && previousRunningBalance.compareTo(transaction.getAmount()) < 0) {
      throw new InsufficientFundsException("Insufficient funds for this transaction");
    }

    BigDecimal newRunningBalance = (indicator == BalanceIndicator.CREDIT)
        ? previousRunningBalance.add(transaction.getAmount())
        : previousRunningBalance.subtract(transaction.getAmount());

    Balance entry = new Balance();
    entry.setAccountHolder(accountHolder);
    entry.setTransaction(transaction);
    entry.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    entry.setAmount(transaction.getAmount());
    entry.setIndicator(indicator);
    entry.setRunningBalance(newRunningBalance);
    balanceRepository.save(entry);
  }

  private Balance newZeroBalanceEntry() {
    Balance balance = new Balance();
    balance.setAmount(BigDecimal.ZERO);
    balance.setRunningBalance(BigDecimal.ZERO);
    return balance;
  }
}
