package com.redmath.redbank.account.admin;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.admin.AccountHolderCreator;
import com.redmath.redbank.user.admin.dto.AccountHolderSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountHolderCreatorImpl implements AccountHolderCreator {

  private final AccountHolderService accountHolderService;
  private final AdminAccountHolderService adminAccountHolderService;
  private final AccountHolderRepository accountHolderRepository;
  private final AuditService auditService;

  @Override
  public AccountHolderSummaryDto createAccountHolder(User user, Long adminUserId) {
    AccountHolder accountHolder = accountHolderService.createAccountHolder(user);
    if (adminUserId != null) {
      auditService.recordAuditLog(adminUserId, AuditAction.ACCOUNT_CREATED, AuditTargetType.ACCOUNT,
          accountHolder.getId().toString(), null);
    }
    return new AccountHolderSummaryDto(
        accountHolder.getId(),
        accountHolder.getAccountNumber(),
        accountHolder.getCurrency(),
        accountHolder.getAccountStatus() != null ? accountHolder.getAccountStatus().name() : null,
        accountHolder.getApprovedAt(),
        accountHolder.getCreatedAt(),
        accountHolder.getUpdatedAt()
    );
  }

  @Override
  public void deactivateAccountHolder(Long userId, Long adminUserId) {
    accountHolderRepository.findByUserId(userId)
        .ifPresent(accountHolder -> adminAccountHolderService.deactivateAccountHolder(
            accountHolder.getId(), adminUserId));
  }

  @Override
  public void reactivateAccountHolder(Long userId, Long adminUserId) {
    accountHolderRepository.findByUserId(userId)
        .ifPresent(accountHolder -> adminAccountHolderService.reactivateAccountHolder(
            accountHolder.getId(), adminUserId));
  }
}
