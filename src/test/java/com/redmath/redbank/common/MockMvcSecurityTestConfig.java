package com.redmath.redbank.common;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MockMvcSecurityTestConfig {

  @Bean
  MockMvcBuilderCustomizer csrfRequestCustomizer() {
    return builder -> builder.defaultRequest(get("/").with(csrf()));
  }
}
