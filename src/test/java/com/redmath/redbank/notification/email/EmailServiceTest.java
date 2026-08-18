package com.redmath.redbank.notification.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender javaMailSender;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
    emailService = new EmailService(javaMailSender);
    ReflectionTestUtils.setField(emailService, "fromEmailAddress", "no-reply@redbank.com");
    ReflectionTestUtils.setField(emailService, "mailHost", "smtp.redbank.com");
  }

  @Test
  void sendEmail_htmlEmail_attachesLogoAndSends() {
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    doNothing().when(javaMailSender).send(any(MimeMessage.class));

    EmailMessage message = EmailMessage.builder()
        .to("user@example.com")
        .subject("Test Subject")
        .body("<html><body><img src=\"cid:logo\">Hello</body></html>")
        .isHtml(true)
        .build();

    boolean result = emailService.sendEmail(message);

    assertTrue(result);
    verify(javaMailSender).send(mimeMessage);
  }

  @Test
  void sendEmail_unconfigured_skipsSending() {
    ReflectionTestUtils.setField(emailService, "mailHost", "");

    EmailMessage message = EmailMessage.builder()
        .to("user@example.com")
        .subject("Test")
        .body("Test")
        .isHtml(false)
        .build();

    boolean result = emailService.sendEmail(message);

    assertFalse(result);
  }

  @Test
  void sendEmail_invalidRecipient_skipsSending() {
    EmailMessage message = EmailMessage.builder()
        .to("invalid-email")
        .subject("Test")
        .body("Test")
        .isHtml(false)
        .build();

    boolean result = emailService.sendEmail(message);

    assertFalse(result);
  }
}
