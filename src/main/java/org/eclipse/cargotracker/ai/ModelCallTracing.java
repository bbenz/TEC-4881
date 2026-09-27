package org.eclipse.cargotracker.ai;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

public class ModelCallTracing implements ChatModelListener {

  private static final String SPAN_KEY = ModelCallTracing.class.getName() + ".span";

  private final Tracer tracer;

  public ModelCallTracing(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public void onRequest(ChatModelRequestContext context) {
    String modelName = context.chatRequest().modelName();
    String spanName = modelName == null ? "chat" : "chat " + modelName;
    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();
    span.setAttribute("gen_ai.operation.name", "chat");
    span.setAttribute("gen_ai.provider.name", providerName(context.modelProvider()));
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
            AttributeKey.stringArrayKey("gen_ai.response.finish_reasons"),
            java.util.List.of(context.chatResponse().finishReason().name()));
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
      span.setAttribute("otel.status_code", "ERROR");
      span.setAttribute("error.type", context.error().getClass().getName());
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
    if (provider == ModelProvider.OPEN_AI) {
      return "openai";
    }
    if (provider == ModelProvider.AZURE_OPEN_AI) {
      return "azure.ai.openai";
    }
    return provider == null ? "openai" : provider.name().toLowerCase();
  }
}
