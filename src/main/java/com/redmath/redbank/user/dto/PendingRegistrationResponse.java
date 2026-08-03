package com.redmath.redbank.user.dto;

import com.redmath.redbank.user.UserStatus;
import java.time.Instant;

public record PendingRegistrationResponse(
    Long id,
    String email,
    String phoneNumber,
    String name,
    String address,
    UserStatus status,
    Instant createdAt
) {
}