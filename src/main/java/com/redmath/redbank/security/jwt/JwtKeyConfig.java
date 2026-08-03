package com.redmath.redbank.security.jwt;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtKeyConfig {

  @Bean
  KeyPair jwtKeyPair(@Value("${spring.security.jwt.private-key}") String privateKeyValue,
      @Value("${spring.security.jwt.public-key}") String publicKeyValue)
      throws GeneralSecurityException {

    byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyValue);
    byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyValue);

    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

    RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

    return new KeyPair(publicKey, privateKey);
  }

  @Bean
  JwtEncoder jwtEncoder(KeyPair jwtKeyPair) {
    RSAPublicKey publicKey = (RSAPublicKey) jwtKeyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) jwtKeyPair.getPrivate();

    return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
  }

  @Bean
  @Primary
  JwtDecoder jwtDecoder(KeyPair jwtKeyPair) {
    return createDecoder(jwtKeyPair, "access");
  }

  @Bean("refreshJwtDecoder")
  JwtDecoder refreshJwtDecoder(KeyPair jwtKeyPair) {
    return createDecoder(jwtKeyPair, "refresh");
  }

  private JwtDecoder createDecoder(KeyPair jwtKeyPair, String expectedTokenType) {
    RSAPublicKey publicKey = (RSAPublicKey) jwtKeyPair.getPublic();

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

    OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();

    OAuth2TokenValidator<Jwt> tokenTypeValidator = new JwtClaimValidator<>("tokenType",
        expectedTokenType::equals);

    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(defaultValidator, tokenTypeValidator));

    return decoder;
  }
}