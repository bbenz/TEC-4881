package org.eclipse.cargotracker.ai;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class CargoQuestionServiceTest {

  private final FakeCargoLookup cargoLookup = new FakeCargoLookup();
  private final DocumentSourceCatalog sourceCatalog = new DocumentSourceCatalog();
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final Logger logger = Logger.getLogger(getClass().getName());

  @Test
  void returnsFallbackWhenAssistantThrows() {
    CargoQuestionService service =
        service(
            question -> {
              throw new IllegalStateException("down");
            });

    CargoAnswer answer = service.ask("Where is ABC123?");

    assertThat(answer).isEqualTo(CargoAnswer.cannotAnswer());
  }

  @Test
  void returnsFallbackForInvalidRecord() {
    CargoQuestionService service =
        service(question -> new CargoAnswer("", List.of("ABC123"), List.of("tool:cargoStatus")));

    CargoAnswer answer = service.ask("Where is ABC123?");

    assertThat(answer).isEqualTo(CargoAnswer.cannotAnswer());
  }

  @Test
  void returnsFallbackForUnknownTrackingId() {
    CargoQuestionService service =
        service(
            question ->
                new CargoAnswer("Found it", List.of("ZZZ999"), List.of("tool:cargoStatus")));

    CargoAnswer answer = service.ask("Where is ZZZ999?");

    assertThat(answer).isEqualTo(CargoAnswer.cannotAnswer());
  }

  @Test
  void acceptsKnownTrackingIdAndSource() {
    sourceCatalog.add("customs-holds.md");
    CargoQuestionService service =
        service(
            question ->
                new CargoAnswer(
                    "ABC123 is in New York.", List.of("ABC123"), List.of("customs-holds.md")));

    CargoAnswer answer = service.ask("Where is ABC123?");

    assertThat(answer.answer()).contains("ABC123");
  }

  private CargoQuestionService service(CargoAssistant assistant) {
    return new CargoQuestionService(assistant, cargoLookup, sourceCatalog, validator, logger);
  }
}
