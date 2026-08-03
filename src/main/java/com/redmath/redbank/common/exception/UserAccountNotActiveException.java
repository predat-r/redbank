package com.redmath.redbank.common.exception;

public class UserAccountNotActiveException extends RuntimeException {

  public UserAccountNotActiveException() {
    super("User account is not active");
  }
}