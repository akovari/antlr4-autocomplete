package akovari.antlr4.autocomplete;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

public class ReflectionLexerAndParserFactory implements LexerAndParserFactory {
  private final Constructor<? extends Lexer> lexerCtr;
  private final Constructor<? extends Parser> parserCtr;
  private final Function<String, Boolean> isValidState;

  public ReflectionLexerAndParserFactory(Class<? extends Lexer> lexerClass, Class<? extends Parser> parserClass) {
    this(lexerClass, parserClass, (suggestion) -> true);
  }

  public ReflectionLexerAndParserFactory(Class<? extends Lexer> lexerClass, Class<? extends Parser> parserClass, Function<String, Boolean> isValidState) {
    lexerCtr = getConstructor(lexerClass, CharStream.class);
    parserCtr = getConstructor(parserClass, TokenStream.class);
    this.isValidState = isValidState;
  }

  @Override
  public Lexer createLexer(CharStream input) {
    return create(lexerCtr, input);
  }

  @Override
  public boolean isValidSuggestion(String suggestion) {
    return isValidState.apply(suggestion);
  }

  @Override
  public Parser createParser(TokenStream tokenStream) {
    return create(parserCtr, tokenStream);
  }

  private static <T> Constructor<? extends T> getConstructor(Class<? extends T> givenClass, Class<?> argClass) {
    try {
      return givenClass.getConstructor(argClass);
    } catch (NoSuchMethodException | SecurityException e) {
      throw new IllegalArgumentException(
          givenClass.getSimpleName() + " must have constructor from " + argClass.getSimpleName() + ".");
    }
  }

  private <T> T create(Constructor<? extends T> contructor, Object arg) {
    try {
      return contructor.newInstance(arg);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
