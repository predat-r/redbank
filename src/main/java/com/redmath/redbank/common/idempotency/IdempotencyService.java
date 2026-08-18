package com.redmath.redbank.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class IdempotencyService {

  public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
  public static final String REPLAYED_HEADER = "X-Idempotent-Replayed";
  public static final String HAZELCAST_MAP_NAME = "idempotency-keys";
  private static final long TTL_HOURS = 24;

  private final ObjectMapper objectMapper;
  private final HazelcastInstance hazelcastInstance;
  private final Map<String, IdempotencyKey> fallbackMap = new ConcurrentHashMap<>();

  @Autowired
  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public IdempotencyService(
      ObjectMapper objectMapper,
      ObjectProvider<HazelcastInstance> hazelcastProvider) {
    this.objectMapper = objectMapper;
    this.hazelcastInstance =
        hazelcastProvider != null ? hazelcastProvider.getIfAvailable() : null;
  }

  public String buildCacheKey(String key, Long userId) {
    return userId + ":" + key;
  }

  public void lockKey(String cacheKey) {
    if (hazelcastInstance != null) {
      try {
        hazelcastInstance.getMap(HAZELCAST_MAP_NAME).lock(cacheKey);
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Failed to lock Hazelcast key {}: {}", cacheKey, e.getMessage());
        }
      }
    }
  }

  public void unlockKey(String cacheKey) {
    if (hazelcastInstance != null) {
      try {
        IMap<String, IdempotencyKey> map = hazelcastInstance.getMap(HAZELCAST_MAP_NAME);
        if (map.isLocked(cacheKey)) {
          map.unlock(cacheKey);
        }
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Failed to unlock Hazelcast key {}: {}", cacheKey, e.getMessage());
        }
      }
    }
  }

  public Optional<IdempotencyKey> findExistingKey(String cacheKey) {
    if (hazelcastInstance != null) {
      try {
        IMap<String, IdempotencyKey> map = hazelcastInstance.getMap(HAZELCAST_MAP_NAME);
        IdempotencyKey cached = map.get(cacheKey);
        if (cached != null) {
          return Optional.of(cached);
        }
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Hazelcast get failed for key {}: {}", cacheKey, e.getMessage());
        }
      }
    }
    return Optional.ofNullable(fallbackMap.get(cacheKey));
  }

  public IdempotencyKey createInProgressRecord(String cacheKey, String key, Long userId,
      String path, String hash) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    IdempotencyKey record = IdempotencyKey.builder()
        .idempotencyKey(key)
        .userId(userId)
        .requestPath(path)
        .requestHash(hash)
        .status(IdempotencyStatus.IN_PROGRESS)
        .createdAt(now)
        .updatedAt(now)
        .build();

    if (hazelcastInstance != null) {
      try {
        IMap<String, IdempotencyKey> map = hazelcastInstance.getMap(HAZELCAST_MAP_NAME);
        IdempotencyKey existing = map.putIfAbsent(cacheKey, record, TTL_HOURS, TimeUnit.HOURS);
        return existing != null ? existing : record;
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Hazelcast putIfAbsent failed for key {}: {}", cacheKey, e.getMessage());
        }
      }
    }

    IdempotencyKey existing = fallbackMap.putIfAbsent(cacheKey, record);
    return existing != null ? existing : record;
  }

  public void markCompleted(String cacheKey, int statusCode, Object responseBody) {
    IdempotencyKey record = findExistingKey(cacheKey).orElse(null);
    if (record != null) {
      record.setStatus(IdempotencyStatus.COMPLETED);
      record.setResponseStatusCode(statusCode);
      record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      try {
        record.setResponseBody(objectMapper.writeValueAsString(responseBody));
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Failed to serialize response body for cache key {}", cacheKey, e);
        }
        record.setResponseBody(null);
      }
      putInCache(cacheKey, record);
      if (log.isInfoEnabled()) {
        log.warn("Cached response against the key {}", cacheKey);
      }
    }
  }

  public void markFailed(String cacheKey) {
    IdempotencyKey record = findExistingKey(cacheKey).orElse(null);
    if (record != null) {
      record.setStatus(IdempotencyStatus.FAILED);
      record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      putInCache(cacheKey, record);
    }
  }

  private void putInCache(String cacheKey, IdempotencyKey record) {
    if (hazelcastInstance != null) {
      try {
        IMap<String, IdempotencyKey> map = hazelcastInstance.getMap(HAZELCAST_MAP_NAME);
        map.put(cacheKey, record, TTL_HOURS, TimeUnit.HOURS);
        return;
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Failed to put key in Hazelcast {}: {}", cacheKey, e.getMessage());
        }
      }
    }
    fallbackMap.put(cacheKey, record);
  }

  public String computeHash(Object... args) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (Object arg : args) {
        if (arg == null) {
          continue;
        }
        String className = arg.getClass().getName();
        if (className.startsWith("org.springframework") || className.startsWith(
            "jakarta.servlet")) {
          continue;
        }
        byte[] jsonBytes = objectMapper.writeValueAsString(arg).getBytes(StandardCharsets.UTF_8);
        digest.update(jsonBytes);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn("Failed to compute hash for request arguments", e);
      }
      return "HASH_ERROR";
    }
  }

  public ResponseEntity<Object> createReplayedResponse(IdempotencyKey record) {
    if (log.isInfoEnabled()) {
      log.info("Replaying cached idempotent response for key: {} user: {}",
          record.getIdempotencyKey(), record.getUserId());
    }
    HttpStatus status = HttpStatus.resolve(record.getResponseStatusCode());
    if (status == null) {
      status = HttpStatus.OK;
    }

    Object body = null;
    if (record.getResponseBody() != null && !record.getResponseBody().isBlank()) {
      try {
        body = objectMapper.readValue(record.getResponseBody(), Object.class);
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Failed to deserialize cached idempotency response body", e);
        }
      }
    }

    return ResponseEntity.status(status)
        .header(REPLAYED_HEADER, "true")
        .body(body);
  }
}
