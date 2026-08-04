package com.redmath.redbank.common.exception;

import java.util.Collection;
import java.util.stream.Collectors;

public class InvalidSortException extends RuntimeException {

  public InvalidSortException() {
    this(java.util.Set.of("createdAt", "id"));
  }

  public InvalidSortException(Collection<String> allowedFields) {
    super(
        "Unsupported sort field. Allowed fields are: "
            + allowedFields.stream()
            .sorted()
            .collect(Collectors.joining(", "))
    );
  }
}