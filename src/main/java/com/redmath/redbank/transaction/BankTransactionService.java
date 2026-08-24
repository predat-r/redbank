package com.redmath.redbank.transaction;

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
import com.redmath.redbank.transaction.spec.BankTransactionSpecification;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.redmath.redbank.transaction.event.TransactionCancelledEvent;
import com.redmath.redbank.transaction.event.TransactionCompletedEvent;
import com.redmath.redbank.transaction.event.TransactionSubmittedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankTransactionService {

  private final BankTransactionRepository bankTransactionRepository;
  private final AccountHolderService accountHolderService;
  private final AuditService auditService;
  private final BalanceService balanceService;
  private final ApplicationEventPublisher eventPublisher;

  public BankTransactionService(
      BankTransactionRepository bankTransactionRepository,
      AccountHolderService accountHolderService,
      AuditService auditService,
      BalanceService balanceService,
      ApplicationEventPublisher eventPublisher) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.accountHolderService = accountHolderService;
    this.auditService = auditService;
    this.balanceService = balanceService;
    this.eventPublisher = eventPublisher;
  }

  public Page<BankTransaction> getTransactionsForUser(
      Long userId,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      TransactionCategory category,
      OffsetDateTime fromDate,
      OffsetDateTime toDate,
      Pageable pageable) {
    AccountHolder accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    return bankTransactionRepository.findAll(
        BankTransactionSpecification.filterForUser(
            accountHolder.getId(), accountNumber, type, status, category, fromDate, toDate),
        pageable);
  }

  public Page<BankTransaction> getAllTransactions(
      String reference,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      TransactionCategory category,
      AnomalyFlag anomalyFlag,
      OffsetDateTime fromDate,
      OffsetDateTime toDate,
      Pageable pageable) {
    return bankTransactionRepository.findAll(
        BankTransactionSpecification.filter(reference, accountNumber, type, status, category,
            anomalyFlag, fromDate, toDate),
        pageable);
  }

  //admin method
  public Page<BankTransaction> getTransactionsByAccountNumber(String accountNumber,
      Pageable pageable) {
    AccountHolder accountHolder = accountHolderService.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Account holder not found"));
    return bankTransactionRepository.findBySourceAccountHolderIdOrDestinationAccountHolderId(
        accountHolder.getId(), accountHolder.getId(), pageable);
  }

  public BankTransaction getTransactionById(Long id) {
    return bankTransactionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + id));
  }

  public BankTransaction getUserTransactionById(Long userId, Long transactionId) {
    AccountHolder myAccount = accountHolderService.getAccountHolderByUserId(userId);
    BankTransaction transaction = getTransactionById(transactionId);

    boolean isSource = transaction.getSourceAccountHolder() != null
        && transaction.getSourceAccountHolder().getId().equals(myAccount.getId());
    boolean isDestination = transaction.getDestinationAccountHolder() != null
        && transaction.getDestinationAccountHolder().getId().equals(myAccount.getId());

    if (!isSource && !isDestination) {
      throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
    }

    // If transaction is pending, and user is not the sender, then don't show transaction.
    if (isDestination && !isSource && transaction.getStatus() != TransactionStatus.COMPLETED) {
      throw new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
    }

    return transaction;
  }

  public BankTransaction getTransactionByReference(String reference) {
    return bankTransactionRepository.findByTransactionReference(reference)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Transaction not found with reference: " + reference));
  }

  @Transactional
  public BankTransaction transfer(Long userId, TransferRequest request) {
    AccountHolder myAccount = getAndValidateInitiatorAccount(userId);
    lockAccount(myAccount.getId());
    BankTransaction transaction = buildPendingTransaction(TransactionType.TRANSFER,
        request.getAmount(), request.getDescription());
    transaction.setCategory(request.getCategory());
    processTransferRules(transaction, myAccount, request.getDestinationAccountNumber());

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(transaction.getSourceAccountHolder(), transaction.getId(),
        transaction.getAmount(), BalanceIndicator.DEBIT);

    eventPublisher.publishEvent(new TransactionSubmittedEvent(transaction.getId()));

    return transaction;
  }

  @Transactional
  public BankTransaction deposit(Long adminUserId, DepositRequest request) {
    AccountHolder targetAccount = accountHolderService.findByAccountNumber(
            request.getAccountNumber())
        .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

    if (targetAccount.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException(
          "Destination account is not active (status: " + targetAccount.getAccountStatus() + ")");
    }

    lockAccount(targetAccount.getId());

    BankTransaction transaction = buildCompletedTransaction(TransactionType.DEPOSIT,
        request.getAmount(),
        request.getDescription());
    transaction.setDestinationAccountHolder(targetAccount);

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(targetAccount, transaction.getId(), transaction.getAmount(),
        BalanceIndicator.CREDIT);
    auditService.recordAuditLog(adminUserId, AuditAction.ADMIN_DEPOSIT_RECORDED,
        AuditTargetType.TRANSACTION, transaction.getId().toString(), null);

    eventPublisher.publishEvent(new TransactionCompletedEvent(transaction.getId()));

    return transaction;
  }

  @Transactional
  public BankTransaction withdraw(Long userId, WithdrawalRequest request) {
    AccountHolder sourceAccount = getAndValidateInitiatorAccount(userId);
    lockAccount(sourceAccount.getId());

    BankTransaction transaction = buildPendingTransaction(TransactionType.WITHDRAWAL,
        request.getAmount(), request.getDescription());
    transaction.setCategory(request.getCategory());
    transaction.setSourceAccountHolder(sourceAccount);

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(sourceAccount, transaction.getId(), transaction.getAmount(),
        BalanceIndicator.DEBIT);

    eventPublisher.publishEvent(new TransactionSubmittedEvent(transaction.getId()));

    return transaction;
  }

  @Transactional
  public BankTransaction completePendingTransaction(Long transactionId) {
    BankTransaction transaction = bankTransactionRepository.findByIdWithLock(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));
    if (transaction.getStatus() != TransactionStatus.PENDING) {
      throw new IllegalStateException(
          "Transaction is not PENDING (current status: " + transaction.getStatus() + ")");
    }

    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCompletedAt(OffsetDateTime.now());
    transaction = bankTransactionRepository.save(transaction);

    if (transaction.getType() == TransactionType.TRANSFER
        && transaction.getDestinationAccountHolder() != null) {
      balanceService.recordLedgerEntry(transaction.getDestinationAccountHolder(), transaction.getId(),
          transaction.getAmount(), BalanceIndicator.CREDIT);
    }

    eventPublisher.publishEvent(new TransactionCompletedEvent(transaction.getId()));

    return transaction;
  }

  @Transactional
  public BankTransaction reverseTransaction(Long adminUserId, Long transactionId, String reason) {
    BankTransaction original = bankTransactionRepository.findByIdWithLock(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

    if (original.getStatus() == TransactionStatus.REVERSED
        || original.getStatus() == TransactionStatus.CANCELLED) {
      throw new IllegalStateException("Transaction is already " + original.getStatus());
    }

    original.setStatus(TransactionStatus.REVERSED);
    bankTransactionRepository.save(original);

    BankTransaction reversal = new BankTransaction();
    reversal.setTransactionReference(generateTransactionReference());
    reversal.setType(TransactionType.REVERSAL);
    reversal.setAmount(original.getAmount());
    reversal.setDescription(reason != null && !reason.isBlank() ? reason
        : "Reversal of " + original.getTransactionReference());
    reversal.setCategory(original.getCategory());
    reversal.setStatus(TransactionStatus.COMPLETED);
    reversal.setCreatedAt(OffsetDateTime.now());
    reversal.setCompletedAt(OffsetDateTime.now());
    reversal.setReversedTransaction(original);

    if (original.getSourceAccountHolder() != null) {
      reversal.setSourceAccountHolder(original.getSourceAccountHolder());
      reversal = bankTransactionRepository.save(reversal);
      balanceService.recordLedgerEntry(original.getSourceAccountHolder(), reversal.getId(),
          reversal.getAmount(), BalanceIndicator.CREDIT);
    } else {
      reversal = bankTransactionRepository.save(reversal);
    }

    if (adminUserId != null) {
      auditService.recordAuditLog(adminUserId, AuditAction.TRANSACTION_REVERSED,
          AuditTargetType.TRANSACTION, original.getId().toString(), reason);
    }

    eventPublisher.publishEvent(new TransactionCancelledEvent(original.getId(), reason));

    return reversal;
  }

  private AccountHolder getAndValidateInitiatorAccount(Long userId) {
    AccountHolder myAccount = accountHolderService.getAccountHolderByUserId(userId);

    if (myAccount.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Initiating account must be active");
    }
    return myAccount;
  }

  private void lockAccount(Long accountHolderId) {
    accountHolderService.lockById(accountHolderId);
  }

  private BankTransaction buildPendingTransaction(TransactionType type, BigDecimal amount,
      String description) {
    BankTransaction transaction = new BankTransaction();
    transaction.setTransactionReference(generateTransactionReference());
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setDescription(description);
    transaction.setCreatedAt(OffsetDateTime.now());
    transaction.setStatus(TransactionStatus.PENDING);
    return transaction;
  }

  private BankTransaction buildCompletedTransaction(TransactionType type, BigDecimal amount,
      String description) {
    BankTransaction transaction = new BankTransaction();
    transaction.setTransactionReference(generateTransactionReference());
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setDescription(description);
    transaction.setCreatedAt(OffsetDateTime.now());
    transaction.setStatus(TransactionStatus.COMPLETED);
    transaction.setCompletedAt(OffsetDateTime.now());
    return transaction;
  }

  private String generateTransactionReference() {
    return "TXN-" + java.util.UUID.randomUUID().toString()
        .replace("-", "")
        .substring(0, 12)
        .toUpperCase();
  }

  private void processTransferRules(BankTransaction transaction, AccountHolder sourceAccount,
      String destinationAccountNumber) {
    if (destinationAccountNumber == null || destinationAccountNumber.isBlank()) {
      throw new IllegalArgumentException("Destination account number is required for transfers");
    }
    if (sourceAccount.getAccountNumber().equals(destinationAccountNumber)) {
      throw new IllegalArgumentException("Source and destination accounts must differ");
    }
    AccountHolder destAccount = accountHolderService.findByAccountNumber(
            destinationAccountNumber)
        .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

    if (destAccount.getAccountStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException(
          "Destination account is not active (status: " + destAccount.getAccountStatus() + ")");
    }

    transaction.setSourceAccountHolder(sourceAccount);
    transaction.setDestinationAccountHolder(destAccount);
  }
}
