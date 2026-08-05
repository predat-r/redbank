package com.redmath.redbank.account;

import com.redmath.redbank.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

  Optional<AccountHolder> findByUserId(Long userId);
  
  AccountHolder getAccountHoldersById(Long id);

  AccountHolder getAccountHoldersByAccountNumber(String accountNumber);

  Optional<AccountHolder> findByUser(User user);

  Optional<AccountHolder> findByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);

}
