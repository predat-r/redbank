package com.redmath.redbank.locationrisk.geolocation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IpGeolocationIoProviderTest {

  @Test
  void rejectsBlankIpWithoutCallingProvider() {
    IpGeolocationIoProvider provider =
        new IpGeolocationIoProvider("https://example.test", "api-key");

    IpGeolocationResult result = provider.resolve(" ");

    assertFalse(result.successful());
    assertEquals(" ", result.ipAddress());
  }

  @Test
  void rejectsPrivateIp() {
    IpGeolocationIoProvider provider =
        new IpGeolocationIoProvider("https://example.test", "api-key");

    IpGeolocationResult result = provider.resolve("127.0.0.1");

    assertFalse(result.successful());
  }

  @Test
  void rejectsPublicIpWhenApiKeyIsMissing() {
    IpGeolocationIoProvider provider =
        new IpGeolocationIoProvider("https://example.test", "");

    IpGeolocationResult result = provider.resolve("198.51.100.10");

    assertFalse(result.successful());
  }
}
