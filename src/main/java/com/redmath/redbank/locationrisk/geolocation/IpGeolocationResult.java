package com.redmath.redbank.locationrisk.geolocation;

public record IpGeolocationResult(
    String ipAddress,
    String city,
    String country,
    String countryCode,
    String provider,
    boolean successful
) {

}