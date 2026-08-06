package com.redmath.redbank.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.account.dto.AccountHolderDto;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountHolderDtoTest {

  @Test
  @DisplayName("AccountHolderDto.from(accountHolder) with null user sets user to null")
  void fromAccountHolderNullUser() {
    AccountHolder ah = new AccountHolder();
    ah.setId(100L);
    ah.setAccountNumber("RB-DTO-001");
    ah.setCurrency("USD");
    ah.setAccountStatus(AccountStatus.ACTIVE);

    AccountHolderDto dto = AccountHolderDto.from(ah);
    assertEquals(100L, dto.getId());
    assertNull(dto.getUser());
    assertEquals("RB-DTO-001", dto.getAccountNumber());
    assertEquals("USD", dto.getCurrency());
    assertEquals(AccountStatus.ACTIVE, dto.getAccountStatus());
  }

  @Test
  @DisplayName("AccountHolderDto.from(accountHolder) populates all fields when user present")
  void fromAccountHolderPopulatedUser() {
    User user = User.builder()
        .id(55L)
        .email("dto@example.com")
        .name("Test User")
        .phoneNumber("+123456789")
        .address("123 Street")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
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
    assertNotNull(dto.getUser());
    assertEquals(55L, dto.getUser().getId());
    assertEquals("dto@example.com", dto.getUser().getEmail());
    assertEquals("Test User", dto.getUser().getName());
    assertEquals("RB-DTO-002", dto.getAccountNumber());
    assertEquals("USD", dto.getCurrency());
    assertEquals(AccountStatus.FROZEN, dto.getAccountStatus());
    assertEquals(now, dto.getApprovedAt());
    assertEquals(now, dto.getCreatedAt());
    assertEquals(now, dto.getUpdatedAt());
  }
}
