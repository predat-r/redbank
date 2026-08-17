package com.redmath.redbank.security.ratelimit;

import java.time.Duration;

public enum RateLimitType {
  AUTH(10, Duration.ofMinutes(1)),
  CHATBOT(5, Duration.ofMinutes(1)),
  FINANCIAL(20, Duration.ofMinutes(1)),
  GENERAL(50, Duration.ofMinutes(1));

  private final long capacity;
  private final Duration refillDuration;

  RateLimitType(long capacity, Duration refillDuration) {
    this.capacity = capacity;
    this.refillDuration = refillDuration;
  }

  public long getCapacity() {
    return capacity;
  }

  public Duration getRefillDuration() {
    return refillDuration;
  }
}
