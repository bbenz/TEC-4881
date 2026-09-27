package org.eclipse.cargotracker.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CargoQuestionService {

  @Inject private CargoAssistant assistant;
  @Inject private CargoLookup cargos;
  @Inject private DocumentSourceCatalog sources;
  @Inject private Validator validator;
  @Inject private Logger log;

  public CargoQuestionService() {}

  CargoQuestionService(
      CargoAssistant assistant,
      CargoLookup cargos,
      DocumentSourceCatalog sources,
      Validator validator,
      Logger log) {
    this.assistant = assistant;
    this.cargos = cargos;
    this.sources = sources;
    this.validator = validator;
    this.log = log;
  }

  public CargoAnswer ask(String question) {
    try {
      CargoAnswer reply = assistant.answer(question);
      boolean ok =
          validator.validate(reply).isEmpty()
              && reply.trackingIds().stream().allMatch(cargos::exists)
              && reply.sources().stream().allMatch(sources::isKnown);
      return ok ? reply : CargoAnswer.cannotAnswer();
    } catch (RuntimeException e) {
      // guard failed, timeout, or model down
      log.log(Level.WARNING, "Assistant unavailable", e);
      return CargoAnswer.cannotAnswer();
    }
  }
}
