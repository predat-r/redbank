package com.redmath.redbank.transaction.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectTransactionRequest {
  private String reason;
}
