package com.redmath.redbank.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import com.redmath.redbank.config.serialization.AccountHolderCompactSerializer;
import com.redmath.redbank.config.serialization.RoleCompactSerializer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class HazelcastConfig {

  @Bean
  public HazelcastInstance hazelcastInstance() {
    Config config = new Config();
    config.setClusterName("redbank-dev");

    config.addMapConfig(
        new MapConfig("account-holder-by-number")
            .setTimeToLiveSeconds(900));

    config.addMapConfig(
        new MapConfig("role-by-name")
            .setTimeToLiveSeconds(900));

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
