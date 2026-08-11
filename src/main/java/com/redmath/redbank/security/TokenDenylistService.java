package com.redmath.redbank.security;

import java.time.Duration;

public interface TokenDenylistService {

  void deny(String jti, Duration duration);

  boolean isDenylisted(String jti);

  void remove(String jti);
}
