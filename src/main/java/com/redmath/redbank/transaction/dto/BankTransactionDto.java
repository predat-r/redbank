package com.redmath.redbank.transaction.dto;

import com.redmath.redbank.ai.anomaly.AnomalyFlag;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionCategory;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankTransactionDto {

  private Long id;
  private String transactionReference;
  private String sourceAccountNumber;
  private String destinationAccountNumber;
  private String reversedTransactionReference;
  private TransactionType type;
  private String description;
  private TransactionCategory category;
  private BigDecimal amount;
  private TransactionStatus status;
  private AnomalyFlag anomalyFlag;
  private OffsetDateTime createdAt;
  private OffsetDateTime completedAt;

  public static BankTransactionDto from(BankTransaction transaction) {
    BankTransactionDto dto = new BankTransactionDto();
    dto.setId(transaction.getId());
    dto.setTransactionReference(transaction.getTransactionReference());
    if (transaction.getSourceAccountHolder() != null) {
      dto.setSourceAccountNumber(transaction.getSourceAccountHolder().getAccountNumber());
    }
    if (transaction.getDestinationAccountHolder() != null) {
      dto.setDestinationAccountNumber(transaction.getDestinationAccountHolder().getAccountNumber());
    }
    if (transaction.getReversedTransaction() != null) {
      dto.setReversedTransactionReference(
          transaction.getReversedTransaction().getTransactionReference());
    }
    dto.setType(transaction.getType());
    dto.setDescription(transaction.getDescription());
    dto.setCategory(transaction.getCategory());
    dto.setAmount(transaction.getAmount());
    dto.setStatus(transaction.getStatus());
    dto.setAnomalyFlag(transaction.getAnomalyFlag());
    dto.setCreatedAt(transaction.getCreatedAt());
    dto.setCompletedAt(transaction.getCompletedAt());
    dto.setAnomalyFlag(transaction.getAnomalyFlag());
    return dto;
  }
}
