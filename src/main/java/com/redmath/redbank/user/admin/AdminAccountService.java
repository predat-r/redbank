package com.redmath.redbank.user.admin;

import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderService;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.dto.AdminAccountResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

  private final AccountHolderService accountHolderService;

  @Transactional(readOnly = true)
  public Page<AdminAccountResponse> findAccounts(Pageable pageable) {
    Sort.Order order = pageable.getSort().stream().findFirst().orElse(Sort.Order.asc("createdAt"));

    Page<AccountHolder> accounts = accountHolderService.getAllAccountHolders(
        pageable.getPageNumber(), pageable.getPageSize(), order.getProperty(),
        order.getDirection().name());

    return accounts.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public AdminAccountResponse findAccount(String accountNumber) {
    return toResponse(findAccountOrThrow(accountNumber));
  }

  @Transactional
  public void freezeAccount(String accountNumber) {
    AccountHolder account = findAccountOrThrow(accountNumber);
    accountHolderService.freezeAccountHolder(account.getId());
  }

  @Transactional
  public void closeAccount(String accountNumber) {
    AccountHolder account = findAccountOrThrow(accountNumber);
    accountHolderService.deactivateAccountHolder(account.getId());
  }

  private AdminAccountResponse toResponse(AccountHolder account) {
    User user = account.getUser();

    return new AdminAccountResponse(account.getId(), account.getAccountNumber(),
        account.getCurrency(), account.getAccountStatus(), user.getId(), user.getName(),
        user.getEmail(), account.getApprovedAt(), account.getCreatedAt(), account.getUpdatedAt());
  }

  private AccountHolder findAccountOrThrow(String accountNumber) {
    return Optional.ofNullable(
            accountHolderService.getAccountHolderByAccountNumber(accountNumber)
        )
        .orElseThrow(() ->
            new ResourceNotFoundException("Account not found: " + accountNumber)
        );
  }
}
