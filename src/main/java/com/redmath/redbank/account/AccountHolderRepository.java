package com.redmath.redbank.account;

import com.redmath.redbank.user.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

  Optional<AccountHolder> findByUserId(Long userId);

  AccountHolder getAccountHoldersById(Long id);

  AccountHolder getAccountHoldersByAccountNumber(String accountNumber);

  Optional<AccountHolder> findByUser(User user);

  Optional<AccountHolder> findByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM AccountHolder a WHERE a.id = :id")
  Optional<AccountHolder> findByIdWithLock(Long id);
}

