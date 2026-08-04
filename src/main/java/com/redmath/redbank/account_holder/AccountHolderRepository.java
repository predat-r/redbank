package com.redmath.redbank.account_holder;

import com.redmath.redbank.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

  Optional<AccountHolder> findByUserId(Long userId);

  Optional<AccountHolder> findByUser(User user);

  Optional<AccountHolder> findByAccountNumber(String accountNumber);

  AccountHolder getAccountHoldersById(Long id);

  AccountHolder getAccountHoldersByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);

}
