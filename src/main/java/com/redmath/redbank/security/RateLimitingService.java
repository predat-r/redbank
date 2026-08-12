package com.redmath.redbank.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingService {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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
