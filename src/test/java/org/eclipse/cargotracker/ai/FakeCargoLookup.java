package org.eclipse.cargotracker.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class FakeCargoLookup implements CargoLookup {

  private final Map<String, CargoStatusDetails> cargos;

  FakeCargoLookup() {
    cargos =
        Map.of(
            "ABC123",
            details(
                "ABC123",
                "Hong Kong",
                "Helsinki",
                "ROUTED",
                "IN_PORT",
                "New York",
                "LOAD at New York",
                false),
            "JKL567",
            details(
                "JKL567",
                "Hangzou",
                "Stockholm",
                "MISROUTED",
                "ONBOARD_CARRIER",
                "New York",
                "none",
                true),
            "DEF789",
            details(
                "DEF789",
                "Hong Kong",
                "Melbourne",
                "NOT_ROUTED",
                "NOT_RECEIVED",
                "Unknown location",
                "none",
                false),
            "MNO456",
            details("MNO456", "New York", "Dallas", "ROUTED", "CLAIMED", "Dallas", "none", false));
  }

  @Override
  public Optional<CargoStatusDetails> cargoStatus(String trackingId) {
    return Optional.ofNullable(cargos.get(trackingId));
  }

  @Override
  public List<CargoSummary> listCargos() {
    return cargos.values().stream()
        .map(cargo -> new CargoSummary(cargo.trackingId(), cargo.origin(), cargo.destination()))
        .toList();
  }

  @Override
  public boolean exists(String trackingId) {
    return cargos.containsKey(trackingId);
  }

  @Override
  public Set<String> knownTrackingIds() {
    return cargos.keySet();
  }

  private CargoStatusDetails details(
      String trackingId,
      String origin,
      String destination,
      String routingStatus,
      String transportStatus,
      String lastKnownLocation,
      String nextExpectedActivity,
      boolean misdirected) {
    return new CargoStatusDetails(
        trackingId,
        origin,
        destination,
        routingStatus,
        transportStatus,
        lastKnownLocation,
        nextExpectedActivity,
        "unknown",
        misdirected,
        List.of(origin + " to " + destination));
  }
}
