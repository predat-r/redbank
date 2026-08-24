package com.redmath.redbank.balance.dto;

import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
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
    if (balance == null) {
      dto.amount = BigDecimal.ZERO;
      dto.runningBalance = BigDecimal.ZERO;
      return dto;
    }
    dto.id = balance.getId();
    dto.accountHolderId =
        balance.getAccountHolder() != null ? balance.getAccountHolder().getId() : null;
    dto.transactionId = balance.getTransactionId();
    dto.entryDate = balance.getEntryDate();
    dto.amount = balance.getAmount() != null ? balance.getAmount() : BigDecimal.ZERO;
    dto.indicator = balance.getIndicator();
    dto.runningBalance =
        balance.getRunningBalance() != null ? balance.getRunningBalance() : BigDecimal.ZERO;
    return dto;
  }
}