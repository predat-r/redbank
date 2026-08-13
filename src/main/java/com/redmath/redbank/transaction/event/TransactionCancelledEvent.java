package com.redmath.redbank.transaction.event;

import lombok.Getter;

@Getter
public class TransactionCancelledEvent {

  private final Long transactionId;
  private final String reason;

  public TransactionCancelledEvent(Long transactionId, String reason) {
    this.transactionId = transactionId;
    this.reason = reason;
  }
}
