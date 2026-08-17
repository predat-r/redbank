package com.redmath.redbank.locationrisk.assessment;

public record LocationRiskAssessment(
    String riskLevel,
    String reason,
    String confidence,
    String recommendedAction
) {

}