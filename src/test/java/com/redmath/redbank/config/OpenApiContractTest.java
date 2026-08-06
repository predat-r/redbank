package com.redmath.redbank.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

  private static final Set<String> HTTP_METHODS = Set.of(
      "get", "post", "put", "patch", "delete", "options", "head", "trace");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void generatedContractIsReadyForFrontendConsumption() throws Exception {
    String document = mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode openApi = objectMapper.readTree(document);

    assertPublicOperation(openApi, "/api/auth/register", "post");
    assertPublicOperation(openApi, "/api/auth/login", "post");
    assertPublicOperation(openApi, "/api/auth/refresh", "post");
    assertPublicOperation(openApi, "/api/auth/logout", "post");

    assertResponse(openApi, "/api/admin/deposits", "post", "201");
    assertResponse(openApi, "/api/admin/users", "post", "201");
    assertResponse(openApi, "/api/admin/users/{userId}/deactivate", "patch", "204");
    assertResponse(openApi, "/api/accounts/me/transfers", "post", "201");
    assertResponse(openApi, "/api/accounts/me/withdrawals", "post", "201");
    assertResponse(openApi, "/api/admin/accounts/freeze/{accountId}", "patch", "204");
    assertResponse(openApi, "/api/accounts/freeze/me", "patch", "204");

    assertEveryOperationHasSuccessAndStandardErrors(openApi);
    assertFalse(document.contains("\"*/*\""));

    JsonNode schemas = openApi.path("components").path("schemas");
    assertRequiredFields(schemas.path("ApiError"),
        "timestamp", "status", "error", "message", "path");
    assertRequiredFields(schemas.path("LoginResponse"),
        "accessToken", "tokenType");
    assertRequiredFields(schemas.path("BankTransactionDto"),
        "id", "transactionReference", "type", "amount", "status", "createdAt");
    assertRequiredFields(schemas.path("PageMetadata"),
        "size", "number", "totalElements", "totalPages");

    JsonNode badRequest = openApi.path("components").path("responses").path("BadRequest");
    assertEquals("#/components/schemas/ApiError",
        badRequest.path("content").path("application/json").path("schema").path("$ref").asText());
  }

  private void assertEveryOperationHasSuccessAndStandardErrors(JsonNode openApi) {
    int operationCount = 0;
    for (Map.Entry<String, JsonNode> pathEntry : openApi.path("paths").properties()) {
      for (Map.Entry<String, JsonNode> methodEntry : pathEntry.getValue().properties()) {
        if (!HTTP_METHODS.contains(methodEntry.getKey())) {
          continue;
        }
        operationCount++;
        JsonNode responses = methodEntry.getValue().path("responses");
        assertTrue(hasSuccessfulResponse(responses),
            () -> pathEntry.getKey() + " " + methodEntry.getKey() + " has no success response");
        assertNotNull(responses.get("400"),
            () -> pathEntry.getKey() + " " + methodEntry.getKey() + " has no 400 response");
        assertNotNull(responses.get("500"),
            () -> pathEntry.getKey() + " " + methodEntry.getKey() + " has no 500 response");
        assertJsonResponseMediaTypes(responses, pathEntry.getKey(), methodEntry.getKey());
      }
    }
    assertEquals(36, operationCount);
  }

  private boolean hasSuccessfulResponse(JsonNode responses) {
    return responses.properties().stream()
        .anyMatch(entry -> entry.getKey().startsWith("2"));
  }

  private void assertJsonResponseMediaTypes(JsonNode responses, String path, String method) {
    for (Map.Entry<String, JsonNode> responseEntry : responses.properties()) {
      JsonNode content = responseEntry.getValue().path("content");
      if (content.isMissingNode()) {
        continue;
      }
      assertEquals(Set.of("application/json"),
          content.properties().stream()
              .map(Map.Entry::getKey)
              .collect(java.util.stream.Collectors.toSet()),
          () -> path + " " + method + " response " + responseEntry.getKey()
              + " has a non-JSON media type");
    }
  }

  private void assertPublicOperation(JsonNode openApi, String path, String method) {
    JsonNode security = openApi.path("paths").path(path).path(method).path("security");
    assertTrue(security.isArray());
    assertTrue(security.isEmpty());
  }

  private void assertResponse(JsonNode openApi, String path, String method, String statusCode) {
    JsonNode responses = openApi.path("paths").path(path).path(method).path("responses");
    assertNotNull(responses.get(statusCode));
  }

  private void assertRequiredFields(JsonNode schema, String... fields) {
    Set<String> required = new HashSet<>();
    schema.path("required").forEach(node -> required.add(node.asText()));
    assertTrue(required.containsAll(Set.of(fields)));
  }
}
