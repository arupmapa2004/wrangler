package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.cdap.wrangler.api.annotations.PublicEvolving;

@PublicEvolving
public class ByteSize implements Token {
  private final String original;
  private final long bytes;

  public ByteSize(String size) {
    this.original = size.trim().toUpperCase();
    this.bytes = parseBytes(original);
  }

  private long parseBytes(String input) {
    if (input.matches("\\d+[KMGTP]?B?")) {
      long multiplier = 1;
      String numberPart = input.replaceAll("[^0-9]", "");
      String unitPart = input.replaceAll("[0-9]", "").replace("B", "");

      switch (unitPart) {
        case "K":
          multiplier = 1024L;
          break;
        case "M":
          multiplier = 1024L * 1024;
          break;
        case "G":
          multiplier = 1024L * 1024 * 1024;
          break;
        case "T":
          multiplier = 1024L * 1024 * 1024 * 1024;
          break;
        case "":
          multiplier = 1;
          break;
        default:
          throw new IllegalArgumentException("Unsupported byte unit: " + unitPart);
      }
      return Long.parseLong(numberPart) * multiplier;
    }
    throw new IllegalArgumentException("Invalid byte size format: " + input);
  }

  public long getBytes() {
    return bytes;
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(bytes);
  }

  @Override
  public String toString() {
    return original;
  }
}
