package com.redmath.redbank.transaction.dto;

import com.redmath.redbank.transaction.BankTransaction;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

@Getter
@Setter
public class BankTransactionDetailDto extends BankTransactionDto {

  private String sourceAccountHolderName;
  private String destinationAccountHolderName;

  public static BankTransactionDetailDto fromDetail(BankTransaction transaction) {
    BankTransactionDetailDto dto = new BankTransactionDetailDto();
    dto.setId(transaction.getId());
    dto.setTransactionReference(transaction.getTransactionReference());
    if (transaction.getSourceAccountHolder() != null) {
      dto.setSourceAccountNumber(transaction.getSourceAccountHolder().getAccountNumber());
      if (Hibernate.isInitialized(transaction.getSourceAccountHolder())
          && transaction.getSourceAccountHolder().getUser() != null
          && Hibernate.isInitialized(transaction.getSourceAccountHolder().getUser())) {
        dto.setSourceAccountHolderName(transaction.getSourceAccountHolder().getUser().getName());
      }
    }
    if (transaction.getDestinationAccountHolder() != null) {
      dto.setDestinationAccountNumber(transaction.getDestinationAccountHolder().getAccountNumber());
      if (Hibernate.isInitialized(transaction.getDestinationAccountHolder())
          && transaction.getDestinationAccountHolder().getUser() != null
          && Hibernate.isInitialized(transaction.getDestinationAccountHolder().getUser())) {
        dto.setDestinationAccountHolderName(
            transaction.getDestinationAccountHolder().getUser().getName());
      }
    }
    dto.setType(transaction.getType());
    dto.setDescription(transaction.getDescription());
    dto.setAmount(transaction.getAmount());
    dto.setStatus(transaction.getStatus());
    dto.setCreatedAt(transaction.getCreatedAt());
    dto.setCompletedAt(transaction.getCompletedAt());
    return dto;
  }
}
