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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

  public BankTransactionService(
      BankTransactionRepository bankTransactionRepository,
      AccountHolderService accountHolderService,
      AuditService auditService,
      BalanceService balanceService) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.accountHolderService = accountHolderService;
    this.auditService = auditService;
    this.balanceService = balanceService;
  }

  public Page<BankTransaction> getTransactionsForUser(Long userId, Pageable pageable) {
    AccountHolder accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    return bankTransactionRepository.findBySourceAccountHolderIdOrDestinationAccountHolderId(
        accountHolder.getId(), accountHolder.getId(), pageable);
  }

  public Page<BankTransaction> getAllTransactions(Pageable pageable) {
    return bankTransactionRepository.findAll(pageable);
  }

  public Page<BankTransaction> getAllTransactions(
      String reference,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      OffsetDateTime fromDate,
      OffsetDateTime toDate,
      Pageable pageable) {
    return bankTransactionRepository.findAll(
        BankTransactionSpecification.filter(reference, accountNumber, type, status, fromDate, toDate),
        pageable);
  }

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

  public BankTransaction getTransactionByReference(String reference) {
    return bankTransactionRepository.findByTransactionReference(reference)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Transaction not found with reference: " + reference));
  }

  @Transactional
  public BankTransaction transfer(Long userId, TransferRequest request) {
    AccountHolder myAccount = getAndValidateInitiatorAccount(userId);
    lockAccount(myAccount.getId());
    BankTransaction transaction = buildBaseTransaction(TransactionType.TRANSFER,
        request.getAmount(), request.getDescription());
    processTransferRules(transaction, myAccount, request.getDestinationAccountNumber());

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(transaction.getSourceAccountHolder(), transaction,
        BalanceIndicator.DEBIT);
    balanceService.recordLedgerEntry(transaction.getDestinationAccountHolder(), transaction,
        BalanceIndicator.CREDIT);

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

    BankTransaction transaction = buildBaseTransaction(TransactionType.DEPOSIT, request.getAmount(),
        request.getDescription());
    transaction.setDestinationAccountHolder(targetAccount);

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(targetAccount, transaction, BalanceIndicator.CREDIT);
    auditService.recordAuditLog(adminUserId, AuditAction.ADMIN_DEPOSIT_RECORDED,
        AuditTargetType.TRANSACTION, transaction.getId().toString(), null);

    return transaction;
  }

  @Transactional
  public BankTransaction withdraw(Long userId, WithdrawalRequest request) {
    AccountHolder sourceAccount = getAndValidateInitiatorAccount(userId);
    lockAccount(sourceAccount.getId());

    BankTransaction transaction = buildBaseTransaction(TransactionType.WITHDRAWAL,
        request.getAmount(), request.getDescription());
    transaction.setSourceAccountHolder(sourceAccount);

    transaction = bankTransactionRepository.save(transaction);

    balanceService.recordLedgerEntry(sourceAccount, transaction, BalanceIndicator.DEBIT);

    return transaction;
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

  private BankTransaction buildBaseTransaction(TransactionType type, BigDecimal amount,
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
