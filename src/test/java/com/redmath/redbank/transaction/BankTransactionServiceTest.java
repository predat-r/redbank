package com.redmath.redbank.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.balance.BalanceIndicator;
import com.redmath.redbank.balance.BalanceService;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.transaction.request.WithdrawalRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BankTransactionServiceTest {

  @Mock
  private BankTransactionRepository bankTransactionRepository;

  @Mock
  private AccountHolderService accountHolderService;

  @Mock
  private AuditService auditService;

  @Mock
  private BalanceService balanceService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private BankTransactionService bankTransactionService;

  @BeforeEach
  void setUp() {
    bankTransactionService = new BankTransactionService(
        bankTransactionRepository,
        accountHolderService,
        auditService,
        balanceService,
        eventPublisher
    );
  }

  @Test
  @DisplayName("getTransactionsForUser fetches transactions for given user")
  void getTransactionsForUserSuccess() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(10L);

    when(accountHolderService.getAccountHolderByUserId(10L)).thenReturn(accountHolder);
    when(bankTransactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<BankTransaction> result = bankTransactionService.getTransactionsForUser(10L, Pageable.unpaged());

    assertNotNull(result);
    verify(bankTransactionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Pageable.unpaged()));
  }

  @Test
  @DisplayName("getTransactionsForUser with filters executes repository findAll with specification")
  void getTransactionsForUserWithFiltersSuccess() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(10L);

    when(accountHolderService.getAccountHolderByUserId(10L)).thenReturn(accountHolder);
    when(bankTransactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<BankTransaction> result = bankTransactionService.getTransactionsForUser(
        10L, "RB12345", TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
        null, java.time.OffsetDateTime.now().minusDays(1), java.time.OffsetDateTime.now(),
        Pageable.unpaged());

    assertNotNull(result);
    verify(bankTransactionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Pageable.unpaged()));
  }

  @Test
  @DisplayName("getAllTransactions fetches all transactions with pagination")
  void getAllTransactionsSuccess() {
    when(bankTransactionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    Page<BankTransaction> result = bankTransactionService.getAllTransactions(Pageable.unpaged());

    assertNotNull(result);
    verify(bankTransactionRepository).findAll(Pageable.unpaged());
  }

  @Test
  @DisplayName("getAllTransactions with filter parameters executes repository findAll with specification")
  void getAllTransactionsWithFiltersSuccess() {
    when(bankTransactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<BankTransaction> result = bankTransactionService.getAllTransactions(
        "TXN", "RB12345", TransactionType.DEPOSIT, TransactionStatus.COMPLETED,
        null, null, java.time.OffsetDateTime.now().minusDays(1), java.time.OffsetDateTime.now(),
        Pageable.unpaged());

    assertNotNull(result);
    verify(bankTransactionRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Pageable.unpaged()));
  }

  @Test
  @DisplayName("getTransactionsByAccountNumber fetches transactions for matching account number")
  void getTransactionsByAccountNumberSuccess() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(5L);

    when(accountHolderService.findByAccountNumber("RB123456")).thenReturn(Optional.of(accountHolder));
    when(bankTransactionRepository.findBySourceAccountHolderIdOrDestinationAccountHolderId(eq(5L), eq(5L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    Page<BankTransaction> result = bankTransactionService.getTransactionsByAccountNumber("RB123456", Pageable.unpaged());

    assertNotNull(result);
    verify(bankTransactionRepository).findBySourceAccountHolderIdOrDestinationAccountHolderId(5L, 5L, Pageable.unpaged());
  }

  @Test
  @DisplayName("getTransactionsByAccountNumber throws ResourceNotFoundException when account not found")
  void getTransactionsByAccountNumberNotFound() {
    when(accountHolderService.findByAccountNumber("RB999999")).thenReturn(Optional.empty());
    Pageable pageable = Pageable.unpaged();

    assertThrows(ResourceNotFoundException.class,
        () -> bankTransactionService.getTransactionsByAccountNumber("RB999999", pageable));
  }

  @Test
  @DisplayName("getTransactionById returns transaction when found")
  void getTransactionByIdSuccess() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(1L);

    when(bankTransactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

    BankTransaction result = bankTransactionService.getTransactionById(1L);

    assertEquals(1L, result.getId());
  }

  @Test
  @DisplayName("getTransactionById throws ResourceNotFoundException when not found")
  void getTransactionByIdNotFound() {
    when(bankTransactionRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> bankTransactionService.getTransactionById(999L));
  }

  @Test
  @DisplayName("getTransactionByReference returns transaction when found")
  void getTransactionByReferenceSuccess() {
    BankTransaction transaction = new BankTransaction();
    transaction.setTransactionReference("TXN-REF-100");

    when(bankTransactionRepository.findByTransactionReference("TXN-REF-100")).thenReturn(Optional.of(transaction));

    BankTransaction result = bankTransactionService.getTransactionByReference("TXN-REF-100");

    assertEquals("TXN-REF-100", result.getTransactionReference());
  }

  @Test
  @DisplayName("getTransactionByReference throws ResourceNotFoundException when not found")
  void getTransactionByReferenceNotFound() {
    when(bankTransactionRepository.findByTransactionReference("TXN-INVALID")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> bankTransactionService.getTransactionByReference("TXN-INVALID"));
  }

  @Test
  @DisplayName("deposit creates deposit transaction and records audit and ledger")
  void depositSuccess() {
    AccountHolder target = new AccountHolder();
    target.setId(1L);
    target.setAccountNumber("RB-DEST-001");
    target.setAccountStatus(AccountStatus.ACTIVE);

    when(accountHolderService.findByAccountNumber("RB-DEST-001")).thenReturn(Optional.of(target));
    when(bankTransactionRepository.save(any(BankTransaction.class))).thenAnswer(invocation -> {
      BankTransaction tx = invocation.getArgument(0);
      tx.setId(100L);
      return tx;
    });

    DepositRequest request = new DepositRequest();
    request.setAccountNumber("RB-DEST-001");
    request.setAmount(new BigDecimal("500.00"));
    request.setDescription("Test Deposit");

    BankTransaction result = bankTransactionService.deposit(99L, request);

    assertNotNull(result);
    assertEquals(TransactionType.DEPOSIT, result.getType());
    assertEquals(new BigDecimal("500.00"), result.getAmount());
    assertEquals(null, result.getCategory());
    verify(balanceService).recordLedgerEntry(eq(target), any(BankTransaction.class), eq(BalanceIndicator.CREDIT));
    verify(auditService).recordAuditLog(eq(99L), eq(AuditAction.ADMIN_DEPOSIT_RECORDED), eq(AuditTargetType.TRANSACTION), eq("100"), eq(null));
  }

  @Test
  @DisplayName("withdraw creates withdrawal transaction and records ledger")
  void withdrawSuccess() {
    AccountHolder source = new AccountHolder();
    source.setId(1L);
    source.setAccountNumber("RB-SRC-001");
    source.setAccountStatus(AccountStatus.ACTIVE);

    when(accountHolderService.getAccountHolderByUserId(10L)).thenReturn(source);
    when(bankTransactionRepository.save(any(BankTransaction.class))).thenAnswer(invocation -> {
      BankTransaction tx = invocation.getArgument(0);
      tx.setId(101L);
      return tx;
    });

    WithdrawalRequest request = new WithdrawalRequest();
    request.setAmount(new BigDecimal("100.00"));
    request.setDescription("Test Withdrawal");
    request.setCategory(TransactionCategory.FOOD);

    BankTransaction result = bankTransactionService.withdraw(10L, request);

    assertNotNull(result);
    assertEquals(TransactionType.WITHDRAWAL, result.getType());
    assertEquals(new BigDecimal("100.00"), result.getAmount());
    assertEquals(TransactionCategory.FOOD, result.getCategory());
    assertEquals(TransactionStatus.PENDING, result.getStatus());
    verify(balanceService).recordLedgerEntry(eq(source), any(BankTransaction.class), eq(BalanceIndicator.DEBIT));
    verify(eventPublisher).publishEvent(any(com.redmath.redbank.transaction.event.TransactionSubmittedEvent.class));
  }

  @Test
  @DisplayName("transfer creates transfer transaction between source and destination in PENDING status")
  void transferSuccess() {
    AccountHolder source = new AccountHolder();
    source.setId(1L);
    source.setAccountNumber("RB-SRC-001");
    source.setAccountStatus(AccountStatus.ACTIVE);

    AccountHolder dest = new AccountHolder();
    dest.setId(2L);
    dest.setAccountNumber("RB-DEST-002");
    dest.setAccountStatus(AccountStatus.ACTIVE);

    when(accountHolderService.getAccountHolderByUserId(10L)).thenReturn(source);
    when(accountHolderService.findByAccountNumber("RB-DEST-002")).thenReturn(Optional.of(dest));
    when(bankTransactionRepository.save(any(BankTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TransferRequest request = new TransferRequest();
    request.setDestinationAccountNumber("RB-DEST-002");
    request.setAmount(new BigDecimal("200.00"));
    request.setDescription("Test Transfer");
    request.setCategory(TransactionCategory.GROCERY);

    BankTransaction result = bankTransactionService.transfer(10L, request);

    assertNotNull(result);
    assertEquals(TransactionType.TRANSFER, result.getType());
    assertEquals(TransactionCategory.GROCERY, result.getCategory());
    assertEquals(TransactionStatus.PENDING, result.getStatus());
    verify(balanceService).recordLedgerEntry(eq(source), any(BankTransaction.class), eq(BalanceIndicator.DEBIT));
    verify(eventPublisher).publishEvent(any(com.redmath.redbank.transaction.event.TransactionSubmittedEvent.class));
  }
}
