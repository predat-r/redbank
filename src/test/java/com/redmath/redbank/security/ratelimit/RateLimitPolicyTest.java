package com.redmath.redbank.security.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitPolicyTest {

  private RateLimitPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new RateLimitPolicy();
  }

  @Test
  void shouldResolveAuthEndpoints() {
    assertEquals(RateLimitType.AUTH, policy.resolve("/api/auth/login"));
    assertEquals(RateLimitType.AUTH, policy.resolve("/api/auth/register"));
    assertEquals(RateLimitType.AUTH, policy.resolve("/api/auth/refresh"));
  }

  @Test
  void shouldResolveChatbotEndpoint() {
    assertEquals(RateLimitType.CHATBOT, policy.resolve("/api/accounts/me/chat"));
  }

  @Test
  void shouldResolveFinancialEndpoints() {
    assertEquals(RateLimitType.FINANCIAL,
        policy.resolve("/api/accounts/me/transfers"));
    assertEquals(RateLimitType.FINANCIAL,
        policy.resolve("/api/accounts/me/withdrawals"));
    assertEquals(RateLimitType.FINANCIAL,
        policy.resolve("/api/admin/deposits"));
    assertEquals(RateLimitType.FINANCIAL,
        policy.resolve("/api/accounts/me/statement"));
  }

  @Test
  void shouldUseGeneralLimitForUnmatchedEndpoint() {
    assertEquals(RateLimitType.GENERAL,
        policy.resolve("/api/accounts/me/transactions"));
  }
}
