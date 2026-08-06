package com.redmath.redbank.auth;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenCookieService {

  private final String cookieName;
  private final String cookiePath;
  private final String sameSite;
  private final boolean secure;
  private final Duration refreshTokenTtl;

  RefreshTokenCookieService(
      @Value("${app.security.refresh-cookie.name:__Secure-refresh-token}") String cookieName,
      @Value("${app.security.refresh-cookie.path:/api/auth}") String cookiePath,
      @Value("${app.security.refresh-cookie.same-site:None}") String sameSite,
      @Value("${app.security.refresh-cookie.secure:true}") boolean secure,
      @Value("${spring.security.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
    this.cookieName = cookieName;
    this.cookiePath = cookiePath;
    this.sameSite = sameSite;
    this.secure = secure;
    this.refreshTokenTtl = refreshTokenTtl;
  }

  void write(HttpServletResponse response, String refreshToken) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, refreshTokenTtl).toString());
  }

  void clear(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(cookieName, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(cookiePath)
        .maxAge(maxAge)
        .build();
  }
}
