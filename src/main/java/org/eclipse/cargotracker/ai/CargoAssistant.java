package org.eclipse.cargotracker.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;

public interface CargoAssistant {

  @SystemMessage(fromResource = "prompts/cargo-assistant.txt")
  @InputGuardrails(StaysOnTopic.class)
  CargoAnswer answer(@UserMessage String question);
}
