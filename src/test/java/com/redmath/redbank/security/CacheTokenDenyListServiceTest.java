package com.redmath.redbank.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.redmath.redbank.security.denylist.CacheTokenDenyListService;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheTokenDenyListServiceTest {

  @Mock
  private HazelcastInstance hazelcastInstance;

  @Mock
  private IMap<String, Boolean> denylistedTokens;

  private CacheTokenDenyListService service;

  @BeforeEach
  void setUp() {
    when(hazelcastInstance.<String, Boolean>getMap("token-denylist"))
        .thenReturn(denylistedTokens);
    service = new CacheTokenDenyListService(hazelcastInstance);
  }

  @Test
  void storesJtiWithRequestedTtl() {
    service.deny("jti-1", Duration.ofMinutes(10));

    verify(denylistedTokens).put("jti-1", Boolean.TRUE, 600_000L, TimeUnit.MILLISECONDS);
  }

  @Test
  void checksAndRemovesJti() {
    when(denylistedTokens.containsKey("jti-1")).thenReturn(true);

    assertTrue(service.isDenylisted("jti-1"));
    service.remove("jti-1");

    verify(denylistedTokens).remove("jti-1");
  }

  @Test
  void ignoresInvalidDenyRequests() {
    service.deny(null, Duration.ofMinutes(1));
    service.deny("", Duration.ofMinutes(1));
    service.deny("jti-1", Duration.ZERO);
    service.deny("jti-2", Duration.ofSeconds(-1));

    assertFalse(service.isDenylisted(null));
    verify(denylistedTokens, org.mockito.Mockito.never())
        .put(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any(TimeUnit.class));
  }
}
