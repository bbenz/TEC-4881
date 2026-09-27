package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

class ThrowingChatModel implements ChatModel {

  @Override
  public ChatResponse doChat(ChatRequest chatRequest) {
    throw new AssertionError("The input guardrail should run before the model.");
  }
}
