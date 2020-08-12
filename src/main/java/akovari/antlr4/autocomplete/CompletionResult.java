package akovari.antlr4.autocomplete;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CompletionResult {
  private final List<InputToken> tokens;
  private final String untokenizedText;
  private final Set<String> suggestions;

  public CompletionResult(List<InputToken> tokens, String untokenizedText, Set<String> suggestions) {
    this.tokens = tokens;
    this.untokenizedText = untokenizedText;
    this.suggestions = suggestions;
  }

  public List<InputToken> getTokens() {
    return tokens;
  }

  public String getUntokenizedText() {
    return untokenizedText;
  }

  public Set<String> getSuggestions() {
    return suggestions;
  }

  @Override
  public String toString() {
    return "CompletionResult{" +
        "tokens=" + tokens +
        ", untokenizedText='" + untokenizedText + '\'' +
        ", suggestions=" + suggestions +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CompletionResult that = (CompletionResult) o;
    return Objects.equals(tokens, that.tokens) &&
        Objects.equals(untokenizedText, that.untokenizedText) &&
        Objects.equals(suggestions, that.suggestions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokens, untokenizedText, suggestions);
  }

  public static final class InputToken {
    private final String type;
    private final String text;

    public InputToken(String type, String text) {
      this.type = type;
      this.text = text;
    }

    public String getType() {
      return type;
    }

    public String getText() {
      return text;
    }

    @Override
    public String toString() {
      return "InputToken{" +
          "type='" + type + '\'' +
          ", text='" + text + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      InputToken that = (InputToken) o;
      return Objects.equals(type, that.type) &&
          Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, text);
    }
  }
}
