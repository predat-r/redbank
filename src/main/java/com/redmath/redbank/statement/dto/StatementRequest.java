package com.redmath.redbank.statement.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatementRequest {

  @NotNull(message = "From date is required")
  private LocalDate fromDate;
  @NotNull(message = "To date is required")
  private LocalDate toDate;
}
