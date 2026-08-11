package com.redmath.redbank.chatbot.dto;

import com.redmath.redbank.chatbot.enums.Direction;
import com.redmath.redbank.chatbot.enums.Metric;
import com.redmath.redbank.chatbot.enums.QueryType;
import com.redmath.redbank.transaction.TransactionCategory;
import com.redmath.redbank.transaction.TransactionType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LlmIntentOutput {
  private QueryType queryType;
  private Metric metric;
  private Direction direction;
  private TransactionCategory category;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate asOfDate;
  private String counterpartyName;
  // NOTE: counterpartyAccountHolderId is intentionally omitted to prevent prompt injection
  private boolean needsClarification;
  private String clarificationQuestion;
  private TransactionType transactionType;
  private String sortOrder;
}
