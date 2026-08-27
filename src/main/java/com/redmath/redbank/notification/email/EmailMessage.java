package com.redmath.redbank.notification.email;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailMessage {

  private final String to;
  private final String subject;
  private final String body;
  private final boolean isHtml;
}
