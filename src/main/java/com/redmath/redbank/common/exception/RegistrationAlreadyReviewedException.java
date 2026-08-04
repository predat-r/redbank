package com.redmath.redbank.common.exception;

public class RegistrationAlreadyReviewedException extends RuntimeException {

  public RegistrationAlreadyReviewedException() {
    super("Registration has already been reviewed");
  }
}