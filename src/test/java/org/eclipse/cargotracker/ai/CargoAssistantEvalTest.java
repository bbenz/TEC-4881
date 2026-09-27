package org.eclipse.cargotracker.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CargoAssistantEvalTest {

  private static final int MAX_TOKENS_PER_ANSWER = 2_500;
  private static final long P95_LATENCY_LIMIT_MILLIS = 4_000;

  private CargoAssistant guardedAssistant;
  private DocumentSourceCatalog sourceCatalog;
  private List<EvalResult> modelResults = List.of();
  private boolean modelConfigured;

  @BeforeAll
  void runModelEvalOnce() {
    modelConfigured = isModelConfigured();
    if (!modelConfigured) {
      System.out.println("AI model not configured; skipping model-backed evals.");
      return;
    }

    sourceCatalog = new DocumentSourceCatalog();
    EvalTokenCounter tokenCounter = new EvalTokenCounter();
    CargoAssistant modelAssistant = configuredAssistant(tokenCounter);
    List<EvalResult> results = new ArrayList<>();
    for (EvalCase evalCase : EvalCase.load("/eval/cargo-questions.csv")) {
      tokenCounter.reset();
      long started = System.nanoTime();
      CargoAnswer answer = CargoAnswer.cannotAnswer();
      String error = null;
      try {
        answer = modelAssistant.answer(evalCase.question());
      } catch (RuntimeException e) {
        error = e.getMessage();
      }
      long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
      results.add(
          new EvalResult(evalCase, answer, elapsedMillis, tokenCounter.totalTokens(), error));
    }
    modelResults = List.copyOf(results);
  }

  @BeforeEach
  void setUp() {
    guardedAssistant =
        AssistantFactory.create(
            new ThrowingChatModel(), query -> List.of(), new CargoTools(new FakeCargoLookup()));
  }

  @Test
  void findsTheRightCargo() {
    assumeModelConfigured();

    long correct = modelResults.stream().filter(EvalResult::foundExpectedId).count();

    assertThat(correct).isGreaterThanOrEqualTo(38);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/eval/off-topic.csv")
  void refusesOffTopicQuestions(String question) {
    assertThatThrownBy(() -> guardedAssistant.answer(question))
        .isInstanceOf(InputGuardrailException.class);
  }

  @Test
  void citesRealSources() {
    assumeModelConfigured();

    assertThat(modelResults)
        .allSatisfy(
            result -> {
              assertThat(result.answer().sources()).isNotEmpty();
              assertThat(result.answer().sources()).allMatch(sourceCatalog::isKnown);
            });
  }

  @Test
  void returnsValidData() {
    assumeModelConfigured();
    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    assertThat(modelResults)
        .allSatisfy(result -> assertThat(validator.validate(result.answer())).isEmpty());
  }

  @Test
  void answersInTime() {
    assumeModelConfigured();

    long p95LatencyMillis =
        modelResults.stream()
            .map(EvalResult::latencyMillis)
            .sorted()
            .toList()
            .get(percentileIndex(modelResults.size(), 0.95));

    assertThat(p95LatencyMillis).isLessThan(P95_LATENCY_LIMIT_MILLIS);
  }

  @Test
  void costsWhatWeExpect() {
    assumeModelConfigured();

    assertThat(modelResults)
        .allSatisfy(result -> assertThat(result.tokens()).isLessThan(MAX_TOKENS_PER_ANSWER));
  }

  private void assumeModelConfigured() {
    assumeTrue(modelConfigured, "AI model not configured");
  }

  private boolean isModelConfigured() {
    return System.getenv("AI_API_KEY") != null || System.getenv("AI_BASE_URL") != null;
  }

  private int percentileIndex(int size, double percentile) {
    return Math.max(0, (int) Math.ceil(size * percentile) - 1);
  }

  private CargoAssistant configuredAssistant(EvalTokenCounter tokenCounter) {
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
            .listeners(tokenCounter)
            .build();
    ContentRetriever docs =
        DocumentIndex.build(
            Path.of("src/main/ai-docs"), sourceCatalog, OpenTelemetry.noop().getTracer("eval"));
    return AssistantFactory.create(chatModel, docs, new CargoTools(new FakeCargoLookup()));
  }

  private record EvalResult(
      EvalCase evalCase, CargoAnswer answer, long latencyMillis, int tokens, String error) {

    boolean foundExpectedId() {
      return answer.trackingIds().contains(evalCase.expectedId());
    }
  }

  private static final class EvalTokenCounter implements ChatModelListener {

    private int totalTokens;

    void reset() {
      totalTokens = 0;
    }

    int totalTokens() {
      return totalTokens;
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
      TokenUsage tokenUsage = context.chatResponse().tokenUsage();
      if (tokenUsage == null) {
        return;
      }
      totalTokens += value(tokenUsage.inputTokenCount());
      totalTokens += value(tokenUsage.outputTokenCount());
    }

    private int value(Integer tokenCount) {
      return tokenCount == null ? 0 : tokenCount;
    }
  }
}
