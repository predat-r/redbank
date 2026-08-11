package com.redmath.redbank.locationrisk.login.dto;

import java.time.Instant;

public record LoginEventDto(
    Long userId,
    String ipAddress,
    String userAgent,
    String deviceIdentifier,
    Boolean successful,
    String failureReason,
    String city,
    String country,
    String accessTokenJti,
    Instant expiresAt
) {

}