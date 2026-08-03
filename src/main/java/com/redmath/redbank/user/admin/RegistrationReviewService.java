package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.dto.PendingRegistrationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationReviewService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Page<PendingRegistrationResponse> findPendingRegistrations(Pageable pageable) {
    return userRepository
        .findAllByStatus(UserStatus.PENDING_APPROVAL, pageable)
        .map(this::toResponse);
  }

  private PendingRegistrationResponse toResponse(User user) {
    return new PendingRegistrationResponse(
        user.getId(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getName(),
        user.getAddress(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }
}