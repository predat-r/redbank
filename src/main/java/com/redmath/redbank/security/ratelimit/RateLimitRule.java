package com.redmath.redbank.security.ratelimit;

import java.util.Set;

public enum RateLimitRule {

  AUTH(
      RateLimitType.AUTH,
      Set.of(
          "/api/auth/login",
          "/api/auth/register",
          "/api/auth/refresh"
      )
  ),

  CHATBOT(
      RateLimitType.CHATBOT,
      Set.of("/api/accounts/me/chat")
  ),

  FINANCIAL(
      RateLimitType.FINANCIAL,
      Set.of(
          "/api/accounts/me/transfers",
          "/api/accounts/me/withdrawals",
          "/api/admin/deposits",
          "/api/accounts/me/statement"
      )
  );

  private final RateLimitType type;
  private final Set<String> paths;

  RateLimitRule(RateLimitType type, Set<String> paths) {
    this.type = type;
    this.paths = paths;
  }

  public boolean matches(String path) {
    return paths.contains(path);
  }

  public RateLimitType getType() {
    return type;
  }
}