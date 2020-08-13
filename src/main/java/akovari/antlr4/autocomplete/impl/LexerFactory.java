package akovari.antlr4.autocomplete.impl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public interface LexerFactory {
  Lexer createLexer(CharStream input);

  boolean isValidSuggestion(String suggestion);
}
