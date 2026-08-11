package com.redmath.redbank.locationrisk.assessment;

public record LocationRiskAssessmentRequested(Long loginEventId, Long userId, String ipAddress,
                                              String accessTokenJti, boolean loginSuccessful) {

}
