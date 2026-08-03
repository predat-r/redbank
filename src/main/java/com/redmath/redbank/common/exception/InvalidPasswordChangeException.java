package com.redmath.redbank.common.exception;

public class InvalidPasswordChangeException extends RuntimeException {

  public InvalidPasswordChangeException(String message) {
    super(message);
  }
}