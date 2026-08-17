package com.redmath.redbank.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyResolver {

  public String resolve(HttpServletRequest request, RateLimitType type) {
    String identity = extractUserIdentity();
    if (identity == null) {
      identity = extractClientIp(request);
    }
    return type.name().toLowerCase() + ":" + identity;
  }

  private String extractUserIdentity() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
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
