package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class TimeDuration implements Token {
  private final String original;
  private final long millis;

  public TimeDuration(String duration) {
    this.original = duration.trim().toLowerCase();
    this.millis = parseMillis(original);
  }

  private long parseMillis(String input) {
    if (input.matches("\\d+[smhd]")) {
      long multiplier = 1;
      String numberPart = input.replaceAll("[^0-9]", "");
      String unitPart = input.replaceAll("[0-9]", "");

      switch (unitPart) {
        case "s":
          multiplier = 1000L;
          break;
        case "m":
          multiplier = 60L * 1000;
          break;
        case "h":
          multiplier = 60L * 60 * 1000;
          break;
        case "d":
          multiplier = 24L * 60 * 60 * 1000;
          break;
        default:
          throw new IllegalArgumentException("Unsupported time unit: " + unitPart);
      }

      return Long.parseLong(numberPart) * multiplier;
    }
    throw new IllegalArgumentException("Invalid time duration format: " + input);
  }

  public long getMillis() {
    return millis;
  }

  @Override
  public Object value() {
    return millis;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(millis);
  }

  @Override
  public String toString() {
    return original;
  }
}
