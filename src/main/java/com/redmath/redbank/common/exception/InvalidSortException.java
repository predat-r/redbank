package com.redmath.redbank.common.exception;

public class InvalidSortException extends RuntimeException {

  public InvalidSortException() {
    super("Unsupported sort field. Allowed fields are: createdAt, id");
  }
}