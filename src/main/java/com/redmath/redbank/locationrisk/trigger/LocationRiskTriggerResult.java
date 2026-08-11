package com.redmath.redbank.locationrisk.trigger;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.login.LoginEvent;

public record LocationRiskTriggerResult(
    IpGeolocationResult currentLocation,
    LoginEvent previousSuccessfulLogin,
    boolean ipPreviouslyUsed,
    boolean requiresAiAssessment
) {

}