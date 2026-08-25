package com.redmath.redbank.notification.email;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionType;
import com.redmath.redbank.transaction.event.TransactionCancelledEvent;
import com.redmath.redbank.transaction.event.TransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

  private final EmailService emailService;
  private final EmailTemplateService emailTemplateService;
  private final BankTransactionRepository bankTransactionRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTransactionCompleted(TransactionCompletedEvent event) {
    if (event == null || event.getTransactionId() == null) {
      return;
    }

    try {
      BankTransaction transaction = bankTransactionRepository.findById(event.getTransactionId())
          .orElse(null);
      if (transaction == null) {
        return;
      }

      if (transaction.getType() == TransactionType.TRANSFER) {
        handleTransferCompleted(transaction);
      } else if (transaction.getType() == TransactionType.DEPOSIT) {
        handleDepositCompleted(transaction);
      } else if (transaction.getType() == TransactionType.WITHDRAWAL) {
        handleWithdrawalCompleted(transaction);
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error processing TransactionCompletedEvent for transaction ID {}: {}",
            event.getTransactionId(), e.getMessage(), e);
      }
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTransactionCancelled(TransactionCancelledEvent event) {
    if (event == null || event.getTransactionId() == null) {
      return;
    }

    try {
      BankTransaction transaction = bankTransactionRepository.findById(event.getTransactionId())
          .orElse(null);
      if (transaction == null) {
        return;
      }

      if (transaction.getType() == TransactionType.TRANSFER) {
        handleTransferCancelled(transaction, event.getReason());
      } else if (transaction.getType() == TransactionType.WITHDRAWAL) {
        handleWithdrawalCancelled(transaction, event.getReason());
      }

    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error processing TransactionCancelledEvent for transaction ID {}: {}",
            event.getTransactionId(), e.getMessage(), e);
      }
    }
  }

  private void handleTransferCompleted(BankTransaction transaction) {
    if (transaction.getSourceAccountHolder() != null
        && transaction.getSourceAccountHolder().getUser() != null) {
      String senderEmail = transaction.getSourceAccountHolder().getUser().getEmail();
      String senderName = transaction.getSourceAccountHolder().getUser().getName();
      String destAccNum = transaction.getDestinationAccountHolder() != null
          ? transaction.getDestinationAccountHolder().getAccountNumber()
          : "N/A";

      String htmlBody = emailTemplateService.buildTransferCompletedSenderHtml(
          senderName, transaction.getAmount(), destAccNum, transaction.getTransactionReference(),
          transaction.getCompletedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(senderEmail)
          .subject("RedBank: Transfer Sent Successfully")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }

    if (transaction.getDestinationAccountHolder() != null
        && transaction.getDestinationAccountHolder().getUser() != null) {
      String receiverEmail = transaction.getDestinationAccountHolder().getUser().getEmail();
      String receiverName = transaction.getDestinationAccountHolder().getUser().getName();
      String sourceAccNum = transaction.getSourceAccountHolder() != null
          ? transaction.getSourceAccountHolder().getAccountNumber()
          : "N/A";

      String htmlBody = emailTemplateService.buildTransferCompletedReceiverHtml(
          receiverName, transaction.getAmount(), sourceAccNum,
          transaction.getTransactionReference(), transaction.getCompletedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(receiverEmail)
          .subject("RedBank: You Received Funds!")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }
  }

  private void handleTransferCancelled(BankTransaction transaction, String reason) {
    if (transaction.getSourceAccountHolder() != null
        && transaction.getSourceAccountHolder().getUser() != null) {
      String senderEmail = transaction.getSourceAccountHolder().getUser().getEmail();
      String senderName = transaction.getSourceAccountHolder().getUser().getName();
      String destAccNum = transaction.getDestinationAccountHolder() != null
          ? transaction.getDestinationAccountHolder().getAccountNumber()
          : "N/A";

      String htmlBody = emailTemplateService.buildTransferCancelledSenderHtml(
          senderName, transaction.getAmount(), destAccNum, transaction.getTransactionReference(),
          reason, transaction.getCreatedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(senderEmail)
          .subject("RedBank: Transfer Request Cancelled")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }
  }

  private void handleDepositCompleted(BankTransaction transaction) {
    if (transaction.getDestinationAccountHolder() != null
        && transaction.getDestinationAccountHolder().getUser() != null) {
      String email = transaction.getDestinationAccountHolder().getUser().getEmail();
      String name = transaction.getDestinationAccountHolder().getUser().getName();
      String accNum = transaction.getDestinationAccountHolder().getAccountNumber();

      String htmlBody = emailTemplateService.buildDepositCompletedHtml(
          name, transaction.getAmount(), accNum, transaction.getTransactionReference(),
          transaction.getCompletedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(email)
          .subject("RedBank: Deposit Credited to Your Account")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }
  }

  private void handleWithdrawalCompleted(BankTransaction transaction) {
    if (transaction.getSourceAccountHolder() != null
        && transaction.getSourceAccountHolder().getUser() != null) {
      String email = transaction.getSourceAccountHolder().getUser().getEmail();
      String name = transaction.getSourceAccountHolder().getUser().getName();
      String accNum = transaction.getSourceAccountHolder().getAccountNumber();

      String htmlBody = emailTemplateService.buildWithdrawalCompletedHtml(
          name, transaction.getAmount(), accNum, transaction.getTransactionReference(),
          transaction.getCompletedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(email)
          .subject("RedBank: Withdrawal Successful")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }
  }

  private void handleWithdrawalCancelled(BankTransaction transaction, String reason) {
    if (transaction.getSourceAccountHolder() != null
        && transaction.getSourceAccountHolder().getUser() != null) {
      String email = transaction.getSourceAccountHolder().getUser().getEmail();
      String name = transaction.getSourceAccountHolder().getUser().getName();
      String accNum = transaction.getSourceAccountHolder().getAccountNumber();

      String htmlBody = emailTemplateService.buildTransferCancelledSenderHtml(
          name, transaction.getAmount(), accNum, transaction.getTransactionReference(), reason,
          transaction.getCreatedAt()
      );

      emailService.sendEmail(EmailMessage.builder()
          .to(email)
          .subject("RedBank: Withdrawal Request Cancelled")
          .body(htmlBody)
          .isHtml(true)
          .build());
    }
  }
}
