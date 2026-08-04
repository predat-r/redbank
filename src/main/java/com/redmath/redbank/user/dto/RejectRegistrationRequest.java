package com.redmath.redbank.user.dto;

import jakarta.validation.constraints.Size;

public record RejectRegistrationRequest(
    @Size(max = 500)
    String rejectionReason
) {

}
