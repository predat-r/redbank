package com.redmath.redbank.locationrisk.trigger;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;

public record LocationRiskTriggerResult(
    IpGeolocationResult currentLocation,
    boolean ipPreviouslyUsed,
    boolean requiresAiAssessment
) {

}
