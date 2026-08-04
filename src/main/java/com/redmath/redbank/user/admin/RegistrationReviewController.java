package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.dto.PendingRegistrationResponse;
import com.redmath.redbank.user.dto.RejectRegistrationRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/registrations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RegistrationReviewController {

  private final RegistrationReviewService registrationReviewService;

  @GetMapping
  public Page<PendingRegistrationResponse> findPendingRegistrations(
      @PageableDefault(
          size = 20,
          sort = {"createdAt", "id"},
          direction = Sort.Direction.ASC
      )
      Pageable pageable
  ) {
    return registrationReviewService.findPendingRegistrations(pageable);
  }

  @PostMapping("/{userId}/approve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void approveRegistration(
      @PathVariable Long userId,
      @AuthenticationPrincipal Jwt jwt
  ) {
    Number adminUserIdClaim = jwt.getClaim("userId");

    registrationReviewService.approveRegistration(
        userId,
        adminUserIdClaim.longValue()
    );
  }

  @PostMapping("/{userId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rejectRegistration(
      @PathVariable Long userId,
      @Valid @RequestBody RejectRegistrationRequest request
  ) {
    registrationReviewService.rejectRegistration(userId, request);
  }

  @GetMapping("/{userId}")
  public PendingRegistrationResponse findPendingRegistration(
      @PathVariable Long userId
  ) {
    return registrationReviewService.findPendingRegistration(userId);
  }
}