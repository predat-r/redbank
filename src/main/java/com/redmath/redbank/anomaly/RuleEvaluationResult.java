package com.redmath.redbank.anomaly;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuleEvaluationResult {

  private boolean flagged;
  private AnomalyFlag anomalyFlag = AnomalyFlag.NONE;
  private int riskScore = 0;
  private List<String> reasons = new ArrayList<>();

  public void addReason(String reason, int scoreBonus) {
    this.flagged = true;
    this.reasons.add(reason);
    this.riskScore += scoreBonus;
  }
}
