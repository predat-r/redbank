package com.redmath.redbank.transaction.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.ai.anomaly.AnomalyFlag;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminBankTransactionDtoTest {

  @Test
  @DisplayName("from returns null when transaction is null")
  void fromNullTransaction() {
    assertNull(AdminBankTransactionDto.from(null));
  }

  @Test
  @DisplayName("from maps transaction to AdminBankTransactionDto correctly")
  void fromTransactionSuccess() {
    OffsetDateTime now = OffsetDateTime.now();
    BankTransaction transaction = new BankTransaction();
    transaction.setId(100L);
    transaction.setTransactionReference("TXN-ADMIN-001");
    transaction.setType(TransactionType.TRANSFER);
    transaction.setAmount(new BigDecimal("500.00"));
    transaction.setAnomalyFlag(AnomalyFlag.HIGH);
    transaction.setStatus(TransactionStatus.PENDING);
    transaction.setCreatedAt(now);

    AdminBankTransactionDto dto = AdminBankTransactionDto.from(transaction);

    assertNotNull(dto);
    assertEquals(100L, dto.getId());
    assertEquals("TXN-ADMIN-001", dto.getTransactionReference());
    assertEquals(TransactionType.TRANSFER, dto.getType());
    assertEquals(new BigDecimal("500.00"), dto.getAmount());
    assertEquals(AnomalyFlag.HIGH, dto.getAnomalyFlag());
    assertEquals(TransactionStatus.PENDING, dto.getStatus());
    assertEquals(now, dto.getCreatedAt());
  }
}
