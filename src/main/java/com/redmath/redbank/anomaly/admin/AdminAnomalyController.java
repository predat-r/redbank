package com.redmath.redbank.anomaly.admin;

import com.redmath.redbank.anomaly.AnomalyAnalysisService;
import com.redmath.redbank.anomaly.AnomalyReport;
import com.redmath.redbank.anomaly.AnomalyReportDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAnomalyController {

  private final AnomalyAnalysisService anomalyAnalysisService;

  public AdminAnomalyController(AnomalyAnalysisService anomalyAnalysisService) {
    this.anomalyAnalysisService = anomalyAnalysisService;
  }

  @GetMapping("/transactions/{id}/anomaly-report")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AnomalyReportDto> getAnomalyReport(@PathVariable Long id) {
    AnomalyReport report = anomalyAnalysisService.getAnomalyReportByTransactionId(id);
    return ResponseEntity.ok(AnomalyReportDto.from(report));
  }
}
