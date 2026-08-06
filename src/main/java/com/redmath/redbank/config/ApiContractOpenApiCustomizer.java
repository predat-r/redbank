package com.redmath.redbank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiContractOpenApiCustomizer implements OpenApiCustomizer {

  private static final String API_ERROR_SCHEMA = "#/components/schemas/ApiError";
  private static final String COMPONENT_RESPONSE_PREFIX = "#/components/responses/";

  private static final Set<String> PUBLIC_OPERATIONS = Set.of(
      "register", "login", "refresh", "logout");

  private static final Set<String> PUBLIC_UNAUTHORIZED_OPERATIONS = Set.of(
      "login", "refresh", "logout");

  private static final Set<String> PUBLIC_FORBIDDEN_OPERATIONS = Set.of("login", "refresh");

  private static final Set<String> NOT_FOUND_OPERATIONS = Set.of(
      "getRegistrationStatus",
      "approveRegistration",
      "rejectRegistration",
      "findPendingRegistration",
      "findUser",
      "updateUser",
      "deactivateUser",
      "reactivateUser",
      "getAccountHolder",
      "getMyAccountHolder",
      "getAccountHolderByAccountNumber",
      "freezeAccountHolder",
      "deactivateAccountHolder",
      "freezeMyAccountHolder",
      "deactivateMyAccountHolder",
      "getLatestBalance",
      "findAuditLog",
      "getMyTransactions",
      "createTransfer",
      "createWithdrawal",
      "createDeposit",
      "getTransactionsByAccountNumber",
      "getTransactionById",
      "getTransactionByReference");

  private static final Set<String> CONFLICT_OPERATIONS = Set.of(
      "register",
      "createUser",
      "updateUser",
      "deactivateUser",
      "reactivateUser",
      "approveRegistration",
      "rejectRegistration",
      "findPendingRegistration",
      "freezeAccountHolder",
      "deactivateAccountHolder",
      "freezeMyAccountHolder",
      "deactivateMyAccountHolder");

  private static final Map<String, String> CREATED_OPERATIONS = Map.of(
      "createUser", "User created",
      "createDeposit", "Deposit created",
      "createTransfer", "Transfer created",
      "createWithdrawal", "Withdrawal created");

  private static final Set<String> NO_CONTENT_OPERATIONS = Set.of(
      "deactivateUser",
      "reactivateUser",
      "freezeAccountHolder",
      "deactivateAccountHolder",
      "freezeMyAccountHolder",
      "deactivateMyAccountHolder");

  private static final Map<String, List<String>> REQUIRED_RESPONSE_FIELDS = Map.ofEntries(
      Map.entry("ApiError", List.of("timestamp", "status", "error", "message", "path")),
      Map.entry("LoginResponse", List.of("accessToken", "tokenType")),
      Map.entry("RegisterResponse", List.of("id", "email", "status", "tokens")),
      Map.entry("RegistrationStatusResponse", List.of("userId", "status")),
      Map.entry("AdminUserResponse",
          List.of("id", "email", "phoneNumber", "name", "address", "status", "createdAt",
              "updatedAt")),
      Map.entry("CreateUserResponse", List.of("user", "accountHolder")),
      Map.entry("PendingRegistrationResponse",
          List.of("id", "email", "phoneNumber", "name", "address", "status", "createdAt")),
      Map.entry("AccountHolderDto",
          List.of("id", "userId", "accountNumber", "currency", "accountStatus", "approvedAt",
              "createdAt", "updatedAt")),
      Map.entry("BalanceDto", List.of("amount", "runningBalance")),
      Map.entry("AuditLogResponse",
          List.of("id", "actorUserId", "actorEmail", "action", "targetType",
              "targetIdentifier", "createdAt")),
      Map.entry("BankTransactionDto",
          List.of("id", "transactionReference", "type", "amount", "status", "createdAt")),
      Map.entry("AdminBankTransactionDetailDto",
          List.of("id", "transactionReference", "type", "amount", "status", "createdAt")),
      Map.entry("PageMetadata",
          List.of("size", "number", "totalElements", "totalPages")),
      Map.entry("PagedModelAdminUserResponse", List.of("content", "page")),
      Map.entry("PagedModelBankTransactionDto", List.of("content", "page")),
      Map.entry("PagedModelPendingRegistrationResponse", List.of("content", "page")),
      Map.entry("PagedModelBalanceDto", List.of("content", "page")),
      Map.entry("PagedModelAuditLogResponse", List.of("content", "page")),
      Map.entry("PagedModelAccountHolderDto", List.of("content", "page")));

  @Override
  public void customise(OpenAPI openApi) {
    Components components = ensureComponents(openApi);
    addApiErrorSchema(components);
    addReusableErrorResponses(components);

    openApi.getPaths().values().forEach(pathItem ->
        pathItem.readOperations().forEach(operation -> customizeOperation(operation)));

    markRequiredResponseFields(components);
  }

  private Components ensureComponents(OpenAPI openApi) {
    if (openApi.getComponents() == null) {
      openApi.setComponents(new Components());
    }
    return openApi.getComponents();
  }

  private void addApiErrorSchema(Components components) {
    ObjectSchema apiError = new ObjectSchema();
    apiError.addProperty("timestamp", new StringSchema().format("date-time"));
    apiError.addProperty("status", new IntegerSchema().format("int32"));
    apiError.addProperty("error", new StringSchema());
    apiError.addProperty("message", new StringSchema());
    apiError.addProperty("path", new StringSchema());
    apiError.setRequired(REQUIRED_RESPONSE_FIELDS.get("ApiError"));
    components.addSchemas("ApiError", apiError);
  }

  private void addReusableErrorResponses(Components components) {
    components.addResponses("BadRequest", errorResponse("Bad Request"));
    components.addResponses("Unauthorized", errorResponse("Unauthorized"));
    components.addResponses("Forbidden", errorResponse("Forbidden"));
    components.addResponses("NotFound", errorResponse("Not Found"));
    components.addResponses("Conflict", errorResponse("Conflict"));
    components.addResponses("InternalServerError", errorResponse("Internal Server Error"));
  }

  private ApiResponse errorResponse(String description) {
    io.swagger.v3.oas.models.media.MediaType mediaType =
        new io.swagger.v3.oas.models.media.MediaType();
    mediaType.setSchema(new Schema<>().$ref(API_ERROR_SCHEMA));
    Content content = new Content();
    content.addMediaType(MediaType.APPLICATION_JSON_VALUE, mediaType);
    return new ApiResponse().description(description).content(content);
  }

  private void customizeOperation(Operation operation) {
    String operationId = operation.getOperationId();
    boolean publicOperation = PUBLIC_OPERATIONS.contains(operationId);

    if (publicOperation) {
      operation.setSecurity(Collections.emptyList());
    }

    normalizeSuccessResponse(operation, operationId);
    normalizeResponseMediaTypes(operation.getResponses());
    addErrorResponse(operation, "400", "BadRequest");
    addErrorResponse(operation, "500", "InternalServerError");

    if (!publicOperation || PUBLIC_UNAUTHORIZED_OPERATIONS.contains(operationId)) {
      addErrorResponse(operation, "401", "Unauthorized");
    }
    if (!publicOperation || PUBLIC_FORBIDDEN_OPERATIONS.contains(operationId)) {
      addErrorResponse(operation, "403", "Forbidden");
    }
    if (NOT_FOUND_OPERATIONS.contains(operationId)) {
      addErrorResponse(operation, "404", "NotFound");
    }
    if (CONFLICT_OPERATIONS.contains(operationId)) {
      addErrorResponse(operation, "409", "Conflict");
    }
  }

  private void normalizeSuccessResponse(Operation operation, String operationId) {
    if (CREATED_OPERATIONS.containsKey(operationId)) {
      moveSuccessResponse(operation.getResponses(), "200", "201",
          CREATED_OPERATIONS.get(operationId), false);
    } else if (NO_CONTENT_OPERATIONS.contains(operationId)) {
      moveSuccessResponse(operation.getResponses(), "200", "204", "No Content", true);
    }
  }

  private void moveSuccessResponse(ApiResponses responses, String oldCode, String newCode,
      String description, boolean removeContent) {
    ApiResponse response = responses.get(newCode);
    if (response == null) {
      response = responses.remove(oldCode);
    }
    if (response == null) {
      response = new ApiResponse();
    }
    response.setDescription(description);
    if (removeContent) {
      response.setContent(null);
    }
    responses.addApiResponse(newCode, response);
  }

  private void normalizeResponseMediaTypes(ApiResponses responses) {
    responses.values().forEach(response -> {
      Content content = response.getContent();
      if (content == null) {
        return;
      }
      io.swagger.v3.oas.models.media.MediaType wildcard = content.remove("*/*");
      if (wildcard != null) {
        content.putIfAbsent(MediaType.APPLICATION_JSON_VALUE, wildcard);
      }
    });
  }

  private void addErrorResponse(Operation operation, String statusCode, String componentName) {
    ApiResponse response = new ApiResponse()
        .$ref(COMPONENT_RESPONSE_PREFIX + componentName);
    operation.getResponses().addApiResponse(statusCode, response);
  }

  private void markRequiredResponseFields(Components components) {
    if (components.getSchemas() == null) {
      return;
    }
    REQUIRED_RESPONSE_FIELDS.forEach((schemaName, requiredFields) -> {
      Schema<?> schema = components.getSchemas().get(schemaName);
      if (schema != null) {
        schema.setRequired(requiredFields);
      }
    });
  }
}
