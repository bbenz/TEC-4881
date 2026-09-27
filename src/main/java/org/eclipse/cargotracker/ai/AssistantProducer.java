package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AssistantProducer {

  @Produces
  @ApplicationScoped
  CargoAssistant assistant(ChatModel model, ContentRetriever docs, CargoTools tools) {
    return AiServices.builder(CargoAssistant.class)
        .chatModel(model)
        .contentRetriever(docs)
        .tools(tools)
        .build();
  }
}
