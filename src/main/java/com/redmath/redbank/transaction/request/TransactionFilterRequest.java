package com.redmath.redbank.transaction.request;

import com.redmath.redbank.transaction.AnomalyFlag;
import com.redmath.redbank.transaction.TransactionCategory;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class TransactionFilterRequest {

  private String reference;
  private String accountNumber;
  private TransactionType type;
  private TransactionStatus status;
  private TransactionCategory category;
  private AnomalyFlag anomalyFlag;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime fromDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime toDate;
}
