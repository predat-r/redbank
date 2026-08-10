package com.redmath.redbank.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionCategory {
  FOOD,
  GROCERY,
  DONATION,
  BILLS,
  ENTERTAINMENT,
  SHOPPING,
  HEALTH,
  TRANSPORT,
  EDUCATION,
  INVESTMENT,
  OTHER;

  @JsonCreator
  public static TransactionCategory fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    for (TransactionCategory category : TransactionCategory.values()) {
      if (category.name().equalsIgnoreCase(value.trim())) {
        return category;
      }
    }
    throw new IllegalArgumentException("Invalid transaction category: " + value);
  }
}
