package com.redmath.redbank.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankTransactionDto {

    private Long id;
    private String transactionReference;
    private Long sourceAccountHolderId;
    private Long destinationAccountHolderId;
    private TransactionType type;
    private String description;
    private BigDecimal amount;
    private TransactionStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;

    public static BankTransactionDto from(BankTransaction transaction) {
        BankTransactionDto dto = new BankTransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionReference(transaction.getTransactionReference());
        if (transaction.getSourceAccountHolder() != null) {
            dto.setSourceAccountHolderId(transaction.getSourceAccountHolder().getId());
        }
        if (transaction.getDestinationAccountHolder() != null) {
            dto.setDestinationAccountHolderId(transaction.getDestinationAccountHolder().getId());
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
