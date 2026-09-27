package org.eclipse.cargotracker.ai;

import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CargoQuestionService {

  @Inject private CargoAssistant assistant;
  @Inject private CargoLookup cargoLookup;
  @Inject private DocumentSourceCatalog sourceCatalog;
  @Inject private Validator validator;
  @Inject private Logger log;

  public CargoQuestionService() {}

  CargoQuestionService(
      CargoAssistant assistant,
      CargoLookup cargoLookup,
      DocumentSourceCatalog sourceCatalog,
      Validator validator,
      Logger log) {
    this.assistant = assistant;
    this.cargoLookup = cargoLookup;
    this.sourceCatalog = sourceCatalog;
    this.validator = validator;
    this.log = log;
  }

  public CargoAnswer ask(String question) {
    try {
      CargoAnswer reply = assistant.answer(question);
      boolean ok =
          validator.validate(reply).isEmpty()
              && reply.trackingIds().stream().allMatch(cargoLookup::isKnownTrackingId)
              && reply.sources().stream().allMatch(sourceCatalog::isKnown);
      if (ok && !reply.trackingIds().isEmpty()) {
        Span.current().setAttribute("cargo.tracking_id", reply.trackingIds().get(0));
      }
      return ok ? reply : CargoAnswer.cannotAnswer();
    } catch (RuntimeException e) {
      log.log(Level.WARNING, "Assistant unavailable", e);
      return CargoAnswer.cannotAnswer();
    }
  }
}
