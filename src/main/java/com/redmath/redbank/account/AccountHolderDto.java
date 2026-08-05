package com.redmath.redbank.account;

import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class AccountHolderDto {

  Long id;
  Long userId;
  String accountNumber;
  String currency;
  AccountStatus accountStatus;
  OffsetDateTime approvedAt;
  OffsetDateTime createdAt;
  OffsetDateTime updatedAt;

  public static AccountHolderDto from(AccountHolder accountHolder) {
    AccountHolderDto dto = new AccountHolderDto();
    dto.id = accountHolder.getId();
    if (accountHolder.getUser() != null) {
      dto.userId = accountHolder.getUser().getId();
    }
    dto.accountNumber = accountHolder.getAccountNumber();
    dto.currency = accountHolder.getCurrency();
    dto.accountStatus = accountHolder.getAccountStatus();
    dto.approvedAt = accountHolder.getApprovedAt();
    dto.createdAt = accountHolder.getCreatedAt();
    dto.updatedAt = accountHolder.getUpdatedAt();
    return dto;
  }
}
