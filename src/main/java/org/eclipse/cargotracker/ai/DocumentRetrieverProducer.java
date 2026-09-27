package org.eclipse.cargotracker.ai;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DocumentRetrieverProducer {

  @Inject private AiConfiguration configuration;
  @Inject private DocumentSourceCatalog sourceCatalog;
  @Inject private Tracer tracer;
  @Inject private Logger logger;

  @Produces
  @ApplicationScoped
  ContentRetriever contentRetriever() {
    if (!Files.isDirectory(configuration.docsDir())) {
      logger.info("AI document directory not found: " + configuration.docsDir());
      return query -> List.of();
    }

    try {
      ContentRetriever retriever =
          DocumentIndex.build(configuration.docsDir(), sourceCatalog, tracer);
      logger.info("Indexed " + sourceCatalog.all().size() + " AI sample documents.");
      return retriever;
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "AI document search disabled", e);
      return query -> List.of();
    }
  }
}
