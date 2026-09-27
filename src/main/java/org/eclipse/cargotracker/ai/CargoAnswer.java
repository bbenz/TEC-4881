package org.eclipse.cargotracker.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// spotless:off
public record CargoAnswer(
    @NotBlank String answer,
    @NotNull List<String> trackingIds,
    @NotEmpty List<String> sources) {

  static CargoAnswer cannotAnswer() {
    return new CargoAnswer("I can't answer that.", List.of(), List.of());
  }
}
// spotless:on
