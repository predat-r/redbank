package com.redmath.redbank.transaction.event;

import lombok.Getter;

@Getter
public class TransactionCompletedEvent {

  private final Long transactionId;

  public TransactionCompletedEvent(Long transactionId) {
    this.transactionId = transactionId;
  }
}
