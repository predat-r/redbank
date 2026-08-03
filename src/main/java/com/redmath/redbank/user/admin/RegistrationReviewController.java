package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.dto.PendingRegistrationResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}