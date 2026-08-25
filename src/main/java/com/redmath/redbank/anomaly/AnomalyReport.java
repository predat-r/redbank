package com.redmath.redbank.anomaly;

import com.redmath.redbank.transaction.BankTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "anomaly_reports")
@Getter
@Setter
@NoArgsConstructor
public class AnomalyReport {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "anomaly_reports_seq")
  @SequenceGenerator(name = "anomaly_reports_seq", sequenceName = "anomaly_reports_id_seq", allocationSize = 50)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id", nullable = false, unique = true)
  private BankTransaction transaction;

  @Column(name = "risk_score", nullable = false)
  private Integer riskScore;

  @Column(name = "recommendation", nullable = false, length = 50)
  private String recommendation;

  @Column(name = "reasoning", length = 2000)
  private String reasoning;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
