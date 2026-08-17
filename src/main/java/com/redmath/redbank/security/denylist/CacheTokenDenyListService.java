package com.redmath.redbank.security.denylist;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CacheTokenDenyListService implements TokenDenylistService {

  private static final String TOKEN_DENYLIST_MAP = "token-denylist";

  private final IMap<String, Boolean> denylistedTokens;

  public CacheTokenDenyListService(HazelcastInstance hazelcastInstance) {
    this.denylistedTokens = hazelcastInstance.getMap(TOKEN_DENYLIST_MAP);
  }

  @Override
  public void deny(String jti, Duration duration) {
    if (jti == null || jti.isBlank() || duration == null || duration.isZero()
        || duration.isNegative()) {
      return;
    }

    denylistedTokens.put(jti, Boolean.TRUE, duration.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean isDenylisted(String jti) {
    return jti != null && !jti.isBlank() && denylistedTokens.containsKey(jti);
  }

  @Override
  public void remove(String jti) {
    if (jti != null && !jti.isBlank()) {
      denylistedTokens.remove(jti);
    }
  }
}
