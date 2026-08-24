package com.redmath.redbank.transaction.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectTransactionRequest {
  @Size(max = 500, message = "Reason must not exceed 500 characters")
  private String reason;
}
