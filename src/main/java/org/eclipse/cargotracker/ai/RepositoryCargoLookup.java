package org.eclipse.cargotracker.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.CargoRepository;
import org.eclipse.cargotracker.domain.model.cargo.HandlingActivity;
import org.eclipse.cargotracker.domain.model.cargo.Leg;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.location.Location;

@ApplicationScoped
public class RepositoryCargoLookup implements CargoLookup {

  @Inject private CargoRepository cargoRepository;

  @Override
  public Optional<CargoStatusDetails> cargoStatus(String trackingId) {
    Cargo cargo = cargoRepository.find(new TrackingId(trackingId));
    return Optional.ofNullable(cargo).map(this::toDetails);
  }

  @Override
  public List<CargoSummary> listCargos() {
    return cargoRepository.findAll().stream().map(this::toSummary).toList();
  }

  @Override
  public boolean exists(String trackingId) {
    return trackingId != null && cargoRepository.find(new TrackingId(trackingId)) != null;
  }

  @Override
  public Set<String> knownTrackingIds() {
    return cargoRepository.findAll().stream()
        .map(cargo -> cargo.getTrackingId().getIdString())
        .collect(Collectors.toSet());
  }

  private CargoSummary toSummary(Cargo cargo) {
    return new CargoSummary(
        cargo.getTrackingId().getIdString(),
        name(cargo.getOrigin()),
        name(cargo.getRouteSpecification().getDestination()));
  }

  private CargoStatusDetails toDetails(Cargo cargo) {
    return new CargoStatusDetails(
        cargo.getTrackingId().getIdString(),
        name(cargo.getOrigin()),
        name(cargo.getRouteSpecification().getDestination()),
        cargo.getDelivery().getRoutingStatus().name(),
        cargo.getDelivery().getTransportStatus().name(),
        name(cargo.getDelivery().getLastKnownLocation()),
        nextExpectedActivity(cargo.getDelivery().getNextExpectedActivity()),
        cargo.getDelivery().getEstimatedTimeOfArrival() == null
            ? "unknown"
            : cargo
                .getDelivery()
                .getEstimatedTimeOfArrival()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        cargo.getDelivery().isMisdirected(),
        cargo.getItinerary().getLegs().stream().map(this::leg).toList());
  }

  private String name(Location location) {
    return location == null ? "unknown" : location.getName();
  }

  private String nextExpectedActivity(HandlingActivity activity) {
    if (activity == null || activity.isEmpty()) {
      return "none";
    }
    String voyage =
        activity.getVoyage() == null ? "" : " on " + activity.getVoyage().getVoyageNumber();
    return activity.getType() + " at " + name(activity.getLocation()) + voyage;
  }

  private String leg(Leg leg) {
    return name(leg.getLoadLocation())
        + " to "
        + name(leg.getUnloadLocation())
        + " on "
        + leg.getVoyage().getVoyageNumber();
  }
}
