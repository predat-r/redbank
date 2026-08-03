package com.redmath.redbank.transaction;

import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderRepository;
import com.redmath.redbank.account_holder.AccountStatus;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.request.DepositRequest;
import com.redmath.redbank.transaction.request.TransferRequest;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankTransactionService {

    private static final int MAX_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final BankTransactionRepository bankTransactionRepository;
    private final UserRepository userRepository;
    private final AccountHolderRepository accountHolderRepository;

    public BankTransactionService(BankTransactionRepository bankTransactionRepository,
                                  UserRepository userRepository,
                                  AccountHolderRepository accountHolderRepository) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.userRepository = userRepository;
        this.accountHolderRepository = accountHolderRepository;
    }

    public Page<BankTransaction> getTransactionsForUser(String email, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AccountHolder accountHolder = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Account holder not found"));
        return bankTransactionRepository.findBySourceAccountHolderIdOrDestinationAccountHolderId(
                accountHolder.getId(), accountHolder.getId(), PageRequest.of(safePage, safeSize));
    }

    @Transactional
    public BankTransaction transfer(String email, TransferRequest request) {
        AccountHolder myAccount = getAndValidateInitiatorAccount(email);
        BankTransaction transaction = buildBaseTransaction(TransactionType.TRANSFER, request.getAmount(), request.getDescription());
        processTransferRules(transaction, myAccount, request.getDestinationAccountNumber());
        return bankTransactionRepository.save(transaction);
    }

    @Transactional
    public BankTransaction deposit(DepositRequest request) {
        AccountHolder targetAccount = accountHolderRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (targetAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new IllegalArgumentException("Destination account is closed");
        }

        BankTransaction transaction = buildBaseTransaction(TransactionType.DEPOSIT, request.getAmount(), request.getDescription());
        transaction.setDestinationAccountHolder(targetAccount);
        return bankTransactionRepository.save(transaction);
    }

    private AccountHolder getAndValidateInitiatorAccount(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        AccountHolder myAccount = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Account holder not found"));

        if (myAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Initiating account must be active");
        }
        return myAccount;
    }

    private BankTransaction buildBaseTransaction(TransactionType type, BigDecimal amount, String description) {
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

    private void processTransferRules(BankTransaction transaction, AccountHolder sourceAccount, String destinationAccountNumber) {
        if (destinationAccountNumber == null || destinationAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Destination account number is required for transfers");
        }
        if (sourceAccount.getAccountNumber().equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }
        AccountHolder destAccount = accountHolderRepository.findByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        if (destAccount.getAccountStatus() == AccountStatus.CLOSED) {
            throw new IllegalArgumentException("Destination account is closed");
        }

        transaction.setSourceAccountHolder(sourceAccount);
        transaction.setDestinationAccountHolder(destAccount);
    }
}
