package com.redmath.redbank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Page<BankTransaction> findBySourceAccountHolderIdOrDestinationAccountHolderId(
            Long sourceId, Long destinationId, Pageable pageable);
}
