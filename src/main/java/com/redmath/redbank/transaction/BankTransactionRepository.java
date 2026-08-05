package com.redmath.redbank.transaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

  @EntityGraph(attributePaths = {"sourceAccountHolder.user", "destinationAccountHolder.user"})
  Optional<BankTransaction> findById(Long id);

  @EntityGraph(attributePaths = {"sourceAccountHolder.user", "destinationAccountHolder.user"})
  Optional<BankTransaction> findByTransactionReference(String transactionReference);

  List<BankTransaction> findAllByStatusAndCreatedAtBefore(TransactionStatus status,
      OffsetDateTime cutoff);
}
