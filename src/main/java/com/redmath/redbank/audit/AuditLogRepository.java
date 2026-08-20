package com.redmath.redbank.audit;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  @Override
  Page<AuditLog> findAll(Pageable pageable);

  @Override
  Optional<AuditLog> findById(Long auditLogId);
}
