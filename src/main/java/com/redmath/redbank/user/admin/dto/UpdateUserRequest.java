package com.redmath.redbank.user.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 20) String phoneNumber,
    @NotBlank @Size(min = 2, max = 150) String name,
    @NotBlank @Size(min = 5, max = 500) String address
) {

}
