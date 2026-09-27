package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.inject.Vetoed;

@Vetoed
public class ModelCallTracing implements ChatModelListener {

  private static final String SPAN_KEY = ModelCallTracing.class.getName() + ".span";
  private final Tracer tracer = GlobalOpenTelemetry.getTracer("org.eclipse.cargotracker.ai");

  @Override
  public void onRequest(ChatModelRequestContext context) {
    Span span = tracer.spanBuilder("gen_ai.chat").startSpan();
    span.setAttribute("gen_ai.operation.name", "chat");
    span.setAttribute("gen_ai.provider.name", providerName(context.modelProvider()));
    String modelName = context.chatRequest().modelName();
    if (modelName != null) {
      span.setAttribute("gen_ai.request.model", modelName);
    }
    context.attributes().put(SPAN_KEY, span);
  }

  @Override
  public void onResponse(ChatModelResponseContext context) {
    Span span = span(context.attributes().remove(SPAN_KEY));
    if (span == null) {
      return;
    }
    try {
      if (context.chatResponse().modelName() != null) {
        span.setAttribute("gen_ai.response.model", context.chatResponse().modelName());
      }
      TokenUsage usage = context.chatResponse().tokenUsage();
      if (usage != null) {
        setLong(span, "gen_ai.usage.input_tokens", usage.inputTokenCount());
        setLong(span, "gen_ai.usage.output_tokens", usage.outputTokenCount());
      }
      if (context.chatResponse().finishReason() != null) {
        span.setAttribute(
            "gen_ai.response.finish_reasons", context.chatResponse().finishReason().name());
      }
    } finally {
      span.end();
    }
  }

  @Override
  public void onError(ChatModelErrorContext context) {
    Span span = span(context.attributes().remove(SPAN_KEY));
    if (span == null) {
      return;
    }
    try {
      span.recordException(context.error());
      span.setStatus(StatusCode.ERROR, context.error().getMessage());
    } finally {
      span.end();
    }
  }

  private void setLong(Span span, String name, Integer value) {
    if (value != null) {
      span.setAttribute(name, value.longValue());
    }
  }

  private Span span(Object value) {
    return value instanceof Span span ? span : null;
  }

  private String providerName(ModelProvider provider) {
    return provider == null ? "openai" : provider.name().toLowerCase().replace('_', '-');
  }
}
