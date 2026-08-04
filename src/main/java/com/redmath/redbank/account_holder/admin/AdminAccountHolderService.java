package com.redmath.redbank.account_holder.admin;

import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderRepository;
import com.redmath.redbank.account_holder.AccountStatus;
import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.UserService;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountHolderService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "accountNumber",
      "accountStatus", "createdAt", "updatedAt");
  private final AccountHolderRepository accountHolderRepository;
  private final UserService userService;

  public AdminAccountHolderService(AccountHolderRepository accountHolderRepository,
      UserService userService) {
    this.accountHolderRepository = accountHolderRepository;
    this.userService = userService;
  }

  public AccountHolder getAccountHolderById(Long accountId) {
    return getOrThrow(accountId);
  }

  @Transactional(readOnly = true)
  public Page<AccountHolder> getAllAccountHolders(Pageable pageable) {
    return accountHolderRepository.findAll(pageable);
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
    userService.deactivateUser(accountHolder.getUser().getId());
  }

  private AccountHolder getOrThrow(Long accountId) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account id is required");
    }
    return accountHolderRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account holder not found: " + accountId));
  }
}
