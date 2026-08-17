package com.redmath.redbank.security.ratelimit;

import com.redmath.redbank.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RateLimitResponseWriter {

  private final ObjectMapper objectMapper;

  public void write(HttpServletRequest request, HttpServletResponse response,
      long waitForRefillSeconds) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));

    ApiError error = new ApiError(OffsetDateTime.now(ZoneOffset.UTC),
        HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
        "Rate limit exceeded. Please try again in " + waitForRefillSeconds + " seconds.",
        request.getRequestURI());
    objectMapper.writeValue(response.getOutputStream(), error);
  }
}
