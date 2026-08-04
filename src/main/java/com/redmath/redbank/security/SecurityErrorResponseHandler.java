package com.redmath.redbank.security;

import com.redmath.redbank.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(@NonNull HttpServletRequest request, HttpServletResponse response,
      @NonNull AuthenticationException exception) throws IOException {
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

    writeError(request, response, HttpStatus.UNAUTHORIZED,
        "A valid bearer token is required to access this resource");
  }

  @Override
  public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull AccessDeniedException exception) throws IOException {
    writeError(request, response, HttpStatus.FORBIDDEN,
        "You do not have permission to access this resource");
  }

  private void writeError(HttpServletRequest request, HttpServletResponse response,
      HttpStatus status, String message) throws IOException {
    ApiError error = new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status.value(),
        status.getReasonPhrase(), message, request.getRequestURI());

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    objectMapper.writeValue(response.getOutputStream(), error);
  }
}