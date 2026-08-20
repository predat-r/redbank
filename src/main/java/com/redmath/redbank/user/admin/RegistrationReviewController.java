package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.admin.dto.PendingRegistrationResponse;
import com.redmath.redbank.user.admin.dto.RejectRegistrationRequest;
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
    registrationReviewService.approveRegistration(
        userId,
        extractUserId(jwt)
    );
  }

  @PostMapping(value = "/{userId}/reject", consumes = "application/json")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rejectRegistration(
      @PathVariable Long userId,
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody RejectRegistrationRequest request
  ) {
    registrationReviewService.rejectRegistration(
        userId,
        extractUserId(jwt),
        request
    );
  }

  @GetMapping("/{userId}")
  public PendingRegistrationResponse findPendingRegistration(
      @PathVariable Long userId
  ) {
    return registrationReviewService.findPendingRegistration(userId);
  }

  private long extractUserId(Jwt jwt) {
    Object claim = jwt.getClaim("userId");

    if (!(claim instanceof Number userId)) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }

    return userId.longValue();
  }
}
