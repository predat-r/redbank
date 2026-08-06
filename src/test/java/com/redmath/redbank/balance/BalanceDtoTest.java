package com.redmath.redbank.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.balance.dto.BalanceDto;
import com.redmath.redbank.transaction.BankTransaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BalanceDtoTest {

  @Test
  @DisplayName("BalanceDto.from(null) returns DTO with zero balances")
  void fromNullBalance() {
    BalanceDto dto = BalanceDto.from(null);
    assertNull(dto.getId());
    assertNull(dto.getAccountHolderId());
    assertNull(dto.getTransactionId());
    assertNull(dto.getEntryDate());
    assertNull(dto.getIndicator());
    assertEquals(BigDecimal.ZERO, dto.getAmount());
    assertEquals(BigDecimal.ZERO, dto.getRunningBalance());
  }

  @Test
  @DisplayName("BalanceDto.from(balance) handles null nested fields")
  void fromBalanceWithNullNestedFields() {
    Balance balance = new Balance();
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setIndicator(BalanceIndicator.CREDIT);

    BalanceDto dto = BalanceDto.from(balance);
    assertNull(dto.getId());
    assertNull(dto.getAccountHolderId());
    assertNull(dto.getTransactionId());
    assertEquals(balance.getEntryDate(), dto.getEntryDate());
    assertEquals(BalanceIndicator.CREDIT, dto.getIndicator());
    assertEquals(BigDecimal.ZERO, dto.getAmount());
    assertEquals(BigDecimal.ZERO, dto.getRunningBalance());
  }

  @Test
  @DisplayName("BalanceDto.from(balance) populates all fields")
  void fromBalanceComplete() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(10L);

    BankTransaction transaction = new BankTransaction();
    transaction.setId(20L);

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransaction(transaction);
    balance.setEntryDate(now);
    balance.setAmount(new BigDecimal("100.50"));
    balance.setIndicator(BalanceIndicator.DEBIT);
    balance.setRunningBalance(new BigDecimal("499.50"));

    BalanceDto dto = BalanceDto.from(balance);
    assertEquals(10L, dto.getAccountHolderId());
    assertEquals(20L, dto.getTransactionId());
    assertEquals(now, dto.getEntryDate());
    assertEquals(new BigDecimal("100.50"), dto.getAmount());
    assertEquals(BalanceIndicator.DEBIT, dto.getIndicator());
    assertEquals(new BigDecimal("499.50"), dto.getRunningBalance());
  }
}
