package com.redmath.redbank.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.transaction.dto.AdminBankTransactionDetailDto;
import com.redmath.redbank.transaction.dto.BankTransactionDto;
import com.redmath.redbank.user.User;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BankTransactionDtoTest {

  @Test
  @DisplayName("BankTransactionDto.from maps transfer transaction fields accurately")
  void fromBankTransactionTransferMapping() {
    AccountHolder source = new AccountHolder();
    source.setAccountNumber("SRC-123");

    AccountHolder dest = new AccountHolder();
    dest.setAccountNumber("DEST-456");

    BankTransaction transaction = new BankTransaction();
    transaction.setId(10L);
    transaction.setTransactionReference("TXN-REF-1");
    transaction.setType(TransactionType.TRANSFER);
    transaction.setAmount(new BigDecimal("150.00"));
    transaction.setDescription("Test mapping");
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    transaction.setSourceAccountHolder(source);
    transaction.setDestinationAccountHolder(dest);

    BankTransactionDto dto = BankTransactionDto.from(transaction);

    assertNotNull(dto);
    assertEquals(10L, dto.getId());
    assertEquals("TXN-REF-1", dto.getTransactionReference());
    assertEquals(TransactionType.TRANSFER, dto.getType());
    assertEquals(new BigDecimal("150.00"), dto.getAmount());
    assertEquals("SRC-123", dto.getSourceAccountNumber());
    assertEquals("DEST-456", dto.getDestinationAccountNumber());
  }

  @Test
  @DisplayName("AdminBankTransactionDetailDto.from maps full details including user info")
  void fromAdminBankTransactionDetailDtoFullMapping() {
    User srcUser = User.builder()
        .name("Alice")
        .email("alice@example.com")
        .phoneNumber("03001112223")
        .build();

    AccountHolder source = new AccountHolder();
    source.setAccountNumber("SRC-789");
    source.setCurrency("USD");
    source.setAccountStatus(AccountStatus.ACTIVE);
    source.setUser(srcUser);

    User destUser = User.builder()
        .name("Bob")
        .email("bob@example.com")
        .phoneNumber("03004445556")
        .build();

    AccountHolder dest = new AccountHolder();
    dest.setAccountNumber("DEST-999");
    dest.setCurrency("EUR");
    dest.setAccountStatus(AccountStatus.ACTIVE);
    dest.setUser(destUser);

    BankTransaction transaction = new BankTransaction();
    transaction.setId(20L);
    transaction.setTransactionReference("TXN-REF-2");
    transaction.setType(TransactionType.TRANSFER);
    transaction.setAmount(new BigDecimal("300.00"));
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setSourceAccountHolder(source);
    transaction.setDestinationAccountHolder(dest);

    AdminBankTransactionDetailDto dto = AdminBankTransactionDetailDto.from(transaction);

    assertNotNull(dto);
    assertEquals(20L, dto.getId());
    assertEquals("SRC-789", dto.getSourceAccountNumber());
    assertEquals("Alice", dto.getSourceUserName());
    assertEquals("alice@example.com", dto.getSourceUserEmail());
    assertEquals("03001112223", dto.getSourceUserPhoneNumber());
    assertEquals("DEST-999", dto.getDestinationAccountNumber());
    assertEquals("Bob", dto.getDestinationUserName());
    assertEquals("bob@example.com", dto.getDestinationUserEmail());
    assertEquals("03004445556", dto.getDestinationUserPhoneNumber());
  }

  @Test
  @DisplayName("AdminBankTransactionDetailDto.from handles null source or destination account gracefully")
  void fromAdminBankTransactionDetailDtoNullAccounts() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(30L);
    transaction.setType(TransactionType.WITHDRAWAL);

    AdminBankTransactionDetailDto dto = AdminBankTransactionDetailDto.from(transaction);

    assertNotNull(dto);
    assertNull(dto.getSourceAccountNumber());
    assertNull(dto.getDestinationAccountNumber());
  }
}
