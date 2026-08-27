package com.redmath.redbank.security.ratelimit;

import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicy {

  public RateLimitType resolve(String path) {
    return Arrays.stream(RateLimitRule.values())
        .filter(rule -> rule.matches(path))
        .map(RateLimitRule::getType)
        .findFirst()
        .orElse(RateLimitType.GENERAL);
  }
}