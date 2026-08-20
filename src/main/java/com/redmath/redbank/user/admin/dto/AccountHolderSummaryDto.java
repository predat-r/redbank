package com.redmath.redbank.user.admin.dto;

import java.time.OffsetDateTime;

public record AccountHolderSummaryDto(
    Long id,
    String accountNumber,
    String currency,
    String accountStatus,
    OffsetDateTime approvedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

}
