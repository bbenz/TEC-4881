package org.eclipse.cargotracker.ai;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CargoAnswerValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void validAnswerPassesValidation() {
    assertThat(validator.validate(new CargoAnswer("Answer", List.of(), List.of("tool:listCargos"))))
        .isEmpty();
  }

  @Test
  void fallbackDoesNotPassValidationBecauseItHasNoSource() {
    assertThat(validator.validate(CargoAnswer.cannotAnswer())).isNotEmpty();
  }
}
