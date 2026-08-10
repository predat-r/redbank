package com.redmath.redbank.transaction.spec;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionCategory;
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
    return filterInternal(null, reference, accountNumber, type, status, null, fromDate, toDate);
  }

  public static Specification<BankTransaction> filter(
      String reference,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      TransactionCategory category,
      OffsetDateTime fromDate,
      OffsetDateTime toDate) {
    return filterInternal(null, reference, accountNumber, type, status, category, fromDate, toDate);
  }

  public static Specification<BankTransaction> filterForUser(
      Long accountHolderId,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      OffsetDateTime fromDate,
      OffsetDateTime toDate) {
    return filterInternal(accountHolderId, null, accountNumber, type, status, null, fromDate, toDate);
  }

  public static Specification<BankTransaction> filterForUser(
      Long accountHolderId,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      TransactionCategory category,
      OffsetDateTime fromDate,
      OffsetDateTime toDate) {
    return filterInternal(accountHolderId, null, accountNumber, type, status, category, fromDate, toDate);
  }

  private static Specification<BankTransaction> filterInternal(
      Long accountHolderId,
      String reference,
      String accountNumber,
      TransactionType type,
      TransactionStatus status,
      TransactionCategory category,
      OffsetDateTime fromDate,
      OffsetDateTime toDate) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      Join<BankTransaction, AccountHolder> sourceJoin = null;
      Join<BankTransaction, AccountHolder> destJoin = null;

      if (accountHolderId != null) {
        sourceJoin = root.join("sourceAccountHolder", JoinType.LEFT);
        destJoin = root.join("destinationAccountHolder", JoinType.LEFT);
        predicates.add(cb.or(
            cb.equal(sourceJoin.get("id"), accountHolderId),
            cb.equal(destJoin.get("id"), accountHolderId)
        ));
      }

      if (reference != null && !reference.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("transactionReference")),
            "%" + reference.trim().toLowerCase() + "%"));
      }

      if (accountNumber != null && !accountNumber.isBlank()) {
        String searchPattern = "%" + accountNumber.trim().toLowerCase() + "%";
        if (sourceJoin == null) {
          sourceJoin = root.join("sourceAccountHolder", JoinType.LEFT);
        }
        if (destJoin == null) {
          destJoin = root.join("destinationAccountHolder", JoinType.LEFT);
        }

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

      if (category != null) {
        predicates.add(cb.equal(root.get("category"), category));
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
