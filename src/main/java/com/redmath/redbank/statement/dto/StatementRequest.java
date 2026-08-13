package com.redmath.redbank.statement.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatementRequest {
  private LocalDate fromDate;
  private LocalDate toDate;
}
