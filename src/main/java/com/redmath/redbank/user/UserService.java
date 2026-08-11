package com.redmath.redbank.user;

import com.redmath.redbank.common.exception.DuplicateUserException;
import com.redmath.redbank.common.exception.InvalidUserStatusTransitionException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.dto.UpdateMyProfileRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmailIgnoreCase(email);
  }

  @Transactional(readOnly = true)
  public boolean existsByPhoneNumber(String phoneNumber) {
    return userRepository.existsByPhoneNumber(phoneNumber);
  }

  @Transactional
  public User save(User user) {
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public Optional<User> findById(Long userId) {
    return userRepository.findById(userId);
  }

  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmailIgnoreCase(email);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<User> findByIdForUpdate(Long userId) {
    return userRepository.findByIdForUpdate(userId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Optional<User> findByEmailForUpdate(String email) {
    return userRepository.findByEmailForUpdate(email);
  }

  @Transactional(readOnly = true)
  public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Page<User> findAllByStatus(UserStatus status, Pageable pageable) {
    return userRepository.findAllByStatus(status, pageable);
  }

  @Transactional
  public User updateMyProfile(Long userId, UpdateMyProfileRequest request) {
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(UserNotFoundException::new);
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    String phoneNumber = request.phoneNumber().trim();

    userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
      if (!existing.getId().equals(userId)) {
        throw new DuplicateUserException("Email is already registered");
      }
    });
    if (!user.getPhoneNumber().equals(phoneNumber) && userRepository.existsByPhoneNumber(
        phoneNumber)) {
      throw new DuplicateUserException("Phone number is already registered");
    }

    user.updateDetails(email, phoneNumber, request.name().trim(), request.address().trim(),
        Instant.now());
    return user;
  }


  @Transactional(propagation = Propagation.MANDATORY)
  public boolean deactivateUser(Long userId) {
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(UserNotFoundException::new);

    if (user.getStatus() == UserStatus.DEACTIVATED) {
      return false;
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new InvalidUserStatusTransitionException(
          "Only active users can be deactivated"
      );
    }

    user.deactivate(Instant.now());
    return true;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public boolean reactivateUser(Long userId) {
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(UserNotFoundException::new);

    if (user.getStatus() == UserStatus.ACTIVE) {
      return false;
    }

    if (user.getStatus() != UserStatus.DEACTIVATED) {
      throw new InvalidUserStatusTransitionException(
          "Only deactivated users can be reactivated"
      );
    }

    user.reactivate(Instant.now());
    return true;
  }
}
