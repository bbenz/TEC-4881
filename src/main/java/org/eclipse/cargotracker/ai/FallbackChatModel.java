package org.eclipse.cargotracker.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import jakarta.enterprise.inject.Vetoed;

@Vetoed
class FallbackChatModel implements ChatModel {

  static final String JSON =
      "{\"answer\":\"I can't answer that.\",\"trackingIds\":[],\"sources\":[]}";

  @Override
  public ChatResponse doChat(ChatRequest chatRequest) {
    return ChatResponse.builder()
        .aiMessage(AiMessage.from(JSON))
        .modelName("fallback")
        .finishReason(FinishReason.STOP)
        .build();
  }

  @Override
  public ModelProvider provider() {
    return ModelProvider.OTHER;
  }
}
