package com.redmath.redbank.account_holder;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

  AccountHolder findByUserId(Long userId);

  AccountHolder getAccountHoldersById(Long id);

  AccountHolder getAccountHoldersByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);
}
