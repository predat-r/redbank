package com.redmath.redbank.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
      ResourceNotFoundException.class,
      RegistrationNotFoundException.class,
      UserNotFoundException.class
  })
  public ResponseEntity<ApiError> handleNotFound(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiError> handleNoResourceFound(
      NoResourceFoundException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.NOT_FOUND, "Endpoint not found: " + request.getRequestURI(), request);
  }

  @ExceptionHandler({
      InvalidCredentialsException.class,
      InvalidRefreshTokenException.class
  })
  public ResponseEntity<ApiError> handleUnauthorized(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
  }

  @ExceptionHandler(UserAccountNotActiveException.class)
  public ResponseEntity<ApiError> handleInactiveAccount(
      UserAccountNotActiveException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
  }

  @ExceptionHandler({
      DuplicateUserException.class,
      RegistrationAlreadyReviewedException.class,
      ConflictException.class,
      InvalidUserStatusTransitionException.class
  })
  public ResponseEntity<ApiError> handleConflict(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler({
      IllegalArgumentException.class,
      InvalidSortException.class,
      InvalidPasswordChangeException.class,
      InsufficientFundsException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining(", "));

    if (message.isBlank()) {
      message = "One or more request fields are invalid";
    }

    return buildResponse(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableRequest(
      HttpMessageNotReadableException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.BAD_REQUEST,
        "The request body is missing or contains malformed JSON", request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(
      Exception ex,
      HttpServletRequest request
  ) {
    log.error("Unhandled exception occurred", ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred", request);
  }

  private ResponseEntity<ApiError> buildResponse(
      HttpStatus status,
      String message,
      HttpServletRequest request
  ) {
    ApiError error = new ApiError(
        OffsetDateTime.now(ZoneOffset.UTC),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI()
    );
    return ResponseEntity.status(status).body(error);
  }
}
