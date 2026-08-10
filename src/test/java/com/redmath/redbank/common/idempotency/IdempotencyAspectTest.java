package com.redmath.redbank.common.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.common.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

  @Mock
  private IdempotencyService idempotencyService;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  @Mock
  private HttpServletRequest request;

  @Mock
  private RequireIdempotency requireIdempotency;

  private IdempotencyAspect idempotencyAspect;

  @BeforeEach
  void setUp() {
    idempotencyAspect = new IdempotencyAspect(idempotencyService);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim("userId", 1L)
        .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, Collections.emptyList()));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  @Test
  void testMissingHeaderWhenRequiredThrowsException() {
    when(request.getHeader(IdempotencyService.IDEMPOTENCY_HEADER)).thenReturn(null);
    when(requireIdempotency.required()).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () ->
        idempotencyAspect.handleIdempotency(joinPoint, requireIdempotency));
  }

  @Test
  void testMissingHeaderWhenNotRequiredProceeds() throws Throwable {
    when(request.getHeader(IdempotencyService.IDEMPOTENCY_HEADER)).thenReturn(null);
    when(requireIdempotency.required()).thenReturn(false);
    when(joinPoint.proceed()).thenReturn("Result");

    Object result = idempotencyAspect.handleIdempotency(joinPoint, requireIdempotency);

    assertEquals("Result", result);
  }

  @Test
  void testInProgressKeyThrowsConflictException() {
    when(request.getHeader(IdempotencyService.IDEMPOTENCY_HEADER)).thenReturn("key-123");
    when(request.getRequestURI()).thenReturn("/api/accounts/me/transfers");
    when(idempotencyService.computeHash(any())).thenReturn("hash-123");

    IdempotencyKey inProgressRecord = IdempotencyKey.builder()
        .idempotencyKey("key-123")
        .userId(1L)
        .status(IdempotencyStatus.IN_PROGRESS)
        .build();

    when(idempotencyService.findExistingKey("key-123", 1L)).thenReturn(Optional.of(inProgressRecord));

    assertThrows(ConflictException.class, () ->
        idempotencyAspect.handleIdempotency(joinPoint, requireIdempotency));
  }

  @Test
  void testCompletedKeyReplaysResponse() throws Throwable {
    when(request.getHeader(IdempotencyService.IDEMPOTENCY_HEADER)).thenReturn("key-123");
    when(request.getRequestURI()).thenReturn("/api/accounts/me/transfers");
    when(idempotencyService.computeHash(any())).thenReturn("hash-123");

    IdempotencyKey completedRecord = IdempotencyKey.builder()
        .idempotencyKey("key-123")
        .userId(1L)
        .requestHash("hash-123")
        .status(IdempotencyStatus.COMPLETED)
        .responseStatusCode(200)
        .responseBody("\"Replayed Response\"")
        .build();

    when(idempotencyService.findExistingKey("key-123", 1L)).thenReturn(Optional.of(completedRecord));
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getReturnType()).thenReturn((Class) ResponseEntity.class);

    ResponseEntity<String> expectedResponse = ResponseEntity.ok("Replayed Response");
    when(idempotencyService.createReplayedResponse(eq(completedRecord), any())).thenReturn((ResponseEntity) expectedResponse);

    Object result = idempotencyAspect.handleIdempotency(joinPoint, requireIdempotency);

    assertEquals(expectedResponse, result);
  }
}
