package com.redmath.redbank.account.dto;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.user.dto.UserDto;
import java.time.OffsetDateTime;
import lombok.Getter;

@Getter
public class AccountHolderDto {

  Long id;
  UserDto user;
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
      dto.user = UserDto.from(accountHolder.getUser());
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
