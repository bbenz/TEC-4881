package org.eclipse.cargotracker.ai;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class DocumentSourceCatalog {

  private final Set<String> sources = ConcurrentHashMap.newKeySet();

  public void add(String source) {
    if (source != null && !source.isBlank()) {
      sources.add(source);
    }
  }

  public boolean isKnown(String source) {
    return source != null
        && (source.equals("tool:cargoStatus")
            || source.equals("tool:listCargos")
            || sources.contains(source));
  }

  public Set<String> all() {
    return Set.copyOf(sources);
  }
}
