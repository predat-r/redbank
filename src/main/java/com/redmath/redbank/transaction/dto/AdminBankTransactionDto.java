package com.redmath.redbank.transaction.dto;

import com.redmath.redbank.anomaly.AnomalyFlag;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBankTransactionDto {

  private Long id;
  private String transactionReference;
  private String sourceAccountNumber;
  private String destinationAccountNumber;
  private TransactionType type;
  private BigDecimal amount;
  private AnomalyFlag anomalyFlag;
  private TransactionStatus status;
  private OffsetDateTime createdAt;

  public static AdminBankTransactionDto from(BankTransaction transaction) {
    if (transaction == null) {
      return null;
    }
    AdminBankTransactionDto dto = new AdminBankTransactionDto();
    dto.setId(transaction.getId());
    dto.setTransactionReference(transaction.getTransactionReference());
    if (transaction.getSourceAccountHolder() != null) {
      dto.setSourceAccountNumber(transaction.getSourceAccountHolder().getAccountNumber());
    }
    if (transaction.getDestinationAccountHolder() != null) {
      dto.setDestinationAccountNumber(transaction.getDestinationAccountHolder().getAccountNumber());
    }
    dto.setType(transaction.getType());
    dto.setAmount(transaction.getAmount());
    dto.setAnomalyFlag(transaction.getAnomalyFlag());
    dto.setStatus(transaction.getStatus());
    dto.setCreatedAt(transaction.getCreatedAt());
    return dto;
  }
}
