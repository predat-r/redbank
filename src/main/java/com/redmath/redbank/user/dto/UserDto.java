package com.redmath.redbank.user.dto;

import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

  private Long id;
  private String email;
  private String phoneNumber;
  private String name;
  private String address;
  private UserStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  public static UserDto from(User user) {
    if (user == null) {
      return null;
    }
    return UserDto.builder()
        .id(user.getId())
        .email(user.getEmail())
        .phoneNumber(user.getPhoneNumber())
        .name(user.getName())
        .address(user.getAddress())
        .status(user.getStatus())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }
}
