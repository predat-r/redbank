package com.redmath.redbank.locationrisk.geolocation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class IpGeolocationIoProvider implements IpGeolocationProvider {

  private final RestClient restClient;
  private final String apiKey;

  public IpGeolocationIoProvider(
      @Value("${ip-geolocation.base-url}") String baseUrl,
      @Value("${ip-geolocation.api-key}") String apiKey
  ) {
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
    this.apiKey = apiKey;
  }

  @Override
  public IpGeolocationResult resolve(String ipAddress) {
    if (ipAddress == null || ipAddress.isBlank() || isPrivateIp(ipAddress)
        || apiKey.isBlank()) {
      return unsuccessfulResult(ipAddress);
    }

    try {
      IpGeolocationResponse response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/v3/ipgeo")
              .queryParam("apiKey", apiKey)
              .queryParam("ip", ipAddress)
              .build())
          .retrieve()
          .body(IpGeolocationResponse.class);

      if (response == null
          || response.location() == null
          || response.location().city() == null
          || response.location().countryName() == null) {
        return unsuccessfulResult(ipAddress);
      }

      return new IpGeolocationResult(
          response.ip(),
          response.location().city(),
          response.location().countryName(),
          response.location().countryCode(),
          "ipgeolocation.io",
          true
      );
    } catch (RestClientException exception) {
      return unsuccessfulResult(ipAddress);
    }
  }

  private IpGeolocationResult unsuccessfulResult(String ipAddress) {
    return new IpGeolocationResult(
        ipAddress,
        null,
        null,
        null,
        "ipgeolocation.io",
        false
    );
  }

  private boolean isPrivateIp(String ipAddress) {
    try {
      InetAddress address = InetAddress.getByName(ipAddress);

      return address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress();
    } catch (UnknownHostException exception) {
      return true;
    }
  }
}