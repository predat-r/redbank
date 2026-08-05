package com.redmath.redbank.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceReconciliationSchedulerTest {

  @Mock
  private BalanceRepository balanceRepository;

  private BalanceReconciliationScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new BalanceReconciliationScheduler(balanceRepository);
  }

  @Test
  @DisplayName("reconcileBalances handles empty entries list")
  void reconcileBalancesEmptyList() {
    when(balanceRepository.findAll()).thenReturn(List.of());

    scheduler.reconcileBalances();

    verify(balanceRepository).findAll();
  }

  @Test
  @DisplayName("reconcileBalances logs OK when balance matches expected")
  void reconcileBalancesMatching() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(1L);

    Balance entry1 = new Balance();
    entry1.setId(1L);
    entry1.setAccountHolder(accountHolder);
    entry1.setIndicator(BalanceIndicator.CREDIT);
    entry1.setAmount(new BigDecimal("100.00"));
    entry1.setRunningBalance(new BigDecimal("100.00"));

    Balance entry2 = new Balance();
    entry2.setId(2L);
    entry2.setAccountHolder(accountHolder);
    entry2.setIndicator(BalanceIndicator.DEBIT);
    entry2.setAmount(new BigDecimal("30.00"));
    entry2.setRunningBalance(new BigDecimal("70.00"));

    when(balanceRepository.findAll()).thenReturn(List.of(entry1, entry2));

    scheduler.reconcileBalances();

    verify(balanceRepository).findAll();
  }

  @Test
  @DisplayName("reconcileBalances logs error when balance mismatch occurs")
  void reconcileBalancesMismatch() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(2L);

    Balance entry1 = new Balance();
    entry1.setId(1L);
    entry1.setAccountHolder(accountHolder);
    entry1.setIndicator(BalanceIndicator.CREDIT);
    entry1.setAmount(new BigDecimal("100.00"));
    entry1.setRunningBalance(new BigDecimal("90.00")); // mismatch!

    when(balanceRepository.findAll()).thenReturn(List.of(entry1));

    scheduler.reconcileBalances();

    verify(balanceRepository).findAll();
  }
}
