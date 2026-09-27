package org.eclipse.cargotracker.ai;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class StaysOnTopic implements InputGuardrail {

  private static final int MAX_LENGTH = 500;
  private static final Pattern TRACKING_ID = Pattern.compile("\\b[A-Z]{3}\\d{3}\\b");
  private static final List<String> SHIPPING_WORDS =
      List.of(
          "cargo",
          "shipment",
          "shipping",
          "ship",
          "route",
          "routing",
          "delivery",
          "deliver",
          "origin",
          "destination",
          "port",
          "customs",
          "hold",
          "tracking",
          "track",
          "voyage",
          "carrier",
          "eta",
          "delay",
          "reroute",
          "claim",
          "hazardous",
          "freight",
          "container");
  private static final List<String> INJECTION_PHRASES =
      List.of(
          "ignore previous",
          "ignore all previous",
          "system prompt",
          "developer message",
          "reveal",
          "show me your instructions",
          "bypass",
          "jailbreak",
          "act as",
          "forget the rules");

  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    String question = userMessage.singleText() == null ? "" : userMessage.singleText().trim();
    String lower = question.toLowerCase(Locale.ROOT);

    if (question.isBlank()) {
      return fatal("Ask a cargo or shipping question.");
    }
    if (question.length() > MAX_LENGTH) {
      return fatal("Questions must be 500 characters or less.");
    }
    if (INJECTION_PHRASES.stream().anyMatch(lower::contains)) {
      return fatal("Prompt-injection requests are not allowed.");
    }
    boolean cargoTopic =
        TRACKING_ID.matcher(question).find() || SHIPPING_WORDS.stream().anyMatch(lower::contains);
    if (!cargoTopic) {
      return fatal("Ask about cargo, shipping, routes, documents, or tracking IDs.");
    }
    return success();
  }
}
