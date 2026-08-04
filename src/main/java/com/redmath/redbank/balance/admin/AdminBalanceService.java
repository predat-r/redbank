package com.redmath.redbank.balance.admin;

import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBalanceService {
  private final BalanceRepository balanceRepository;

  public AdminBalanceService(BalanceRepository balanceRepository) {
    this.balanceRepository = balanceRepository;
  }

  public Balance getLatestBalanceByAccountHolderId(Long accountId) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account holder id is required");
    }
    return balanceRepository.getLatestBalanceByAccountHolderId(accountId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Balance not found for account holder id: " + accountId));
  }

  @Transactional(readOnly = true)
  public Page<Balance> getBalanceLedgerByAccountHolderId(Long accountId, Pageable pageable) {
    if (accountId == null) {
      throw new IllegalArgumentException("Account holder id is required");
    }
    if (pageable.getPageNumber() < 0) {
      throw new IllegalArgumentException("Page index cannot be negative");
    }
    if (pageable.getPageSize() <= 0) {
      throw new IllegalArgumentException("Page size must be positive");
    }
    return balanceRepository.findAllByAccountHolderId(accountId, pageable);
  }
}
