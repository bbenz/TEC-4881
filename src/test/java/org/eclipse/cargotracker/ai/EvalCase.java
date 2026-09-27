package org.eclipse.cargotracker.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

record EvalCase(String question, String expectedId) {

  static List<EvalCase> load(String resource) {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(EvalCase.class.getResourceAsStream(resource)),
                StandardCharsets.UTF_8))) {
      return reader.lines().skip(1).filter(line -> !line.isBlank()).map(EvalCase::parse).toList();
    } catch (Exception e) {
      throw new IllegalStateException("Could not load " + resource, e);
    }
  }

  private static EvalCase parse(String line) {
    int comma = line.lastIndexOf(',');
    String question = unquote(line.substring(0, comma));
    String expected = unquote(line.substring(comma + 1));
    return new EvalCase(question, expected);
  }

  private static String unquote(String value) {
    value = value.trim();
    if (value.startsWith("\"") && value.endsWith("\"")) {
      return value.substring(1, value.length() - 1).replace("\"\"", "\"");
    }
    return value;
  }
}
