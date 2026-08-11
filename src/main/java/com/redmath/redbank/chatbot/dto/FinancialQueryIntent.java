package com.redmath.redbank.chatbot.dto;

import com.redmath.redbank.chatbot.enums.Direction;
import com.redmath.redbank.chatbot.enums.Metric;
import com.redmath.redbank.chatbot.enums.QueryType;
import com.redmath.redbank.transaction.TransactionCategory; // adjust import to your actual package
import com.redmath.redbank.transaction.TransactionType;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinancialQueryIntent {
  private QueryType queryType;
  private Metric metric;
  private Direction direction;
  private TransactionCategory category;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate asOfDate;
  private String counterpartyName;
  private String counterpartyAccountNumber;
  private boolean needsClarification;
  private String clarificationQuestion;
  private TransactionType transactionType;
  private String sortOrder;
}