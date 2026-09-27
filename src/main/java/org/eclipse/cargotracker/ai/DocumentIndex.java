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
import java.nio.file.Path;
import java.util.List;

public final class DocumentIndex {

  private DocumentIndex() {}

  public static ContentRetriever build(Path docsDir, DocumentSourceCatalog catalog, Tracer tracer) {
    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
    List<Document> documents = FileSystemDocumentLoader.loadDocumentsRecursively(docsDir);
    documents.forEach(document -> catalog.add(document.metadata().getString(Document.FILE_NAME)));

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
  }
}
