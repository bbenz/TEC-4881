package org.eclipse.cargotracker.ai;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
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
      EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
      EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
      List<Document> documents =
          FileSystemDocumentLoader.loadDocumentsRecursively(configuration.docsDir());
      documents.forEach(
          document -> sourceCatalog.add(document.metadata().getString(Document.FILE_NAME)));
      logger.info("Indexed " + documents.size() + " AI sample documents.");

      // Indexing: at startup
      EmbeddingStoreIngestor.builder()
          .documentSplitter(DocumentSplitters.recursive(500, 50))
          .embeddingModel(embeddingModel)
          .embeddingStore(store)
          .build()
          .ingest(documents);

      // Retrieval: for every question
      ContentRetriever retriever =
          EmbeddingStoreContentRetriever.builder()
              .embeddingStore(store)
              .embeddingModel(embeddingModel)
              .maxResults(5)
              .minScore(0.7)
              .build();
      return new TracingContentRetriever(retriever, tracer);
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "AI document search disabled", e);
      return query -> List.of();
    }
  }
}
