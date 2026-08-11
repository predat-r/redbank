package com.redmath.redbank.transaction.event;

import lombok.Getter;

@Getter
public class TransactionSubmittedEvent {

  private final Long transactionId;

  public TransactionSubmittedEvent(Long transactionId) {
    this.transactionId = transactionId;
  }
}
