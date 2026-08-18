package com.redmath.redbank.common.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotencyServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private IdempotencyService idempotencyService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    ObjectProvider<HazelcastInstance> hazelcastProvider = mock(ObjectProvider.class);
    when(hazelcastProvider.getIfAvailable()).thenReturn(null);

    idempotencyService = new IdempotencyService(objectMapper, hazelcastProvider);
  }

  @Test
  void testCreateInProgressRecord() {
    String cacheKey = idempotencyService.buildCacheKey("key-123", 1L);
    IdempotencyKey record = idempotencyService.createInProgressRecord(cacheKey, "key-123", 1L, "/api/test", "hash-abc");

    assertNotNull(record);
    assertEquals("key-123", record.getIdempotencyKey());
    assertEquals(1L, record.getUserId());
    assertEquals(IdempotencyStatus.IN_PROGRESS, record.getStatus());

    Optional<IdempotencyKey> existing = idempotencyService.findExistingKey(cacheKey);
    assertTrue(existing.isPresent());
  }

  @Test
  void testMarkCompleted() {
    String cacheKey = idempotencyService.buildCacheKey("key-123", 1L);
    idempotencyService.createInProgressRecord(cacheKey, "key-123", 1L, "/api/test", "hash-abc");
    idempotencyService.markCompleted(cacheKey, 201, "Success Payload");

    Optional<IdempotencyKey> keyOpt = idempotencyService.findExistingKey(cacheKey);
    assertTrue(keyOpt.isPresent());
    IdempotencyKey key = keyOpt.get();
    assertEquals(IdempotencyStatus.COMPLETED, key.getStatus());
    assertEquals(201, key.getResponseStatusCode());
    assertNotNull(key.getResponseBody());
  }

  @Test
  void testCreateReplayedResponse() {
    IdempotencyKey record = IdempotencyKey.builder()
        .idempotencyKey("key-123")
        .userId(1L)
        .responseStatusCode(201)
        .responseBody("\"Success Payload\"")
        .build();

    ResponseEntity<?> response = idempotencyService.createReplayedResponse(record);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Success Payload", response.getBody());
    assertNotNull(response.getHeaders().getFirst(IdempotencyService.REPLAYED_HEADER));
  }

  @Test
  void testComputeHash() {
    String hash1 = idempotencyService.computeHash("testPayload", 100);
    String hash2 = idempotencyService.computeHash("testPayload", 100);

    assertNotNull(hash1);
    assertEquals(hash1, hash2);
  }
}
