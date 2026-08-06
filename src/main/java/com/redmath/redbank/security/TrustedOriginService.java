package com.redmath.redbank.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TrustedOriginService {

  private final List<String> allowedOrigins;

  public TrustedOriginService(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
    this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isEmpty())
        .toList();
  }

  public List<String> allowedOrigins() {
    return List.copyOf(allowedOrigins);
  }

  public void requireTrusted(String origin) {
    if (origin == null || !allowedOrigins.contains(origin)) {
      throw new AccessDeniedException("Request origin is not allowed");
    }
  }
}
