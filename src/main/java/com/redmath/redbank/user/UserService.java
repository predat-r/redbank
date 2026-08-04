package com.redmath.redbank.user;

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
}