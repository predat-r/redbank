package com.redmath.redbank.ai.anomaly;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomalyReportRepository extends JpaRepository<AnomalyReport, Long> {

  @EntityGraph(attributePaths = {"transaction"})
  Optional<AnomalyReport> findByTransactionId(Long transactionId);
}
