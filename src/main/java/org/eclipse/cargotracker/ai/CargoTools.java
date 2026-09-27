package org.eclipse.cargotracker.ai;

import dev.langchain4j.agent.tool.Tool;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class CargoTools {

  @Inject private CargoLookup cargoLookup;
  @Inject private Tracer tracer;

  public CargoTools() {}

  public CargoTools(CargoLookup cargoLookup) {
    this(cargoLookup, OpenTelemetry.noop().getTracer("test"));
  }

  CargoTools(CargoLookup cargoLookup, Tracer tracer) {
    this.cargoLookup = cargoLookup;
    this.tracer = tracer;
  }

  @Tool(
      "Return cargo status, route, last known location, next expected activity, ETA, and misdirected flag for a tracking ID")
  public CargoStatusDetails cargoStatus(String trackingId) {
    Span span = toolSpan("cargoStatus");
    try {
      return cargoLookup.cargoStatus(trackingId).orElse(null);
    } finally {
      span.end();
    }
  }

  @Tool("List all cargo tracking IDs with origin and destination")
  public List<CargoSummary> listCargos() {
    Span span = toolSpan("listCargos");
    try {
      return cargoLookup.listCargos();
    } finally {
      span.end();
    }
  }

  private Span toolSpan(String toolName) {
    Span span = tracer.spanBuilder("execute_tool " + toolName).startSpan();
    span.setAttribute("gen_ai.operation.name", "execute_tool");
    span.setAttribute("gen_ai.tool.name", toolName);
    return span;
  }
}
