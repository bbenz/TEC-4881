package org.eclipse.cargotracker.ai;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.util.List;

class TracingContentRetriever implements ContentRetriever {

  private final ContentRetriever delegate;
  private final Tracer tracer;

  TracingContentRetriever(ContentRetriever delegate, Tracer tracer) {
    this.delegate = delegate;
    this.tracer = tracer;
  }

  @Override
  public List<Content> retrieve(Query query) {
    Span span = tracer.spanBuilder("retrieve documents").startSpan();
    span.setAttribute("gen_ai.operation.name", "retrieve_documents");
    try {
      List<Content> results = delegate.retrieve(query);
      span.setAttribute("gen_ai.retrieved_documents.count", results.size());
      return results;
    } catch (RuntimeException e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }
}
