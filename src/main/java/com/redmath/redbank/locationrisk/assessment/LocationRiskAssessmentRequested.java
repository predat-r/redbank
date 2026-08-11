package com.redmath.redbank.locationrisk.assessment;

import java.time.Instant;

public record LocationRiskAssessmentRequested(Long loginEventId, Long userId, String ipAddress,
                                              String accessTokenJti, boolean loginSuccessful,
                                              Instant expiresAt) {

}
