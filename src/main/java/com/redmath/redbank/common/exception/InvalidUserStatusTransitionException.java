package com.redmath.redbank.common.exception;

public class InvalidUserStatusTransitionException extends RuntimeException {

  public InvalidUserStatusTransitionException(String message) {
    super(message);
  }
}
