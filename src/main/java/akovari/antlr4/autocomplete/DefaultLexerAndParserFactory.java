package akovari.antlr4.autocomplete;

import akovari.antlr4.autocomplete.impl.LexerAndParserFactory;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Callback based factory for both, Lexer and Parser.
 * @param <L>
 * @param <P>
 */
public class DefaultLexerAndParserFactory<L extends Lexer, P extends Parser> implements LexerAndParserFactory {
  private final Predicate<String> isValidState;
  private final Function<CharStream, L> lexerCtor;
  private final Function<TokenStream, P> parserCtor;

  /**
   * @param lexerCtor - Constructor for the Antlr generated lexer
   * @param parserCtor - Constructor for the Antlr generated parser
   */
  public DefaultLexerAndParserFactory(Function<CharStream, L> lexerCtor, Function<TokenStream, P> parserCtor) {
    this(lexerCtor, parserCtor, (suggestion) -> true);
  }

  /**
   * @param lexerCtor - Constructor for the Antlr generated lexer
   * @param parserCtor - Constructor for the Antlr generated parser
   * @param isValidState predicate testing whether a suggested state should be included in the results. State names are used, as they are defined in the lexer grammar.
   */
  public DefaultLexerAndParserFactory(Function<CharStream, L> lexerCtor, Function<TokenStream, P> parserCtor, Predicate<String> isValidState) {
    this.isValidState = isValidState;
    this.lexerCtor = lexerCtor;
    this.parserCtor = parserCtor;
  }

  @Override
  public Lexer createLexer(CharStream input) {
    return lexerCtor.apply(input);
  }

  @Override
  public boolean isValidSuggestion(String suggestion) {
    return isValidState.test(suggestion);
  }

  @Override
  public Parser createParser(TokenStream tokenStream) {
    return parserCtor.apply(tokenStream);
  }
}
