package com.redmath.redbank.transaction.dto;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountStatus;
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
public class AdminBankTransactionDetailDto {

  private Long id;
  private String transactionReference;
  private String reversedTransactionReference;
  private TransactionType type;
  private String description;
  private TransactionCategory category;
  private BigDecimal amount;
  private TransactionStatus status;
  private AnomalyFlag anomalyFlag;
  private OffsetDateTime createdAt;
  private OffsetDateTime completedAt;

  // Source Account & Owner Details
  private String sourceAccountNumber;
  private String sourceAccountCurrency;
  private AccountStatus sourceAccountStatus;
  private String sourceUserName;
  private String sourceUserEmail;
  private String sourceUserPhoneNumber;

  // Destination Account & Owner Details
  private String destinationAccountNumber;
  private String destinationAccountCurrency;
  private AccountStatus destinationAccountStatus;
  private String destinationUserName;
  private String destinationUserEmail;
  private String destinationUserPhoneNumber;

  public static AdminBankTransactionDetailDto from(BankTransaction transaction) {
    AdminBankTransactionDetailDto dto = new AdminBankTransactionDetailDto();
    dto.setId(transaction.getId());
    dto.setTransactionReference(transaction.getTransactionReference());
    if (transaction.getReversedTransaction() != null) {
      dto.setReversedTransactionReference(transaction.getReversedTransaction().getTransactionReference());
    }
    dto.setType(transaction.getType());
    dto.setDescription(transaction.getDescription());
    dto.setCategory(transaction.getCategory());
    dto.setAmount(transaction.getAmount());
    dto.setStatus(transaction.getStatus());
    dto.setAnomalyFlag(transaction.getAnomalyFlag());
    dto.setCreatedAt(transaction.getCreatedAt());
    dto.setCompletedAt(transaction.getCompletedAt());

    if (transaction.getSourceAccountHolder() != null) {
      AccountHolder source = transaction.getSourceAccountHolder();
      dto.setSourceAccountNumber(source.getAccountNumber());
      dto.setSourceAccountCurrency(source.getCurrency());
      dto.setSourceAccountStatus(source.getAccountStatus());
      if (source.getUser() != null) {
        dto.setSourceUserName(source.getUser().getName());
        dto.setSourceUserEmail(source.getUser().getEmail());
        dto.setSourceUserPhoneNumber(source.getUser().getPhoneNumber());
      }
    }

    if (transaction.getDestinationAccountHolder() != null) {
      AccountHolder dest = transaction.getDestinationAccountHolder();
      dto.setDestinationAccountNumber(dest.getAccountNumber());
      dto.setDestinationAccountCurrency(dest.getCurrency());
      dto.setDestinationAccountStatus(dest.getAccountStatus());
      if (dest.getUser() != null) {
        dto.setDestinationUserName(dest.getUser().getName());
        dto.setDestinationUserEmail(dest.getUser().getEmail());
        dto.setDestinationUserPhoneNumber(dest.getUser().getPhoneNumber());
      }
    }

    return dto;
  }
}
