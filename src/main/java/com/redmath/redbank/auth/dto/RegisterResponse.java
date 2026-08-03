package com.redmath.redbank.auth.dto;

import com.redmath.redbank.user.UserStatus;

public record RegisterResponse(
    Long id,
    String email,
    UserStatus status
) {
}