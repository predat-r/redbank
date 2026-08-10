package com.redmath.redbank.common.idempotency;

import java.io.Serializable;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey implements Serializable {
  private static final long serialVersionUID = 1L;

  private String idempotencyKey;
  private Long userId;
  private String requestPath;
  private String requestHash;
  private IdempotencyStatus status;
  private Integer responseStatusCode;
  private String responseBody;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
