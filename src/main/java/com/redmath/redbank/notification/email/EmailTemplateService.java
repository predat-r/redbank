package com.redmath.redbank.notification.email;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss 'UTC'");

    public String buildTransferCompletedSenderHtml(String senderName, BigDecimal amount, String destAccount, String reference, OffsetDateTime timestamp) {
        String title = "Transfer Sent Successfully";
        String subtitle = "Your funds have been transferred successfully.";
        String badgeColor = "#16a34a"; // Green
        String badgeText = "COMPLETED";
        
        String detailsHtml = buildDetailRow("Transaction Type", "Fund Transfer")
            + buildDetailRow("Amount Transferred", "$" + formatAmount(amount))
            + buildDetailRow("Recipient Account", destAccount)
            + buildDetailRow("Reference ID", reference)
            + buildDetailRow("Date & Time", formatDate(timestamp));

        return buildBaseHtmlTemplate(senderName, title, subtitle, badgeColor, badgeText, detailsHtml, "Need help? Contact RedBank support if you did not authorize this transaction.");
    }

    public String buildTransferCompletedReceiverHtml(String receiverName, BigDecimal amount, String sourceAccount, String reference, OffsetDateTime timestamp) {
        String title = "Funds Received!";
        String subtitle = "You have received a transfer into your RedBank account.";
        String badgeColor = "#16a34a"; // Green
        String badgeText = "COMPLETED";
        
        String detailsHtml = buildDetailRow("Transaction Type", "Fund Transfer Received")
            + buildDetailRow("Amount Received", "$" + formatAmount(amount))
            + buildDetailRow("Sender Account", sourceAccount)
            + buildDetailRow("Reference ID", reference)
            + buildDetailRow("Date & Time", formatDate(timestamp));

        return buildBaseHtmlTemplate(receiverName, title, subtitle, badgeColor, badgeText, detailsHtml, "The funds are now available in your balance.");
    }

    public String buildTransferCancelledSenderHtml(String senderName, BigDecimal amount, String destAccount, String reference, String reason, OffsetDateTime timestamp) {
        String title = "Transfer Cancelled / Reversed";
        String subtitle = "Your pending transfer request has been cancelled.";
        String badgeColor = "#dc2626"; // Red
        String badgeText = "CANCELLED";
        
        String detailsHtml = buildDetailRow("Transaction Type", "Fund Transfer")
            + buildDetailRow("Amount", "$" + formatAmount(amount))
            + buildDetailRow("Recipient Account", destAccount)
            + buildDetailRow("Reference ID", reference)
            + buildDetailRow("Reason", reason != null && !reason.isBlank() ? reason : "Security / System cancellation")
            + buildDetailRow("Date & Time", formatDate(timestamp));

        return buildBaseHtmlTemplate(senderName, title, subtitle, badgeColor, badgeText, detailsHtml, "Any reserved funds have been returned to your available balance.");
    }

    public String buildDepositCompletedHtml(String accountHolderName, BigDecimal amount, String accountNumber, String reference, OffsetDateTime timestamp) {
        String title = "Deposit Credited";
        String subtitle = "A deposit has been credited to your RedBank account.";
        String badgeColor = "#16a34a"; // Green
        String badgeText = "COMPLETED";
        
        String detailsHtml = buildDetailRow("Transaction Type", "Account Deposit")
            + buildDetailRow("Amount Credited", "$" + formatAmount(amount))
            + buildDetailRow("Account Number", accountNumber)
            + buildDetailRow("Reference ID", reference)
            + buildDetailRow("Date & Time", formatDate(timestamp));

        return buildBaseHtmlTemplate(accountHolderName, title, subtitle, badgeColor, badgeText, detailsHtml, "Your updated balance is reflected in your dashboard.");
    }

    public String buildWithdrawalCompletedHtml(String accountHolderName, BigDecimal amount, String accountNumber, String reference, OffsetDateTime timestamp) {
        String title = "Withdrawal Successful";
        String subtitle = "A cash withdrawal was processed from your account.";
        String badgeColor = "#2563eb"; // Blue
        String badgeText = "COMPLETED";
        
        String detailsHtml = buildDetailRow("Transaction Type", "Cash Withdrawal")
            + buildDetailRow("Amount Withdrawn", "$" + formatAmount(amount))
            + buildDetailRow("Account Number", accountNumber)
            + buildDetailRow("Reference ID", reference)
            + buildDetailRow("Date & Time", formatDate(timestamp));

        return buildBaseHtmlTemplate(accountHolderName, title, subtitle, badgeColor, badgeText, detailsHtml, "If you did not make this withdrawal, please freeze your account immediately.");
    }

    private String buildDetailRow(String label, String value) {
        return "<tr>"
            + "<td style=\"padding: 10px 0; color: #64748b; font-size: 14px; font-weight: 500; border-bottom: 1px dashed #f1f5f9;\">" + label + "</td>"
            + "<td style=\"padding: 10px 0; color: #0f172a; font-size: 14px; font-weight: 600; text-align: right; border-bottom: 1px dashed #f1f5f9;\">" + value + "</td>"
            + "</tr>";
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%,.2f", amount) : "0.00";
    }

    private String formatDate(OffsetDateTime timestamp) {
        return timestamp != null ? timestamp.format(DATE_FORMATTER) : "N/A";
    }

    private String buildBaseHtmlTemplate(String recipientName, String title, String subtitle, String badgeColor, String badgeText, String detailsHtml, String footerNote) {
        return "<!DOCTYPE html>"
            + "<html>"
            + "<head>"
            + "<meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "</head>"
            + "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">"
            + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9; padding: 40px 15px;\">"
            + "<tr><td align=\"center\">"
            + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width: 580px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.05); border: 1px solid #e2e8f0;\">"
            
            // Header Bar
            + "<tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%); padding: 32px 30px; text-align: center;\">"
            + "<div style=\"display: inline-block; background-color: #ef4444; color: #ffffff; padding: 8px 18px; border-radius: 8px; font-weight: 800; font-size: 20px; letter-spacing: 1px; text-transform: uppercase;\">RedBank</div>"
            + "<h1 style=\"color: #ffffff; margin: 18px 0 6px 0; font-size: 22px; font-weight: 700;\">" + title + "</h1>"
            + "<p style=\"color: #94a3b8; margin: 0; font-size: 14px;\">" + subtitle + "</p>"
            + "</td></tr>"

            // Body Content
            + "<tr><td style=\"padding: 32px 30px;\">"
            + "<p style=\"color: #334155; font-size: 15px; margin-top: 0; margin-bottom: 20px;\">Hello <strong>" + (recipientName != null ? recipientName : "Valued Customer") + "</strong>,</p>"
            
            // Status Badge Card
            + "<div style=\"background-color: #f8fafc; border-radius: 12px; padding: 20px; border: 1px solid #e2e8f0; margin-bottom: 24px;\">"
            + "<div style=\"margin-bottom: 12px; text-align: right;\">"
            + "<span style=\"background-color: " + badgeColor + "; color: #ffffff; font-size: 11px; font-weight: 700; padding: 5px 12px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px;\">" + badgeText + "</span>"
            + "</div>"
            + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse: collapse;\">"
            + detailsHtml
            + "</table>"
            + "</div>"

            // Note/Action
            + "<p style=\"color: #64748b; font-size: 13px; line-height: 1.6; margin: 0;\">" + footerNote + "</p>"
            + "</td></tr>"

            // Footer
            + "<tr><td style=\"background-color: #f8fafc; padding: 20px 30px; text-align: center; border-top: 1px solid #e2e8f0;\">"
            + "<p style=\"color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.6;\">This is an automated security notification from RedBank. Please do not reply directly to this email.<br>&copy; 2026 RedBank Inc. All rights reserved.</p>"
            + "</td></tr>"

            + "</table>"
            + "</td></tr>"
            + "</table>"
            + "</body>"
            + "</html>";
    }
}
