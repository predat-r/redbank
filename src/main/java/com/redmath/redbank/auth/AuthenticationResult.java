package com.redmath.redbank.auth;

import com.redmath.redbank.auth.dto.LoginResponse;

public record AuthenticationResult(LoginResponse response, String refreshToken) {

}
