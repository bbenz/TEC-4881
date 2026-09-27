package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;

public final class AssistantFactory {

  private AssistantFactory() {}

  public static CargoAssistant create(ChatModel model, ContentRetriever docs, CargoTools tools) {
    return AiServices.builder(CargoAssistant.class)
        .chatModel(model)
        .contentRetriever(docs)
        .tools(tools)
        .build();
  }
}
