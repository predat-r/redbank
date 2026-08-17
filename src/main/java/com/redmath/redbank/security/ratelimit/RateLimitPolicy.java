package com.redmath.redbank.security.ratelimit;

import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicy {

  public RateLimitType resolve(String path) {
    if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
      return RateLimitType.AUTH;
    }
    if (path.contains("/chat")) {
      return RateLimitType.CHATBOT;
    }
    if (path.contains("/transfers") || path.contains("/withdrawals")
        || path.contains("/deposits") || path.contains("/statement")) {
      return RateLimitType.FINANCIAL;
    }
    return RateLimitType.GENERAL;
  }
}
