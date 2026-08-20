package com.redmath.redbank.balance.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminBalanceServiceTest {

  @Autowired
  private AdminBalanceService adminBalanceService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  @Test
  @DisplayName(
      "getLatestBalanceByAccountHolderId throws IllegalArgumentException when accountId is null")
  void getLatestBalanceByAccountHolderIdNullThrowsException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        adminBalanceService.getLatestBalanceByAccountHolderId(null));
    assertEquals("Account holder id is required", ex.getMessage());
  }

  @Test
  @DisplayName("getLatestBalanceByAccountHolderId throws ResourceNotFoundException when not found")
  void getLatestBalanceByAccountHolderIdNotFoundThrowsException() {
    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
        adminBalanceService.getLatestBalanceByAccountHolderId(888888L));
    assertEquals("Balance not found for account holder id: 888888", ex.getMessage());
  }

  @Test
  @DisplayName("getLatestBalanceByAccountHolderId returns balance when present")
  void getLatestBalanceByAccountHolderIdSuccess() {
    AccountHolder accountHolder = createAccountHolder(
        "admin.svc.find@example.com", "RB-ADM-SVC-001");
    BankTransaction tx = createTransaction(
        accountHolder, "TX-ADM-SVC-001", new BigDecimal("300.00"));

    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransactionId(tx.getId());
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setAmount(new BigDecimal("300.00"));
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(new BigDecimal("300.00"));
    balanceRepository.save(balance);

    Balance result = adminBalanceService.getLatestBalanceByAccountHolderId(accountHolder.getId());
    assertNotNull(result);
    assertEquals(new BigDecimal("300.00"), result.getRunningBalance());
  }

  @Test
  @DisplayName("getBalanceLedgerByAccountHolderId validates input parameters")
  void getBalanceLedgerByAccountHolderIdValidations() {
    Pageable validPageable = PageRequest.of(0, 10);

    // Account ID null
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
        adminBalanceService.getBalanceLedgerByAccountHolderId(null, validPageable));
    assertEquals("Account holder id is required", ex1.getMessage());

    // Page number < 0
    Pageable invalidPage = org.mockito.Mockito.mock(Pageable.class);
    org.mockito.Mockito.when(invalidPage.getPageNumber()).thenReturn(-1);
    IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
        adminBalanceService.getBalanceLedgerByAccountHolderId(1L, invalidPage));
    assertEquals("Page index cannot be negative", ex2.getMessage());

    // Page size <= 0
    Pageable invalidSize = org.mockito.Mockito.mock(Pageable.class);
    org.mockito.Mockito.when(invalidSize.getPageNumber()).thenReturn(0);
    org.mockito.Mockito.when(invalidSize.getPageSize()).thenReturn(0);
    IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
        adminBalanceService.getBalanceLedgerByAccountHolderId(1L, invalidSize));
    assertEquals("Page size must be positive", ex3.getMessage());
  }

  @Test
  @DisplayName("getBalanceLedgerByAccountHolderId returns paged balances")
  void getBalanceLedgerByAccountHolderIdSuccess() {
    AccountHolder accountHolder = createAccountHolder(
        "admin.svc.page@example.com", "RB-ADM-SVC-002");
    BankTransaction tx = createTransaction(
        accountHolder, "TX-ADM-SVC-002", new BigDecimal("100.00"));

    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransactionId(tx.getId());
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setAmount(new BigDecimal("100.00"));
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(new BigDecimal("100.00"));
    balanceRepository.save(balance);

    Page<Balance> page = adminBalanceService.getBalanceLedgerByAccountHolderId(
        accountHolder.getId(), PageRequest.of(0, 10));
    assertNotNull(page);
    assertEquals(1, page.getTotalElements());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Admin Svc Test User")
        .address("123 Main St, NY")
        .phoneNumber("+1999777" + accountNumber.hashCode())
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

  private BankTransaction createTransaction(AccountHolder ah, String ref, BigDecimal amount) {
    BankTransaction tx = new BankTransaction();
    tx.setTransactionReference(ref);
    tx.setSourceAccountHolder(ah);
    tx.setDestinationAccountHolder(ah);
    tx.setType(TransactionType.DEPOSIT);
    tx.setAmount(amount);
    tx.setStatus(TransactionStatus.COMPLETED);
    tx.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return bankTransactionRepository.save(tx);
  }
}
