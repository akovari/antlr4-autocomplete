package akovari.antlr4.autocomplete;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Result of the autocomplete engine.
 */
public final class CompletionResult {
  private final List<InputToken> tokens;
  private final String untokenizedText;
  private final Set<String> suggestions;

  public CompletionResult(List<InputToken> tokens, String untokenizedText, Set<String> suggestions) {
    this.tokens = tokens;
    this.untokenizedText = untokenizedText;
    this.suggestions = suggestions;
  }

  /**
   * List of tokens read from the input
   * @return the list of tokens
   */
  public List<InputToken> getTokens() {
    return tokens;
  }

  /**
   * Returns the piece of input that could not be matched as a token. Either an incomplete token, or an invalid input.
   * @return the not-yet tokenized part of the input
   */
  public String getUntokenizedText() {
    return untokenizedText;
  }

  /**
   * Suggestions for the current input.
   * @return set of suggestions for the provided input
   */
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

  /**
   * Input token with a type of the token
   */
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
