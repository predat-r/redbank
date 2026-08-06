package com.redmath.redbank.user.admin.dto;

import com.redmath.redbank.account.dto.AccountHolderDto;

public record CreateUserResponse(
    AdminUserResponse user,
    AccountHolderDto accountHolder
) {

}
