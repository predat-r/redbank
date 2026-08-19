package com.redmath.redbank.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import com.redmath.redbank.config.serialization.AccountHolderCompactSerializer;
import com.redmath.redbank.config.serialization.RoleCompactSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class HazelcastConfig {

  private static final int ACCOUNT_HOLDER_CACHE_TTL_SECONDS = 900;
  private static final int ACCOUNT_HOLDER_CACHE_MAX_SIZE = 500;

  private static final int ROLE_CACHE_TTL_SECONDS = 900;
  private static final int ROLE_CACHE_MAX_SIZE = 50;

  private static final int IDEMPOTENCY_KEY_TTL_SECONDS = 3600;
  private static final int IDEMPOTENCY_KEY_MAX_SIZE = 5000;

  @Bean
  @Primary
  public HazelcastInstance hazelcastInstance() {
    Config config = new Config();
    config.setClusterName("redbank");

    config.addMapConfig(
        new MapConfig("account-holder-by-number")
            .setTimeToLiveSeconds(ACCOUNT_HOLDER_CACHE_TTL_SECONDS)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(ACCOUNT_HOLDER_CACHE_MAX_SIZE)));

    config.addMapConfig(
        new MapConfig("role-by-name")
            .setTimeToLiveSeconds(ROLE_CACHE_TTL_SECONDS)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(ROLE_CACHE_MAX_SIZE)));

    config.addMapConfig(
        new MapConfig("idempotency-keys")
            .setTimeToLiveSeconds(IDEMPOTENCY_KEY_TTL_SECONDS)
            .setEvictionConfig(new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                .setSize(IDEMPOTENCY_KEY_MAX_SIZE)));

    config.getSerializationConfig()
        .getCompactSerializationConfig()
        .addSerializer(new AccountHolderCompactSerializer())
        .addSerializer(new RoleCompactSerializer());

    return Hazelcast.newHazelcastInstance(config);
  }

  @Bean
  public CacheManager cacheManager(HazelcastInstance instance) {
    return new HazelcastCacheManager(instance);
  }
}
