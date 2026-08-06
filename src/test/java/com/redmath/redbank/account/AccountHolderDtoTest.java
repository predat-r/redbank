package com.redmath.redbank.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.account.dto.AccountHolderDto;
import com.redmath.redbank.user.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountHolderDtoTest {

  @Test
  @DisplayName("AccountHolderDto.from(accountHolder) with null user sets userId to null")
  void fromAccountHolderNullUser() {
    AccountHolder ah = new AccountHolder();
    ah.setId(100L);
    ah.setAccountNumber("RB-DTO-001");
    ah.setCurrency("USD");
    ah.setAccountStatus(AccountStatus.ACTIVE);

    AccountHolderDto dto = AccountHolderDto.from(ah);
    assertEquals(100L, dto.getId());
    assertNull(dto.getUserId());
    assertEquals("RB-DTO-001", dto.getAccountNumber());
    assertEquals("USD", dto.getCurrency());
    assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
  }

  @Test
  @DisplayName("AccountHolderDto.from(accountHolder) populates all fields when user present")
  void fromAccountHolderPopulatedUser() {
    User user = User.builder().email("dto@example.com").build();
    // Using reflection or id mapping if user has id
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    AccountHolder ah = new AccountHolder();
    ah.setId(200L);
    ah.setUser(user);
    ah.setAccountNumber("RB-DTO-002");
    ah.setCurrency("USD");
    ah.setAccountStatus(AccountStatus.FROZEN);
    ah.setApprovedAt(now);
    ah.setCreatedAt(now);
    ah.setUpdatedAt(now);

    AccountHolderDto dto = AccountHolderDto.from(ah);
    assertEquals(200L, dto.getId());
    assertEquals("RB-DTO-002", dto.getAccountNumber());
    assertEquals("USD", dto.getCurrency());
    assertEquals(AccountStatus.FROZEN, dto.getAccountStatus());
    assertEquals(now, dto.getApprovedAt());
    assertEquals(now, dto.getCreatedAt());
    assertEquals(now, dto.getUpdatedAt());
  }
}
