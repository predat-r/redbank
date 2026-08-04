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
      RuntimeException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiError> handleNoResourceFound(
      NoResourceFoundException exception,
      HttpServletRequest request
  ) {
    return buildResponse(
        HttpStatus.NOT_FOUND,
        "Endpoint not found: " + request.getRequestURI(),
        request
    );
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleInvalidCredentials(
      InvalidCredentialsException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ResponseEntity<ApiError> handleInvalidRefreshToken(
      InvalidRefreshTokenException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(UserAccountNotActiveException.class)
  public ResponseEntity<ApiError> handleInactiveAccount(
      UserAccountNotActiveException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
  }

  @ExceptionHandler({
      DuplicateUserException.class,
      RegistrationAlreadyReviewedException.class,
      ConflictException.class
  })
  public ResponseEntity<ApiError> handleConflict(
      RuntimeException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @ExceptionHandler({
      IllegalArgumentException.class,
      InvalidSortException.class,
      InvalidPasswordChangeException.class,
      InsufficientFundsException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(
      RuntimeException exception,
      HttpServletRequest request
  ) {
    return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    String message = exception.getBindingResult()
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
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "The request body is missing or contains malformed JSON",
        request
    );
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception,
      HttpServletRequest request
  ) {
    return buildResponse(
        HttpStatus.METHOD_NOT_ALLOWED,
        exception.getMessage(),
        request
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpectedException(
      Exception exception,
      HttpServletRequest request
  ) {
    log.error(
        "Unhandled exception while processing {}",
        request.getRequestURI(),
        exception
    );

    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred",
        request
    );
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