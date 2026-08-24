package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.statement.dto.StatementRequest;
import com.redmath.redbank.statement.dto.StatementResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/accounts/me/statement")
@Tag(name = "Statement", description = "Bank Statement APIs")
@PreAuthorize("hasRole('ACCOUNT_HOLDER')")
public class StatementController {

  private final StatementRequestService statementRequestService;
  private final AccountHolderService accountHolderService;

  public StatementController(StatementRequestService statementRequestService, AccountHolderService accountHolderService) {
    this.statementRequestService = statementRequestService;
    this.accountHolderService = accountHolderService;
  }

  @PostMapping(consumes = "application/json")
  @Operation(summary = "Request a bank statement (delivered asynchronously via email)")
  public ResponseEntity<StatementResponse> requestStatement(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody StatementRequest request) {
    Long userId = jwt.getClaim("userId");
    AccountHolder accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    StatementResponse response = statementRequestService.requestStatement(request, accountHolder.getId());
    return ResponseEntity.ok(response);
  }
}
