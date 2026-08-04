package com.redmath.redbank.account_holder;

import com.redmath.redbank.user.User;
import java.time.OffsetDateTime;

public class AccountHolderDto {

  Long id;
  User user;
  String accountNumber;
  String currency;
  AccountStatus accountStatus;
  OffsetDateTime approvedAt;
  OffsetDateTime createdAt;
  OffsetDateTime updatedAt;

  public static AccountHolderDto from(AccountHolder accountHolder) {
    AccountHolderDto dto = new AccountHolderDto();
    dto.id = accountHolder.getId();
    dto.user = accountHolder.getUser();
    dto.accountNumber = accountHolder.getAccountNumber();
    dto.currency = accountHolder.getCurrency();
    dto.accountStatus = accountHolder.getAccountStatus();
    dto.approvedAt = accountHolder.getApprovedAt();
    dto.createdAt = accountHolder.getCreatedAt();
    dto.updatedAt = accountHolder.getUpdatedAt();
    return dto;
  }
}
