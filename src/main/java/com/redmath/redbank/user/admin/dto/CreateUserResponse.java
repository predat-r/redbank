package com.redmath.redbank.user.admin.dto;

public record CreateUserResponse(
    AdminUserResponse user,
    AccountHolderSummaryDto accountHolder
) {

}
