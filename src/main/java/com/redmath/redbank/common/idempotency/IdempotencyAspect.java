package com.redmath.redbank.common.idempotency;

import com.redmath.redbank.common.exception.ConflictException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Slf4j
@Aspect
@Component
public class IdempotencyAspect {

  private final IdempotencyService idempotencyService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public IdempotencyAspect(IdempotencyService idempotencyService) {
    this.idempotencyService = idempotencyService;
  }

  @Around("@annotation(requireIdempotency)")
  public Object handleIdempotency(ProceedingJoinPoint joinPoint,
      RequireIdempotency requireIdempotency) throws Throwable {
    HttpServletRequest request = getCurrentHttpRequest();
    if (request == null) {
      return joinPoint.proceed();
    }

    String idempotencyKey = request.getHeader(IdempotencyService.IDEMPOTENCY_HEADER);
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      if (requireIdempotency.required()) {
        throw new IllegalArgumentException(
            "Header '" + IdempotencyService.IDEMPOTENCY_HEADER + "' is required for this request");
      }
      return joinPoint.proceed();
    }

    Long userId = extractUserId();
    if (userId == null) {
      return joinPoint.proceed();
    }

    String cacheKey = idempotencyService.buildCacheKey(idempotencyKey, userId);
    idempotencyService.lockKey(cacheKey);

    try {
      String requestPath = request.getRequestURI();
      String requestHash = idempotencyService.computeHash(joinPoint.getArgs());

      Optional<IdempotencyKey> existingRecord = idempotencyService.findExistingKey(cacheKey);

      if (existingRecord.isPresent()) {
        IdempotencyKey record = existingRecord.get();

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
          throw new ConflictException(
              "A request with idempotency key '" + idempotencyKey
                  + "' is currently being processed");
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
          if (!record.getRequestHash().equals(requestHash)) {
            throw new ConflictException(
                "Idempotency key '" + idempotencyKey
                    + "' was previously used with a different request payload");
          }
          return idempotencyService.createReplayedResponse(record);
        }
      }

      idempotencyService.createInProgressRecord(cacheKey, idempotencyKey, userId, requestPath,
          requestHash);
    } finally {
      idempotencyService.unlockKey(cacheKey);
    }

    Object result;
    try {
      result = joinPoint.proceed();
    } catch (Throwable t) {
      if (log.isWarnEnabled()) {
        log.warn("Request failed for idempotency key {}: {}", idempotencyKey, t.getMessage());
      }
      idempotencyService.markFailed(cacheKey);
      throw t;
    }

    int statusCode = 200;
    Object bodyToSave = result;

    if (result instanceof ResponseEntity responseEntity) {
      statusCode = responseEntity.getStatusCode().value();
      bodyToSave = responseEntity.getBody();
    }

    idempotencyService.markCompleted(cacheKey, statusCode, bodyToSave);
    return result;
  }

  private HttpServletRequest getCurrentHttpRequest() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attributes != null ? attributes.getRequest() : null;
  }

  private Long extractUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
      Number userId = jwt.getClaim("userId");
      if (userId != null) {
        return userId.longValue();
      }
    }
    return null;
  }
}
