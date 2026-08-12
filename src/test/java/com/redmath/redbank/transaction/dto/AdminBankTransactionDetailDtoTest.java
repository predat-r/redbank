package com.redmath.redbank.transaction.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.anomaly.AnomalyFlag;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.user.User;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminBankTransactionDetailDtoTest {

  @Test
  @DisplayName("from returns null when transaction is null")
  void fromNullTransaction() {
    assertNull(AdminBankTransactionDetailDto.from(null));
  }

  @Test
  @DisplayName("from maps transaction with null account holders correctly")
  void fromMinimalTransaction() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(1L);
    transaction.setTransactionReference("TXN-001");
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setAmount(new BigDecimal("100.00"));
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setAnomalyFlag(AnomalyFlag.NONE);

    AdminBankTransactionDetailDto dto = AdminBankTransactionDetailDto.from(transaction);

    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals("TXN-001", dto.getTransactionReference());
    assertNull(dto.getSourceAccountNumber());
    assertNull(dto.getDestinationAccountNumber());
  }

  @Test
  @DisplayName("from maps transaction with full source and destination details")
  void fromFullTransaction() {
    User sourceUser = User.builder()
        .name("Source User")
        .email("source@example.com")
        .phoneNumber("03001112233")
        .build();

    AccountHolder source = new AccountHolder();
    source.setAccountNumber("RB-SRC");
    source.setCurrency("USD");
    source.setAccountStatus(AccountStatus.ACTIVE);
    source.setUser(sourceUser);

    User destUser = User.builder()
        .name("Dest User")
        .email("dest@example.com")
        .phoneNumber("03004445566")
        .build();

    AccountHolder dest = new AccountHolder();
    dest.setAccountNumber("RB-DEST");
    dest.setCurrency("USD");
    dest.setAccountStatus(AccountStatus.ACTIVE);
    dest.setUser(destUser);

    BankTransaction reversed = new BankTransaction();
    reversed.setTransactionReference("TXN-ORIG");

    BankTransaction transaction = new BankTransaction();
    transaction.setId(2L);
    transaction.setTransactionReference("TXN-REV");
    transaction.setType(TransactionType.TRANSFER);
    transaction.setAmount(new BigDecimal("200.00"));
    transaction.setStatus(TransactionStatus.CANCELLED);
    transaction.setAnomalyFlag(AnomalyFlag.HIGH);
    transaction.setSourceAccountHolder(source);
    transaction.setDestinationAccountHolder(dest);
    transaction.setReversedTransaction(reversed);
    transaction.setCreatedAt(OffsetDateTime.now());
    transaction.setCompletedAt(OffsetDateTime.now());

    AdminBankTransactionDetailDto dto = AdminBankTransactionDetailDto.from(transaction);

    assertNotNull(dto);
    assertEquals("TXN-ORIG", dto.getReversedTransactionReference());
    assertEquals("RB-SRC", dto.getSourceAccountNumber());
    assertEquals("Source User", dto.getSourceUserName());
    assertEquals("source@example.com", dto.getSourceUserEmail());
    assertEquals("03001112233", dto.getSourceUserPhoneNumber());
    assertEquals("RB-DEST", dto.getDestinationAccountNumber());
    assertEquals("Dest User", dto.getDestinationUserName());
    assertEquals("dest@example.com", dto.getDestinationUserEmail());
    assertEquals("03004445566", dto.getDestinationUserPhoneNumber());
  }
}
