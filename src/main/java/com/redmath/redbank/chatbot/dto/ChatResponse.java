package com.redmath.redbank.chatbot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatResponse {

  private String reply;
  private boolean needsClarification;

  public ChatResponse(String reply, boolean needsClarification) {
    this.reply = reply;
    this.needsClarification = needsClarification;
  }
}