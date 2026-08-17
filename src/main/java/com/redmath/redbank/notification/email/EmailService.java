package com.redmath.redbank.notification.email;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
  );

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username:${app.email.sender:no-reply@redbank.com}}")
  private String fromEmailAddress;

  @Value("${spring.mail.host:}")
  private String mailHost;

  public boolean sendEmail(EmailMessage emailMessage) {
    // Silently skip if email configuration (username/host) is missing or default placeholder
    if (!isEmailConfigured()) {
      log.debug("Email sending skipped: SMTP username or host configuration is incomplete.");
      return false;
    }

    if (emailMessage == null || !StringUtils.hasText(emailMessage.getTo())) {
      log.debug("Email sending skipped: Recipient address is null or empty.");
      return false;
    }

    String recipient = emailMessage.getTo().trim();
    if (!EMAIL_PATTERN.matcher(recipient).matches()) {
      log.debug("Email sending skipped: Invalid recipient email format '{}'", recipient);
      return false;
    }

    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      String cleanFromAddress = fromEmailAddress.contains("<")
          ? fromEmailAddress.replaceAll(".*<([^>]+)>.*", "$1")
          : fromEmailAddress;

      helper.setFrom(new InternetAddress(cleanFromAddress, "Redbank"));
      helper.setTo(recipient);
      helper.setSubject(emailMessage.getSubject());
      helper.setText(emailMessage.getBody(), emailMessage.isHtml());

      if (emailMessage.isHtml()) {
        Resource logoResource = new ClassPathResource("static/branding/logo.png");
        if (logoResource.exists()) {
          helper.addInline("logo", logoResource);
        }
      }

      javaMailSender.send(message);
      log.info("Email successfully sent to '{}' with subject '{}'", recipient,
          emailMessage.getSubject());
      return true;

    } catch (Exception e) {
      log.debug("Email sending silently suppressed for '{}': {}", recipient, e.getMessage());
      return false;
    }
  }

  private boolean isEmailConfigured() {
    if (!StringUtils.hasText(fromEmailAddress) || !StringUtils.hasText(mailHost)) {
      return false;
    }
    return true;
  }
}
