package com.redmath.redbank.locationrisk.reputation;

@FunctionalInterface
public interface IpReputationProvider {

  ReputationLookupResult lookup(String ip);
}
