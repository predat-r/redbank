package com.redmath.redbank.locationrisk.login;

public record LoginContext(
    String ipAddress,
    String userAgent,
    String deviceIdentifier
) {

}