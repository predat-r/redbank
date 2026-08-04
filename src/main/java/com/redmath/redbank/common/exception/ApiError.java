package com.redmath.redbank.common.exception;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

  private OffsetDateTime timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
}
