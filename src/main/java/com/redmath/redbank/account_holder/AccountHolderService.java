package com.redmath.redbank.account_holder;

import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountHolderService {

  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("id", "accountNumber", "accountStatus", "createdAt", "updatedAt");
  private final AccountHolderRepository accountHolderRepository;
  public AccountHolderService(AccountHolderRepository accountHolderRepository) {
    this.accountHolderRepository = accountHolderRepository;
  }

  public AccountHolder getAccountHolderByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    return accountHolderRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Account holder not found for user id: " + userId));
  }

  public AccountHolder getAccountHolderById(Long accountId) {
    return getOrThrow(accountId);
  }

  public AccountHolder getAccountHolderByAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.isBlank()) {
      throw new IllegalArgumentException("Account number is required");
    }
    return accountHolderRepository.findByAccountNumber(accountNumber)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Account holder not found for account number: " + accountNumber));
  }

  @Transactional(readOnly = true)
  public Page<AccountHolder> getAllAccountHolders(int page, int size, String sortBy,
      String sortDir) {
    if (page < 0) {
      throw new IllegalArgumentException("Page index cannot be negative");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("Page size must be positive");
    }
    String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
    Sort.Direction direction =
        "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));
    return accountHolderRepository.findAll(pageable);
  }

  @Transactional
  public AccountHolder createAccountHolder(User user) {
    if (user == null) {
      throw new IllegalArgumentException("User is required");
    }
    if (user.getId() == null) {
      throw new IllegalArgumentException("User id is required");
    }

    if (accountHolderRepository.findByUserId(user.getId()).isPresent()) {
      throw new ConflictException("User already has an account holder record");
    }

    OffsetDateTime now = OffsetDateTime.now();
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(user);
    accountHolder.setAccountNumber(generateUniqueAccountNumber());
    accountHolder.setCurrency("USD");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(now);
    accountHolder.setCreatedAt(now);
    accountHolder.setUpdatedAt(now);
    accountHolderRepository.save(accountHolder);
    return accountHolder;
  }

  @Transactional
  public void freezeMyAccountHolder(Long userId){
    AccountHolder accountHolder = getAccountHolderByUserId(userId);
    freezeAccountHolder(accountHolder.getId());
  }

  @Transactional
  public void deactivateMyAccountHolder(Long userId){
    AccountHolder accountHolder = getAccountHolderByUserId(userId);
    deactivateAccountHolder(accountHolder.getId());
  }


  @Transactional
  public void freezeAccountHolder(Long accountId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be frozen");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.FROZEN) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.FROZEN);
    accountHolderRepository.save(accountHolder);
  }

  @Transactional
  public void deactivateAccountHolder(Long accountId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);
  }

  private AccountHolder getOrThrow(Long accountId) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account id is required");
    }
    return accountHolderRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account holder not found: " + accountId));
  }

  private String generateUniqueAccountNumber() {
    String candidate;
    do {
      candidate = "RB" + UUID.randomUUID().toString().replace("-", "")
          .substring(0, 10).toUpperCase();
    } while (accountHolderRepository.existsByAccountNumber(candidate));
    return candidate;
  }
}