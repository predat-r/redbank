package com.redmath.redbank.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType
) {
}