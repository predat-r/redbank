package com.redmath.redbank.account;

import com.redmath.redbank.user.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

  @EntityGraph(attributePaths = {"user"})
  Optional<AccountHolder> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"user"})
  AccountHolder getAccountHoldersById(Long id);

  @EntityGraph(attributePaths = {"user"})
  AccountHolder getAccountHoldersByAccountNumber(String accountNumber);

  @EntityGraph(attributePaths = {"user"})
  Optional<AccountHolder> findByUser(User user);

  @EntityGraph(attributePaths = {"user"})
  Optional<AccountHolder> findByAccountNumber(String accountNumber);

  @EntityGraph(attributePaths = {"user"})
  @Override
  Page<AccountHolder> findAll(Pageable pageable);

  boolean existsByAccountNumber(String accountNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM AccountHolder a WHERE a.id = :id")
  Optional<AccountHolder> findByIdWithLock(Long id);

  @EntityGraph(attributePaths = {"user"})
  java.util.List<AccountHolder> findByUserNameContainingIgnoreCase(String name);

  @Query("""
      SELECT DISTINCT ah FROM AccountHolder ah 
      JOIN BankTransaction t ON (t.sourceAccountHolder = ah OR t.destinationAccountHolder = ah)
      WHERE (t.sourceAccountHolder.id = :myId OR t.destinationAccountHolder.id = :myId)
      AND ah.id != :myId
      AND LOWER(ah.user.name) LIKE LOWER(CONCAT('%', :name, '%'))
      """)
  java.util.List<AccountHolder> findTransactedCounterparties(@Param("myId") Long myId, @Param("name") String name);
}

