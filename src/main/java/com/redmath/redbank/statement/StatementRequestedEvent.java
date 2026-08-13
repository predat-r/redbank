package com.redmath.redbank.statement;

import java.time.LocalDate;

public record StatementRequestedEvent(Long accountHolderId, LocalDate fromDate, LocalDate toDate) {

}
