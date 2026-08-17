package com.redmath.redbank.security.ratelimit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile("!test")
public class RateLimitingFilter extends OncePerRequestFilter {

  private final RateLimitingService rateLimitingService;
  private final RateLimitPolicy rateLimitPolicy;
  private final RateLimitKeyResolver rateLimitKeyResolver;
  private final RateLimitResponseWriter rateLimitResponseWriter;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Constructor receives Spring-managed filter dependencies"
  )
  public RateLimitingFilter(RateLimitingService rateLimitingService,
      RateLimitPolicy rateLimitPolicy, RateLimitKeyResolver rateLimitKeyResolver,
      RateLimitResponseWriter rateLimitResponseWriter) {
    this.rateLimitingService = rateLimitingService;
    this.rateLimitPolicy = rateLimitPolicy;
    this.rateLimitKeyResolver = rateLimitKeyResolver;
    this.rateLimitResponseWriter = rateLimitResponseWriter;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    RateLimitType limitType = rateLimitPolicy.resolve(path);
    String rateLimitKey = rateLimitKeyResolver.resolve(request, limitType);
    ConsumptionProbe probe = rateLimitingService.tryConsume(rateLimitKey, limitType);

    if (probe.isConsumed()) {
      response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
      return;
    }

    long waitForRefillSeconds = Math.max(1,
        TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    rateLimitResponseWriter.write(request, response, waitForRefillSeconds);
  }
}
