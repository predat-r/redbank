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
  public IdempotencyService(
      ObjectMapper objectMapper,
      ObjectProvider<HazelcastInstance> hazelcastProvider) {
    this.objectMapper = objectMapper;
    this.hazelcastInstance =
        hazelcastProvider != null ? hazelcastProvider.getIfAvailable() : null;
  }

  private Map<String, IdempotencyKey> getIdempotencyMap() {
    if (hazelcastInstance != null) {
      try {
        return hazelcastInstance.getMap(HAZELCAST_MAP_NAME);
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Hazelcast map unavailable, using local memory map: {}", e.getMessage());
        }
      }
    }
    return fallbackMap;
  }

  private String buildCacheKey(String key, Long userId) {
    return userId + ":" + key;
  }

  public Optional<IdempotencyKey> findExistingKey(String key, Long userId) {
    String cacheKey = buildCacheKey(key, userId);
    Map<String, IdempotencyKey> map = getIdempotencyMap();
    IdempotencyKey cached = map.get(cacheKey);
    if (cached != null) {
      if (log.isDebugEnabled()) {
        log.debug("Idempotency key found in Hazelcast: {}", cacheKey);
      }
      return Optional.of(cached);
    }
    return Optional.empty();
  }

  public IdempotencyKey createInProgressRecord(String key, Long userId, String path, String hash) {
    String cacheKey = buildCacheKey(key, userId);
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

    Map<String, IdempotencyKey> map = getIdempotencyMap();
    if (map instanceof IMap<String, IdempotencyKey> imap) {
      IdempotencyKey existing = imap.putIfAbsent(cacheKey, record, TTL_HOURS, TimeUnit.HOURS);
      if (existing != null) {
        if (log.isInfoEnabled()) {
          log.info("Hazelcast lock acquired by another request for key: {}", cacheKey);
        }
        return existing;
      }
      return record;
    } else {
      IdempotencyKey existing = map.putIfAbsent(cacheKey, record);
      if (existing != null) {
        return existing;
      }
      return record;
    }
  }

  public void markCompleted(String key, Long userId, int statusCode, Object responseBody) {
    String cacheKey = buildCacheKey(key, userId);
    Map<String, IdempotencyKey> map = getIdempotencyMap();
    IdempotencyKey record = map.get(cacheKey);

    if (record != null) {
      record.setStatus(IdempotencyStatus.COMPLETED);
      record.setResponseStatusCode(statusCode);
      record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      try {
        record.setResponseBody(objectMapper.writeValueAsString(responseBody));
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn("Failed to serialize response body for idempotency key {}", key, e);
        }
        record.setResponseBody(null);
      }

      if (map instanceof IMap<String, IdempotencyKey> imap) {
        imap.put(cacheKey, record, TTL_HOURS, TimeUnit.HOURS);
      } else {
        map.put(cacheKey, record);
      }
    }
  }

  public void markFailed(String key, Long userId) {
    String cacheKey = buildCacheKey(key, userId);
    Map<String, IdempotencyKey> map = getIdempotencyMap();
    IdempotencyKey record = map.get(cacheKey);

    if (record != null) {
      record.setStatus(IdempotencyStatus.FAILED);
      record.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
      if (map instanceof IMap<String, IdempotencyKey> imap) {
        imap.put(cacheKey, record, TTL_HOURS, TimeUnit.HOURS);
      } else {
        map.put(cacheKey, record);
      }
    }
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

  public ResponseEntity<?> createReplayedResponse(IdempotencyKey record,
      Class<?> targetReturnType) {
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
        if (targetReturnType != null && !void.class.equals(targetReturnType)) {
          body = objectMapper.readValue(record.getResponseBody(), targetReturnType);
        } else {
          body = objectMapper.readValue(record.getResponseBody(), Object.class);
        }
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
