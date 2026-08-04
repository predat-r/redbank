package com.redmath.redbank.common.exception;

public class RegistrationNotFoundException extends RuntimeException {

  public RegistrationNotFoundException() {
    super("Registration was not found");
  }
}
