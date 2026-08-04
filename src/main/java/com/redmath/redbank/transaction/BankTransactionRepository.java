package com.redmath.redbank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

  @EntityGraph(attributePaths = {"sourceAccountHolder", "destinationAccountHolder"})
  Page<BankTransaction> findBySourceAccountHolderIdOrDestinationAccountHolderId(
      Long sourceId, Long destinationId, Pageable pageable);

  @EntityGraph(attributePaths = {"sourceAccountHolder", "destinationAccountHolder"})
  Page<BankTransaction> findAll(Pageable pageable);
}
