package akovari.antlr4.autocomplete.impl;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

public interface ParserFactory {
  Parser createParser(TokenStream tokenStream);
}
