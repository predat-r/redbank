package com.redmath.redbank.locationrisk.ai;

public record LocationRiskAssessment(
    String riskLevel,
    String reason,
    String confidence,
    String recommendedAction
) {

}