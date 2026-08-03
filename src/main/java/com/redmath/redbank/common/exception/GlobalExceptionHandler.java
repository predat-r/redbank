package com.redmath.redbank.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidCredentialsException.class)
  public ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage(),
        request);
  }

  @ExceptionHandler(UserAccountNotActiveException.class)
  public ProblemDetail handleInactiveAccount(UserAccountNotActiveException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.FORBIDDEN, "User account is not active", exception.getMessage(),
        request);
  }

  @ExceptionHandler(DuplicateUserException.class)
  public ProblemDetail handleDuplicateUser(DuplicateUserException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.CONFLICT, "Registration conflict", exception.getMessage(),
        request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception,
      HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();

    exception.getBindingResult().getFieldErrors()
        .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));

    ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Validation failed",
        "One or more request fields are invalid", request);

    problem.setProperty("fieldErrors", fieldErrors);

    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.BAD_REQUEST, "Invalid request body",
        "The request body is missing or contains malformed JSON", request);
  }

  @ExceptionHandler(InvalidSortException.class)
  public ProblemDetail handleInvalidSort(InvalidSortException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.BAD_REQUEST, "Invalid sort parameter", exception.getMessage(),
        request);
  }

  @ExceptionHandler(InvalidRefreshTokenException.class)
  public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException exception,
      HttpServletRequest request) {
    return createProblem(HttpStatus.UNAUTHORIZED, "Invalid refresh token", exception.getMessage(),
        request);
  }

  private ProblemDetail createProblem(HttpStatus status, String title, String detail,
      HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now());

    return problem;
  }
}