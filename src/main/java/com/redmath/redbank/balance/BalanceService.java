package com.redmath.redbank.balance;

import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderService;
import com.redmath.redbank.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {

  private final BalanceRepository balanceRepository;

  public BalanceService(BalanceRepository balanceRepository,
      AccountHolderService accountHolderService) {
    this.balanceRepository = balanceRepository;
  }

  Balance getLatestBalanceByUserId(Long userId) {
    return balanceRepository.getLatestBalanceByUserId(userId);
  }

  Balance getLatestBalanceByAccountHolderId(Long accountId) {
    return balanceRepository.getLatestBalanceByAccountHolderId(accountId);
  }

  Balance getBalanceByTransactionId(Long transactionId) {
    return balanceRepository.getBalanceByTransactionId(transactionId);
  }

  @Transactional
  public Balance recordLedgerEntry(AccountHolder accountHolder,
      BankTransaction transaction,
      BalanceIndicator indicator) {

    Balance previousBalance = balanceRepository.getLatestBalanceByAccountHolderId(
        accountHolder.getId());

    BigDecimal newRunningBalance = (indicator == BalanceIndicator.CREDIT)
        ? previousBalance.getAmount().add(transaction.getAmount())
        : previousBalance.getAmount().subtract(transaction.getAmount());

    //TODO: Handle exception related to insufficient funds

    Balance entry = new Balance();
    entry.setAccountHolder(accountHolder);
    entry.setTransaction(transaction);
    entry.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    entry.setAmount(transaction.getAmount());
    entry.setIndicator(indicator);
    entry.setRunningBalance(newRunningBalance);

    return balanceRepository.save(entry);
  }
}
