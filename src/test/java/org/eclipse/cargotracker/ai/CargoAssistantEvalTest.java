package org.eclipse.cargotracker.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class CargoAssistantEvalTest {

  private CargoAssistant assistant;

  @BeforeEach
  void setUp() {
    assistant =
        AssistantFactory.create(
            new ThrowingChatModel(), query -> List.of(), new CargoTools(new FakeCargoLookup()));
  }

  @Test
  void findsTheRightCargo() {
    assumeTrue(System.getenv("AI_API_KEY") != null || System.getenv("AI_BASE_URL") != null);
    List<EvalCase> cases = EvalCase.load("/eval/cargo-questions.csv");
    CargoAssistant assistant = configuredAssistant();

    long correct =
        cases.stream()
            .filter(c -> assistant.answer(c.question()).trackingIds().contains(c.expectedId()))
            .count();

    assertThat(correct).isGreaterThanOrEqualTo(38);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/eval/off-topic.csv")
  void refusesOffTopicQuestions(String question) {
    assertThatThrownBy(() -> assistant.answer(question))
        .isInstanceOf(InputGuardrailException.class);
  }

  private CargoAssistant configuredAssistant() {
    String baseUrl = System.getenv().getOrDefault("AI_BASE_URL", "https://api.openai.com/v1");
    String apiKey = System.getenv().getOrDefault("AI_API_KEY", "not-used");
    String model = System.getenv().getOrDefault("AI_CHAT_MODEL", "gpt-5-mini");
    ChatModel chatModel =
        OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(model)
            .timeout(Duration.ofSeconds(10))
            .maxRetries(1)
            .build();
    ContentRetriever docs = query -> List.of();
    return AssistantFactory.create(chatModel, docs, new CargoTools(new FakeCargoLookup()));
  }
}
