package com.redmath.redbank.security;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("!test")
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimitingService rateLimitingService;

  public RateLimitingFilter(RateLimitingService rateLimitingService) {
    this.rateLimitingService = rateLimitingService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();
    String clientIp = extractClientIp(request);

    RateLimitingService.RateLimitType limitType = resolveRateLimitType(path);
    String rateLimitKey = buildRateLimitKey(limitType, path, clientIp);

    ConsumptionProbe probe = rateLimitingService.tryConsume(rateLimitKey, limitType);

    if (probe.isConsumed()) {
      response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      long waitForRefillSeconds = Math.max(1,
          TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));

      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));

      String jsonBody = String.format(
          "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again in %d seconds.\",\"path\":\"%s\"}",
          java.time.Instant.now(),
          waitForRefillSeconds,
          path);

      response.getWriter().write(jsonBody);
    }
  }

  private RateLimitingService.RateLimitType resolveRateLimitType(String path) {
    if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
      return RateLimitingService.RateLimitType.AUTH;
    }
    if (path.contains("/chat")) {
      return RateLimitingService.RateLimitType.CHATBOT;
    }
    if (path.contains("/transfers") || path.contains("/withdrawals") || path.contains("/deposits")) {
      return RateLimitingService.RateLimitType.FINANCIAL;
    }
    return RateLimitingService.RateLimitType.GENERAL;
  }

  private String buildRateLimitKey(RateLimitingService.RateLimitType type, String path, String ip) {
    String identity = extractUserIdentity();
    if (identity == null) {
      identity = ip;
    }
    return type.name().toLowerCase() + ":" + identity;
  }

  private String extractUserIdentity() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      Number userId = jwt.getClaim("userId");
      if (userId != null) {
        return "user:" + userId;
      }
      return "sub:" + jwt.getSubject();
    }
    return null;
  }

  private String extractClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
