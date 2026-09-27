package org.eclipse.cargotracker.ai;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class CargoTools {

  @Inject private CargoLookup cargoLookup;

  public CargoTools() {}

  public CargoTools(CargoLookup cargoLookup) {
    this.cargoLookup = cargoLookup;
  }

  @Tool(
      "Return cargo status, route, last known location, next expected activity, ETA, and misdirected flag for a tracking ID")
  public CargoStatusDetails cargoStatus(String trackingId) {
    return cargoLookup.cargoStatus(trackingId).orElse(null);
  }

  @Tool("List all cargo tracking IDs with origin and destination")
  public List<CargoSummary> listCargos() {
    return cargoLookup.listCargos();
  }
}
