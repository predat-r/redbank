package com.redmath.redbank.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountHolderServiceTest {

  @Autowired
  private AccountHolderService accountHolderService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Test
  @DisplayName("getAccountHolderByUserId validates input and throws ResourceNotFoundException when missing")
  void getAccountHolderByUserIdValidations() {
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.getAccountHolderByUserId(null));
    assertEquals("User id is required", ex1.getMessage());

    ResourceNotFoundException ex2 = assertThrows(ResourceNotFoundException.class, () ->
        accountHolderService.getAccountHolderByUserId(888888L));
    assertEquals("Account holder not found for user id: 888888", ex2.getMessage());
  }

  @Test
  @DisplayName("getAccountHolderByUserId returns account holder when found")
  void getAccountHolderByUserIdSuccess() {
    AccountHolder created = createAccountHolder("svc.user1@example.com", "RB-AH-SVC-001");

    AccountHolder result = accountHolderService.getAccountHolderByUserId(created.getUser().getId());
    assertNotNull(result);
    assertEquals("RB-AH-SVC-001", result.getAccountNumber());
  }

  @Test
  @DisplayName("lockById validates input and throws ResourceNotFoundException when missing")
  void lockByIdValidations() {
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.lockById(null));
    assertEquals("Account holder id is required", ex1.getMessage());

    ResourceNotFoundException ex2 = assertThrows(ResourceNotFoundException.class, () ->
        accountHolderService.lockById(888888L));
    assertEquals("Account holder not found: 888888", ex2.getMessage());
  }

  @Test
  @DisplayName("lockById returns account holder when found")
  void lockByIdSuccess() {
    AccountHolder created = createAccountHolder("svc.user2@example.com", "RB-AH-SVC-002");

    AccountHolder result = accountHolderService.lockById(created.getId());
    assertNotNull(result);
    assertEquals(created.getId(), result.getId());
  }

  @Test
  @DisplayName("getAccountHolderNameByAccountNumber validates input and throws ResourceNotFoundException when missing")
  void getAccountHolderNameByAccountNumberValidations() {
    assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.getAccountHolderNameByAccountNumber(null));

    assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.getAccountHolderNameByAccountNumber("  "));

    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
        accountHolderService.getAccountHolderNameByAccountNumber("NON-EXISTENT"));
    assertEquals("Account holder not found for account number: NON-EXISTENT", ex.getMessage());
  }

  @Test
  @DisplayName("getAccountHolderNameByAccountNumber returns user name when found")
  void getAccountHolderNameByAccountNumberSuccess() {
    AccountHolder created = createAccountHolder("svc.user3@example.com", "RB-AH-SVC-003");

    String name = accountHolderService.getAccountHolderNameByAccountNumber("RB-AH-SVC-003");
    assertEquals("Account Service User", name);
  }

  @Test
  @DisplayName("findByUser and findByAccountNumber return Optional AccountHolder")
  void findByUserAndAccountNumber() {
    AccountHolder created = createAccountHolder("svc.user4@example.com", "RB-AH-SVC-004");

    assertTrue(accountHolderService.findByUser(created.getUser()).isPresent());
    assertTrue(accountHolderService.findByAccountNumber("RB-AH-SVC-004").isPresent());
  }

  @Test
  @DisplayName("createAccountHolder validates user input and conflict")
  void createAccountHolderValidations() {
    assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.createAccountHolder(null));

    User unpersistedUser = User.builder().email("unpersisted@example.com").build();
    assertThrows(IllegalArgumentException.class, () ->
        accountHolderService.createAccountHolder(unpersistedUser));

    AccountHolder created = createAccountHolder("svc.user5@example.com", "RB-AH-SVC-005");
    ConflictException ex = assertThrows(ConflictException.class, () ->
        accountHolderService.createAccountHolder(created.getUser()));
    assertEquals("User already has an account holder record", ex.getMessage());
  }

  @Test
  @DisplayName("createAccountHolder creates account successfully for new user")
  void createAccountHolderSuccess() {
    User newUser = userRepository.save(User.builder()
        .email("svc.newuser@example.com")
        .name("New Service User")
        .address("456 Park Ave, NY")
        .phoneNumber("+19994440001")
        .passwordHash("hash")
        .status(UserStatus.ACTIVE)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build());

    AccountHolder result = accountHolderService.createAccountHolder(newUser);
    assertNotNull(result);
    assertNotNull(result.getId());
    assertTrue(result.getAccountNumber().startsWith("RB"));
    assertEquals(AccountStatus.ACTIVE, result.getAccountStatus());
    assertEquals("USD", result.getCurrency());
  }

  @Test
  @DisplayName("freezeMyAccountHolder handles ACTIVE, already FROZEN, and CLOSED accounts")
  void freezeMyAccountHolderScenarios() {
    AccountHolder ah = createAccountHolder("svc.user6@example.com", "RB-AH-SVC-006");

    // Freeze active account
    accountHolderService.freezeMyAccountHolder(ah.getUser().getId());
    assertEquals(AccountStatus.FROZEN, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Freeze already frozen account (no-op)
    accountHolderService.freezeMyAccountHolder(ah.getUser().getId());
    assertEquals(AccountStatus.FROZEN, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Freeze closed account (conflict)
    ah.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(ah);
    assertThrows(ConflictException.class, () ->
        accountHolderService.freezeMyAccountHolder(ah.getUser().getId()));
  }

  @Test
  @DisplayName("deactivateMyAccountHolder handles ACTIVE and already CLOSED accounts")
  void deactivateMyAccountHolderScenarios() {
    AccountHolder ah = createAccountHolder("svc.user7@example.com", "RB-AH-SVC-007");

    // Deactivate active account
    accountHolderService.deactivateMyAccountHolder(ah.getUser().getId());
    assertEquals(AccountStatus.CLOSED, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());

    // Deactivate already closed account (no-op)
    accountHolderService.deactivateMyAccountHolder(ah.getUser().getId());
    assertEquals(AccountStatus.CLOSED, accountHolderRepository.findById(ah.getId()).orElseThrow().getAccountStatus());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Account Service User")
        .address("123 Main St, NY")
        .phoneNumber("+1999444" + Math.abs(accountNumber.hashCode()))
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
