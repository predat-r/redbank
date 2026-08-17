package com.redmath.redbank.locationrisk.assessment;

public record LocationRiskAssessment(
    RiskLevel riskLevel,
    String reason,
    AssessmentConfidence confidence,
    RecommendedAction recommendedAction
) {

}
