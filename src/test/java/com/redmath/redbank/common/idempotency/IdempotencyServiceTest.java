package com.redmath.redbank.common.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotencyServiceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private IdempotencyService idempotencyService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    ObjectProvider<ObjectMapper> objectMapperProvider = mock(ObjectProvider.class);
    when(objectMapperProvider.getIfAvailable()).thenReturn(objectMapper);

    ObjectProvider<HazelcastInstance> hazelcastProvider = mock(ObjectProvider.class);
    when(hazelcastProvider.getIfAvailable()).thenReturn(null);

    idempotencyService = new IdempotencyService(objectMapperProvider, hazelcastProvider);
  }

  @Test
  void testCreateInProgressRecord() {
    IdempotencyKey record = idempotencyService.createInProgressRecord("key-123", 1L, "/api/test", "hash-abc");

    assertNotNull(record);
    assertEquals("key-123", record.getIdempotencyKey());
    assertEquals(1L, record.getUserId());
    assertEquals(IdempotencyStatus.IN_PROGRESS, record.getStatus());

    Optional<IdempotencyKey> existing = idempotencyService.findExistingKey("key-123", 1L);
    assertTrue(existing.isPresent());
  }

  @Test
  void testMarkCompleted() {
    idempotencyService.createInProgressRecord("key-123", 1L, "/api/test", "hash-abc");
    idempotencyService.markCompleted("key-123", 1L, 201, "Success Payload");

    Optional<IdempotencyKey> keyOpt = idempotencyService.findExistingKey("key-123", 1L);
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

    ResponseEntity<?> response = idempotencyService.createReplayedResponse(record, String.class);

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
