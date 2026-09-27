package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class ModelProducer {

  @Inject private AiConfiguration configuration;
  @Inject private Tracer tracer;

  @Produces
  @ApplicationScoped
  ChatModel chatModel() {
    if (!configuration.configured()) {
      return new FallbackChatModel();
    }
    return OpenAiChatModel.builder()
        .baseUrl(configuration.baseUrl())
        .apiKey(configuration.apiKey())
        .modelName(configuration.chatModel())
        .timeout(Duration.ofSeconds(configuration.timeoutSeconds()))
        .maxRetries(1)
        .listeners(new ModelCallTracing(tracer))
        .build();
  }
}
