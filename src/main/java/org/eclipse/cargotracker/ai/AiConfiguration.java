package org.eclipse.cargotracker.ai;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class AiConfiguration {

  public boolean enabled() {
    return config().getOptionalValue("ai.enabled", Boolean.class).orElse(false);
  }

  public String baseUrl() {
    return optional("ai.base-url", "https://api.openai.com/v1");
  }

  public String apiKey() {
    return optional("ai.api-key", "");
  }

  public String chatModel() {
    return optional("ai.chat.model", "gpt-5-mini");
  }

  public int timeoutSeconds() {
    return config().getOptionalValue("ai.timeout-seconds", Integer.class).orElse(10);
  }

  public Path docsDir() {
    return Path.of(optional("ai.docs.dir", "src/main/ai-docs"));
  }

  public boolean configured() {
    return enabled() && !apiKey().isBlank() && !baseUrl().isBlank() && !chatModel().isBlank();
  }

  private String optional(String name, String defaultValue) {
    return config()
        .getOptionalValue(name, String.class)
        .filter(value -> !value.isBlank())
        .orElse(defaultValue);
  }

  private Config config() {
    return ConfigProvider.getConfig();
  }
}
