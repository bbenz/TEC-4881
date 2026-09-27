package org.eclipse.cargotracker.ai;

import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("assistant")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AssistantResource {

  @Inject private CargoQuestionService questionService;

  @POST
  public CargoAnswer ask(@Valid AssistantQuestion question) {
    CargoAnswer answer = questionService.ask(question.question());
    if (!answer.trackingIds().isEmpty()) {
      Span.current().setAttribute("cargo.tracking_id", answer.trackingIds().get(0));
    }
    return answer;
  }

  public record AssistantQuestion(@NotBlank String question) {}
}
