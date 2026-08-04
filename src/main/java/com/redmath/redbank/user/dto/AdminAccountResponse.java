package com.redmath.redbank.user.dto;

import com.redmath.redbank.account_holder.AccountStatus;
import java.time.OffsetDateTime;

public record AdminAccountResponse(Long id,
                                   String accountNumber,
                                   String currency,
                                   AccountStatus accountStatus,
                                   Long userId,
                                   String holderName,
                                   String holderEmail,
                                   OffsetDateTime approvedAt,
                                   OffsetDateTime createdAt,
                                   OffsetDateTime updatedAt) {

}