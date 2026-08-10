package com.redmath.redbank.account;

import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountHolderService {

  private final AccountHolderRepository accountHolderRepository;
  private final UserService userService;
  private final AuditService auditService;
  private final CacheManager cacheManager;

  public AccountHolderService(AccountHolderRepository accountHolderRepository,
      UserService userService, AuditService auditService, CacheManager cacheManager) {
    this.accountHolderRepository = accountHolderRepository;
    this.userService = userService;
    this.auditService = auditService;
    this.cacheManager = cacheManager;
  }

  public AccountHolder getAccountHolderByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    Optional<AccountHolder> accountHolder = accountHolderRepository.findByUserId(userId);
    if (accountHolder.isEmpty()) {
      throw new ResourceNotFoundException("Account holder not found for user id: " + userId);
    }
    return accountHolder.get();
  }

  public AccountHolder lockById(Long accountHolderId) {
    if (accountHolderId == null) {
      throw new IllegalArgumentException("Account holder id is required");
    }
    return accountHolderRepository.findByIdWithLock(accountHolderId).orElseThrow(
        () -> new ResourceNotFoundException("Account holder not found: " + accountHolderId));
  }


  @Transactional(readOnly = true)
  public String getAccountHolderNameByAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.isBlank()) {
      throw new IllegalArgumentException("Account number is required");
    }
    AccountHolder accountHolder = accountHolderRepository.getAccountHoldersByAccountNumber(
        accountNumber);
    if (accountHolder == null) {
      throw new ResourceNotFoundException(
          "Account holder not found for account number: " + accountNumber);
    }
    return accountHolder.getUser().getName();
  }

  public Optional<AccountHolder> findByUser(User user) {
    return accountHolderRepository.findByUser(user);
  }

  @Cacheable(cacheNames = "account-holder-by-number", key = "#accountNumber", unless = "#result == null")
  public Optional<AccountHolder> findByAccountNumber(String accountNumber) {
    return accountHolderRepository.findByAccountNumber(accountNumber);
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
    return accountHolderRepository.save(accountHolder);
  }

  @Transactional
  public void freezeMyAccountHolder(Long userId) {
    AccountHolder accountHolder = getOrThrowByUserId(userId);
    if (freezeMyAccountHolderInternal(accountHolder)) {
      evictAccountHolderCache(accountHolder);
      auditService.recordAuditLog(userId, AuditAction.ACCOUNT_FROZEN, AuditTargetType.ACCOUNT,
          accountHolder.getId().toString(), null);
    }
  }

  @Transactional
  public void unfreezeMyAccountHolder(Long userId) {
    AccountHolder accountHolder = getOrThrowByUserId(userId);
    if (unfreezeMyAccountHolderInternal(accountHolder)) {
      evictAccountHolderCache(accountHolder);
      auditService.recordAuditLog(userId, AuditAction.ACCOUNT_ACTIVATED, AuditTargetType.ACCOUNT,
          accountHolder.getId().toString(), null);
    }
  }

  @Transactional
  public void deactivateMyAccountHolder(Long userId) {
    AccountHolder accountHolder = getOrThrowByUserId(userId);
    if (deactivateMyAccountHolderInternal(accountHolder)) {
      evictAccountHolderCache(accountHolder);
      auditService.recordAuditLog(userId, AuditAction.ACCOUNT_CLOSED, AuditTargetType.ACCOUNT,
          accountHolder.getId().toString(), null);
    }
  }

  private boolean freezeMyAccountHolderInternal(AccountHolder accountHolder) {
    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be frozen");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.FROZEN) {
      return false;
    }

    accountHolder.setAccountStatus(AccountStatus.FROZEN);
    accountHolderRepository.save(accountHolder);
    return true;
  }

  private boolean unfreezeMyAccountHolderInternal(AccountHolder accountHolder) {
    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be un-frozen by account holder");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.ACTIVE) {
      return false;
    }

    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setUpdatedAt(OffsetDateTime.now());
    accountHolderRepository.save(accountHolder);
    return true;
  }

  private boolean deactivateMyAccountHolderInternal(AccountHolder accountHolder) {
    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      return false;
    }

    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);
    userService.deactivateUser(accountHolder.getUser().getId());
    return true;
  }

  private AccountHolder getOrThrowByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    return accountHolderRepository.findByUserId(userId).orElseThrow(
        () -> new ResourceNotFoundException("Account holder not found for user: " + userId));
  }

  private String generateUniqueAccountNumber() {
    String candidate;
    do {
      candidate =
          "RB" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    } while (accountHolderRepository.existsByAccountNumber(candidate));
    return candidate;
  }

  private void evictAccountHolderCache(AccountHolder accountHolder) {
    Cache cache = cacheManager.getCache("account-holder-by-number");
    if (cache != null) {
      cache.evict(accountHolder.getAccountNumber());
    }
  }
}
