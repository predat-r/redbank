package com.redmath.redbank.balance;

import com.redmath.redbank.balance.Balance.BalanceIndicator;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class BalanceDto {

  Long id;
  Long accountHolderId;
  Long transactionId;
  OffsetDateTime entryDate;
  BigDecimal amount;
  BalanceIndicator indicator;
  BigDecimal runningBalance;

  public static BalanceDto from(Balance balance) {
    BalanceDto dto = new BalanceDto();
    dto.id = balance.getId();
    dto.accountHolderId = balance.getAccountHolder().getId();
    dto.transactionId = balance.getTransaction().getId();
    dto.entryDate = balance.getEntryDate();
    dto.amount = balance.getAmount();
    dto.indicator = balance.getIndicator();
    dto.runningBalance = balance.getRunningBalance();
    return dto;
  }
}