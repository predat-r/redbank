package com.redmath.redbank.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.common.exception.InsufficientFundsException;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceServiceTest {

  @Autowired
  private BalanceService balanceService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountHolderRepository accountHolderRepository;

  @Autowired
  private BankTransactionRepository bankTransactionRepository;

  @Autowired
  private BalanceRepository balanceRepository;

  @Test
  @DisplayName("getLatestBalanceByUserId throws IllegalArgumentException when userId is null")
  void getLatestBalanceByUserIdNullThrowsException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        balanceService.getLatestBalanceByUserId(null));
    assertEquals("User id is required", ex.getMessage());
  }

  @Test
  @DisplayName(
      "getBalanceByTransactionId throws IllegalArgumentException when transactionId is null")
  void getBalanceByTransactionIdNullThrowsException() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
        balanceService.getBalanceByTransactionId(null));
    assertEquals("Transaction id is required", ex.getMessage());
  }

  @Test
  @DisplayName("getBalanceByTransactionId throws ResourceNotFoundException when not found")
  void getBalanceByTransactionIdNotFoundThrowsException() {
    ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
        balanceService.getBalanceByTransactionId(999999L));
    assertEquals("Balance not found for transaction id: 999999", ex.getMessage());
  }

  @Test
  @DisplayName("getBalanceByTransactionId returns balance when found")
  void getBalanceByTransactionIdSuccess() {
    AccountHolder accountHolder = createAccountHolder("service.tx.found@example.com", "RB-SVC-001");
    BankTransaction tx = createTransaction(accountHolder, "TX-SVC-001", new BigDecimal("150.00"));
    
    Balance balance = new Balance();
    balance.setAccountHolder(accountHolder);
    balance.setTransactionId(tx.getId());
    balance.setEntryDate(OffsetDateTime.now(ZoneOffset.UTC));
    balance.setAmount(new BigDecimal("150.00"));
    balance.setIndicator(BalanceIndicator.CREDIT);
    balance.setRunningBalance(new BigDecimal("150.00"));
    balanceRepository.save(balance);

    Balance result = balanceService.getBalanceByTransactionId(tx.getId());
    assertNotNull(result);
    assertEquals(new BigDecimal("150.00"), result.getAmount());
    assertEquals(BalanceIndicator.CREDIT, result.getIndicator());
  }

  @Test
  @DisplayName("recordLedgerEntry validates inputs")
  void recordLedgerEntryValidations() {
    AccountHolder accountHolder = createAccountHolder("service.val@example.com", "RB-SVC-002");
    BankTransaction tx = createTransaction(accountHolder, "TX-SVC-002", new BigDecimal("100.00"));

    // AccountHolder null
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(null, tx.getId(), tx.getAmount(), BalanceIndicator.CREDIT));

    // AccountHolder ID null
    AccountHolder invalidAh = new AccountHolder();
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(invalidAh, tx.getId(), tx.getAmount(), BalanceIndicator.CREDIT));

    // Transaction ID null
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(accountHolder, null, tx.getAmount(), BalanceIndicator.CREDIT));

    // Transaction amount null
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(accountHolder, tx.getId(), null, BalanceIndicator.CREDIT));

    // Balance indicator null
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(accountHolder, tx.getId(), tx.getAmount(), null));

    // Zero or negative transaction amount
    assertThrows(IllegalArgumentException.class, () ->
        balanceService.recordLedgerEntry(accountHolder, tx.getId(), BigDecimal.ZERO, BalanceIndicator.CREDIT));
  }

  @Test
  @DisplayName("recordLedgerEntry throws InsufficientFundsException when DEBIT exceeds balance")
  void recordLedgerEntryInsufficientFunds() {
    AccountHolder accountHolder = createAccountHolder("service.funds@example.com", "RB-SVC-003");
    BankTransaction tx = createTransaction(accountHolder, "TX-SVC-003", new BigDecimal("100.00"));

    InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () ->
        balanceService.recordLedgerEntry(accountHolder, tx.getId(), tx.getAmount(), BalanceIndicator.DEBIT));
    assertEquals("Insufficient funds for this transaction", ex.getMessage());
  }

  @Test
  @DisplayName("recordLedgerEntry processes CREDIT and DEBIT correctly")
  void recordLedgerEntryCreditAndDebitSuccess() {
    AccountHolder accountHolder = createAccountHolder(
        "service.ledger@example.com", "RB-SVC-004");
    BankTransaction creditTx = createTransaction(
        accountHolder, "TX-SVC-004", new BigDecimal("200.00"));

    balanceService.recordLedgerEntry(accountHolder, creditTx.getId(), creditTx.getAmount(), BalanceIndicator.CREDIT);

    Balance latest1 = balanceService.getLatestBalanceByUserId(accountHolder.getUser().getId());
    assertEquals(new BigDecimal("200.00"), latest1.getRunningBalance());

    BankTransaction debitTx = createTransaction(
        accountHolder, "TX-SVC-005", new BigDecimal("50.00"));
    balanceService.recordLedgerEntry(accountHolder, debitTx.getId(), debitTx.getAmount(), BalanceIndicator.DEBIT);

    Balance latest2 = balanceService.getLatestBalanceByUserId(accountHolder.getUser().getId());
    assertEquals(new BigDecimal("150.00"), latest2.getRunningBalance());
  }

  private AccountHolder createAccountHolder(String email, String accountNumber) {
    User user = userRepository.save(User.builder()
        .email(email)
        .name("Service Test User")
        .address("123 Main St, NY")
        .phoneNumber("+1999888" + accountNumber.hashCode())
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
