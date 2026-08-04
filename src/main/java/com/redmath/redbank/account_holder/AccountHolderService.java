package com.redmath.redbank.account_holder;

import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import java.time.OffsetDateTime;
import java.util.Optional;
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
    return accountHolderRepository.findByUserId(userId);
  }

  public AccountHolder getAccountHolderById(Long accountId){
    return  accountHolderRepository.getAccountHoldersById(accountId);
  }

  public AccountHolder getAccountHolderByAccountNumber(String accountNumber){
    return accountHolderRepository.getAccountHoldersByAccountNumber(accountNumber);
  }

  public Optional<AccountHolder> findByUser(User user) {
    return accountHolderRepository.findByUser(user);
  }

  public Optional<AccountHolder> findByAccountNumber(String accountNumber) {
    return accountHolderRepository.findByAccountNumber(accountNumber);
  }

  @Transactional(readOnly = true)
  public Page<AccountHolder> getAllAccountHolders(int page, int size, String sortBy, String sortDir) {
    if (page < 0) {
      throw new IllegalArgumentException("Page index cannot be negative");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("Page size must be positive");
    }
    String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
    Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, safeSortBy));
    return accountHolderRepository.findAll(pageable);
  }

  @Transactional
  public AccountHolder createAccountHolder(Long userId) {
    //TODO: User user = userService.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

    if(accountHolderRepository.findByUserId(userId) != null) {
      //TODO: throw new ConflictException("User already has an account holder record");
    }

    AccountHolder accountHolder = new AccountHolder();
    //TODO: accountHolder.setUser(user);
    accountHolder.setAccountNumber(generateUniqueAccountNumber());
    accountHolder.setCurrency("PKR");
    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setApprovedAt(OffsetDateTime.now());
    AccountHolder saved = accountHolderRepository.save(accountHolder);

    //TODO: call transaction service
    return saved;
  }


  @Transactional
  public void freezeAccountHolder(Long accountId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be frozen");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.FROZEN) {
      return; // idempotent
    }

    accountHolder.setAccountStatus(AccountStatus.FROZEN);
    accountHolderRepository.save(accountHolder);
  }

  @Transactional
  public void deactivateAccountHolder(Long accountId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      return; // idempotent
    }

    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);
  }

  private AccountHolder getOrThrow(Long accountId) {
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