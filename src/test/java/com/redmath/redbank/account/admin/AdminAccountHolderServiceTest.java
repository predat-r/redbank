package com.redmath.redbank.account.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminAccountHolderServiceTest {

  @Autowired
  private AdminAccountHolderService adminAccountHolderService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Test
  @DisplayName("getAccountHolderById validates input and throws ResourceNotFoundException when missing")
  void getAccountHolderByIdValidations() {
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
        adminAccountHolderService.getAccountHolderById(null));
    assertEquals("Account id is required", ex1.getMessage());

    ResourceNotFoundException ex2 = assertThrows(ResourceNotFoundException.class, () ->
        adminAccountHolderService.getAccountHolderById(888888L));
    assertEquals("Account holder not found: 888888", ex2.getMessage());
  }

  @Test
  @DisplayName("getAccountHolderById returns AccountHolder when present")
  void getAccountHolderByIdSuccess() {
    AccountHolder ah = createAccountHolder("adm.svc1@example.com", "RB-ADM-AH-001");

    AccountHolder result = adminAccountHolderService.getAccountHolderById(ah.getId());
    assertNotNull(result);
    assertEquals("RB-ADM-AH-001", result.getAccountNumber());
  }

  @Test
  @DisplayName("getAllAccountHolders returns paged results")
  void getAllAccountHoldersSuccess() {
    createAccountHolder("adm.svc2@example.com", "RB-ADM-AH-002");

    Page<AccountHolder> page = adminAccountHolderService.getAllAccountHolders(PageRequest.of(0, 10));
    assertNotNull(page);
  }

  @Test
  @DisplayName("freezeAccountHolder handles ACTIVE, already FROZEN, and CLOSED accounts")
  void freezeAccountHolderScenarios() {
    AccountHolder ah = createAccountHolder("adm.svc3@example.com", "RB-ADM-AH-003");
    Long adminUserId = 1L;

    // Freeze active account
    adminAccountHolderService.freezeAccountHolder(ah.getId(), adminUserId);
    assertEquals(AccountStatus.FROZEN, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Freeze already frozen account (no-op)
    adminAccountHolderService.freezeAccountHolder(ah.getId(), adminUserId);
    assertEquals(AccountStatus.FROZEN, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Freeze closed account (conflict)
    ah.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(ah);
    assertThrows(ConflictException.class, () ->
        adminAccountHolderService.freezeAccountHolder(ah.getId(), adminUserId));
  }

  @Test
  @DisplayName("deactivateAccountHolder handles ACTIVE and already CLOSED accounts")
  void deactivateAccountHolderScenarios() {
    AccountHolder ah = createAccountHolder("adm.svc4@example.com", "RB-ADM-AH-004");
    Long adminUserId = 1L;

    // Deactivate active account
    adminAccountHolderService.deactivateAccountHolder(ah.getId(), adminUserId);
    assertEquals(AccountStatus.CLOSED, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Deactivate already closed account (no-op)
    adminAccountHolderService.deactivateAccountHolder(ah.getId(), adminUserId);
    assertEquals(AccountStatus.CLOSED, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Admin Service Account User")
        .address("123 Main St, NY")
        .phoneNumber("+1999333" + Math.abs(accountNumber.hashCode()))
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber(accountNumber);
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    accountHolder.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return accountHolderRepository.save(accountHolder);
  }
}
