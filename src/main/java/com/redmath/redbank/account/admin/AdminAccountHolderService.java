package com.redmath.redbank.account.admin;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountStatus;
import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.common.exception.ConflictException;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.UserService;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountHolderService {

  private final AccountHolderRepository accountHolderRepository;
  private final AuditService auditService;
  private final UserService userService;
  private final com.redmath.redbank.account.AccountHolderService accountHolderService;

  public AdminAccountHolderService(AccountHolderRepository accountHolderRepository,
      AuditService auditService, UserService userService,
      com.redmath.redbank.account.AccountHolderService accountHolderService) {
    this.accountHolderRepository = accountHolderRepository;
    this.auditService = auditService;
    this.userService = userService;
    this.accountHolderService = accountHolderService;
  }

  public AccountHolder getAccountHolderById(Long accountId) {
    return getOrThrow(accountId);
  }

  @Transactional(readOnly = true)
  public Page<AccountHolder> getAllAccountHolders(Pageable pageable) {
    return accountHolderRepository.findAll(pageable);
  }

  @Transactional
  public void freezeAccountHolder(Long accountId, Long adminUserId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      throw new ConflictException("Closed accounts cannot be frozen");
    }
    if (accountHolder.getAccountStatus() == AccountStatus.FROZEN) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.FROZEN);
    accountHolderRepository.save(accountHolder);
    accountHolderService.evictAccountHolderCache(accountHolder);
    auditService.recordAuditLog(adminUserId, AuditAction.ACCOUNT_FROZEN, AuditTargetType.ACCOUNT,
        accountId.toString(), null);
  }

  @Transactional
  public void deactivateAccountHolder(Long accountId, Long adminUserId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.CLOSED) {
      return;
    }

    accountHolder.setAccountStatus(AccountStatus.CLOSED);
    accountHolderRepository.save(accountHolder);
    accountHolderService.evictAccountHolderCache(accountHolder);
    userService.deactivateUser(accountHolder.getUser().getId());
    auditService.recordAuditLog(adminUserId, AuditAction.ACCOUNT_CLOSED, AuditTargetType.ACCOUNT,
        accountId.toString(), null);
  }

  @Transactional
  public void unfreezeAccountHolder(Long accountId, Long adminUserId) {
    AccountHolder accountHolder = getOrThrow(accountId);

    if (accountHolder.getAccountStatus() == AccountStatus.ACTIVE) {
      return;
    }

    boolean wasClosed = accountHolder.getAccountStatus() == AccountStatus.CLOSED;

    accountHolder.setAccountStatus(AccountStatus.ACTIVE);
    accountHolder.setUpdatedAt(OffsetDateTime.now());
    accountHolderRepository.save(accountHolder);
    accountHolderService.evictAccountHolderCache(accountHolder);

    if (wasClosed) {
      userService.reactivateUser(accountHolder.getUser().getId());
    }

    auditService.recordAuditLog(adminUserId, AuditAction.ACCOUNT_ACTIVATED,
        AuditTargetType.ACCOUNT, accountId.toString(), null);
  }

  @Transactional
  public void reactivateAccountHolder(Long accountId, Long adminUserId) {
    unfreezeAccountHolder(accountId, adminUserId);
  }

  private AccountHolder getOrThrow(Long accountId) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account id is required");
    }
    return accountHolderRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account holder not found: " + accountId));
  }
}
