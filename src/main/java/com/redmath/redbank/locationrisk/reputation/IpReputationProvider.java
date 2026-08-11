package com.redmath.redbank.locationrisk.reputation;

public interface IpReputationProvider {

  ReputationLookupResult lookup(String ip);
}
