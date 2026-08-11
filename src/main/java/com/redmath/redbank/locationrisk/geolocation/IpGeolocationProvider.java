package com.redmath.redbank.locationrisk.geolocation;

@FunctionalInterface
public interface IpGeolocationProvider {

  IpGeolocationResult resolve(String ipAddress);
}
