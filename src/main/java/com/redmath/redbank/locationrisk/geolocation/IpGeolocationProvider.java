package com.redmath.redbank.locationrisk.geolocation;

public interface IpGeolocationProvider {

  IpGeolocationResult resolve(String ipAddress);
}