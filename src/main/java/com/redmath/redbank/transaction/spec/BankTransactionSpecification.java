package com.redmath.redbank.transaction.spec;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionStatus;
import com.redmath.redbank.transaction.TransactionType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class BankTransactionSpecification {

  private BankTransactionSpecification() {
    // Private constructor for utility class
  }

  public static Specification<BankTransaction> filter(
      String reference,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      OffsetDateTime fromDate,
      OffsetDateTime toDate) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (reference != null && !reference.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("transactionReference")),
            "%" + reference.trim().toLowerCase() + "%"));
      }

      if (accountNumber != null && !accountNumber.isBlank()) {
        String searchPattern = "%" + accountNumber.trim().toLowerCase() + "%";
        Join<BankTransaction, AccountHolder> sourceJoin = root.join("sourceAccountHolder", JoinType.LEFT);
        Join<BankTransaction, AccountHolder> destJoin = root.join("destinationAccountHolder", JoinType.LEFT);

        Predicate sourceMatch = cb.like(cb.lower(sourceJoin.get("accountNumber")), searchPattern);
        Predicate destMatch = cb.like(cb.lower(destJoin.get("accountNumber")), searchPattern);

        predicates.add(cb.or(sourceMatch, destMatch));
      }

      if (type != null) {
        predicates.add(cb.equal(root.get("type"), type));
      }

      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }

      if (fromDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
      }

      if (toDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
