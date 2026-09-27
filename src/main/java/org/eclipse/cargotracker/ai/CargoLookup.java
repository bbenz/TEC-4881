package org.eclipse.cargotracker.ai;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CargoLookup {

  Optional<CargoStatusDetails> cargoStatus(String trackingId);

  List<CargoSummary> listCargos();

  boolean exists(String trackingId);

  Set<String> knownTrackingIds();
}
