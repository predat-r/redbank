package com.redmath.redbank.account_holder;

import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountHolderService {

  private final AccountHolderRepository accountHolderRepository;
  private final UserService userService;

  public AccountHolderService(AccountHolderRepository accountHolderRepository, UserService userService) {
    this.accountHolderRepository = accountHolderRepository;
    this.userService = userService;
  }

  public AccountHolder getAccountHolderByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    Optional<AccountHolder> accountHolder = accountHolderRepository.findByUserId(userId);
    if(accountHolder.isEmpty()){
      throw new ResourceNotFoundException("Account holder not found for user id: " + userId);
    }
    return accountHolder.get();
  }

  @Transactional(readOnly = true)
  public String getAccountHolderNameByAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.isBlank()) {
      throw new IllegalArgumentException("Account number is required");
    }
    AccountHolder accountHolder = accountHolderRepository.getAccountHoldersByAccountNumber(accountNumber);
    if (accountHolder == null) {
      throw new ResourceNotFoundException("Account holder not found for account number: " + accountNumber);
    }
    return accountHolder.getUser().getName();
  }

  public Optional<AccountHolder> findByUser(User user) {
    return accountHolderRepository.findByUser(user);
  }

  public Optional<AccountHolder> findByAccountNumber(String accountNumber) {
    return accountHolderRepository.findByAccountNumber(accountNumber);
  }

  @Transactional
  public void createAccountHolder(User user) {
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
  }

  @Transactional
  public void freezeMyAccountHolder(Long userId) {
    AccountHolder accountHolder = getOrThrowByUserId(userId);
    freezeMyAccountHolderInternal(accountHolder);
  }

  @Transactional
  public void deactivateMyAccountHolder(Long userId) {
    AccountHolder accountHolder = getOrThrowByUserId(userId);
    deactivateMyAccountHolderInternal(accountHolder);
  }

  private void freezeMyAccountHolderInternal(AccountHolder accountHolder) {
    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be frozen");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.FROZEN) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.FROZEN);
    accountHolderRepository.save(accountHolder);
  }

  private void deactivateMyAccountHolderInternal(AccountHolder accountHolder) {
    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);
    userService.deactivateUser(accountHolder.getUser().getId());
  }

  private AccountHolder getOrThrowByUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("User id is required");
    }
    return accountHolderRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Account holder not found for user: " + userId));
  }

  private String generateUniqueAccountNumber() {
    String candidate;
    do {
      candidate =
          "RB" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    } while (accountHolderRepository.existsByAccountNumber(candidate));
    return candidate;
  }
}