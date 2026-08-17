package com.redmath.redbank.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingService {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public ConsumptionProbe tryConsume(String key, RateLimitType type) {
    Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(type));
    return bucket.tryConsumeAndReturnRemaining(1);
  }

  private Bucket createBucket(RateLimitType type) {
    Bandwidth limit = Bandwidth.builder()
        .capacity(type.getCapacity())
        .refillGreedy(type.getCapacity(), type.getRefillDuration())
        .build();

    return Bucket.builder()
        .addLimit(limit)
        .build();
  }
}
