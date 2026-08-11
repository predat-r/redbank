package com.redmath.redbank.locationrisk.geolocation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IpGeolocationResponse(
    String ip,
    Location location
) {

  public record Location(
      String city,
      @JsonProperty("country_name")
      String countryName,
      @JsonProperty("country_code2")
      String countryCode
  ) {

  }
}