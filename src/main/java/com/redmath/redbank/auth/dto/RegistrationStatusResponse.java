package com.redmath.redbank.auth.dto;

import com.redmath.redbank.user.UserStatus;

public record RegistrationStatusResponse(Long userId,
                                         UserStatus status,
                                         String rejectionReason) {

}
