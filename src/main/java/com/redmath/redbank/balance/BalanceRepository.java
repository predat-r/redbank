package com.redmath.redbank.balance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {

  @Query("""
          SELECT b
          FROM Balance b
          WHERE b.accountHolder.user.id = :userId
          ORDER BY b.id DESC
          LIMIT 1
      """)
  Balance getLatestBalanceByUserId(Long userId);

  @Query(
      """
          SELECT b
          FROM Balance b
          WHERE b.accountHolder.id = :accountId
          ORDER BY b.id DESC
          LIMIT 1
          """
  )
  Balance getLatestBalanceByAccountHolderId(Long accountId);

  Balance getBalanceByTransactionId(Long transactionId);
}