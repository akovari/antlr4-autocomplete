package akovari.antlr4.autocomplete;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.Interval;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Suggests completions for given text, using a given ANTLR4 grammar.
 */
public class Antlr4Completer {
  private static final Logger logger = Logger.getLogger(Antlr4Completer.class.getName());

  private final ParserWrapper parserWrapper;
  private final LexerWrapper lexerWrapper;
  private final String input;

  private final Map<ATNState, Integer> parserStateToTokenListIndexWhereLastVisited = new HashMap<>();

  public Antlr4Completer(LexerAndParserFactory lexerAndParserFactory, String input) {
    this.lexerWrapper = new LexerWrapper(lexerAndParserFactory);
    this.parserWrapper = new ParserWrapper(lexerAndParserFactory, lexerWrapper.getVocabulary());
    this.input = input;
  }

  public CompletionResult complete() {
    // TODO filter suggestion from line
    return runParserAtnAndCollectSuggestions(lexerWrapper.tokenizeNonDefaultChannel(this.input));
  }

  private CompletionResult runParserAtnAndCollectSuggestions(LexerWrapper.TokenizationResult tokenizationResult) {
    ATNState initialState = this.parserWrapper.getAtnState(0);
    logger.fine("Parser initial state: " + initialState);
    List<CompletionResult.InputToken> tokens = tokenizationResult.tokens
        .stream()
        .map(token -> new CompletionResult.InputToken(lexerWrapper.getVocabulary().getDisplayName(token.getType()), token.getText()))
        .collect(Collectors.toUnmodifiableList());
    String untokenizedText = tokenizationResult.untokenizedText;
    return new CompletionResult(tokens, untokenizedText, parseAndCollectTokenSuggestions(initialState, tokenizationResult, 0));
  }

  /**
   * Recursive through the parser ATN to process all tokens. When successful (out of tokens) - collect completion
   * suggestions.
   */
  private Set<String> parseAndCollectTokenSuggestions(ATNState parserState, LexerWrapper.TokenizationResult tokenizationResult, int tokenListIndex) {
    Set<String> candidates = new HashSet<>();
    if (didVisitParserStateOnThisTokenIndex(parserState, tokenListIndex)) {
      logger.fine("State " + parserState + " had already been visited while processing token "
          + tokenListIndex + ", backtracking to avoid infinite loop.");
      return candidates;
    }
    Integer previousTokenListIndexForThisState = setParserStateLastVisitedOnThisTokenIndex(parserState, tokenListIndex);
    try {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("State: " + parserWrapper.toString(parserState));
        logger.fine("State available transitions: " + parserWrapper.transitionsStr(parserState));
      }

      if (!haveMoreTokens(tokenizationResult, tokenListIndex)) { // stop condition for recursion
        candidates.addAll(suggestNextTokensForParserState(parserState, tokenizationResult));
        return candidates;
      }
      for (Transition trans : parserState.getTransitions()) {
        if (trans.isEpsilon()) {
          candidates.addAll(handleEpsilonTransition(trans, tokenizationResult, tokenListIndex));
        } else if (trans instanceof AtomTransition) {
          candidates.addAll(handleAtomicTransition((AtomTransition) trans, tokenizationResult, tokenListIndex));
        } else {
          candidates.addAll(handleSetTransition((SetTransition) trans, tokenizationResult, tokenListIndex));
        }
      }
    } finally {
      setParserStateLastVisitedOnThisTokenIndex(parserState, previousTokenListIndexForThisState);
    }
    return candidates;
  }

  private boolean didVisitParserStateOnThisTokenIndex(ATNState parserState, Integer currentTokenListIndex) {
    Integer lastVisitedThisStateAtTokenListIndex = parserStateToTokenListIndexWhereLastVisited.get(parserState);
    return currentTokenListIndex.equals(lastVisitedThisStateAtTokenListIndex);
  }

  private Integer setParserStateLastVisitedOnThisTokenIndex(ATNState parserState, Integer tokenListIndex) {
    if (tokenListIndex == null) {
      return parserStateToTokenListIndexWhereLastVisited.remove(parserState);
    } else {
      return parserStateToTokenListIndexWhereLastVisited.put(parserState, tokenListIndex);
    }
  }

  private boolean haveMoreTokens(LexerWrapper.TokenizationResult tokenizationResult, int tokenListIndex) {
    return tokenListIndex < tokenizationResult.tokens.size();
  }

  private Set<String> handleEpsilonTransition(Transition trans, LexerWrapper.TokenizationResult tokenizationResult, int tokenListIndex) {
    // Epsilon transitions don't consume a token, so don't move the index
    return parseAndCollectTokenSuggestions(trans.target, tokenizationResult, tokenListIndex);
  }

  private Set<String> handleAtomicTransition(AtomTransition trans, LexerWrapper.TokenizationResult tokenizationResult, int tokenListIndex) {
    Token nextToken = tokenizationResult.tokens.get(tokenListIndex);
    int nextTokenType = tokenizationResult.tokens.get(tokenListIndex).getType();
    boolean nextTokenMatchesTransition = (trans.label == nextTokenType);
    if (nextTokenMatchesTransition) {
      logger.fine("Token " + nextToken + " following transition: " + parserWrapper.toString(trans));
      return Collections.unmodifiableSet(parseAndCollectTokenSuggestions(trans.target, tokenizationResult, tokenListIndex + 1));
    } else {
      logger.fine("Token " + nextToken + " NOT following transition: " + parserWrapper.toString(trans));
      return Collections.unmodifiableSet(new HashSet<>());
    }
  }

  private Set<String> handleSetTransition(SetTransition trans, LexerWrapper.TokenizationResult tokenizationResult, int tokenListIndex) {
    Set<String> candidates = new HashSet<>();
    Token nextToken = tokenizationResult.tokens.get(tokenListIndex);
    int nextTokenType = nextToken.getType();
    for (int transitionTokenType : trans.label().toList()) {
      boolean nextTokenMatchesTransition = (transitionTokenType == nextTokenType);
      if (nextTokenMatchesTransition) {
        logger.fine("Token " + nextToken + " following transition: " + parserWrapper.toString(trans) + " to " + transitionTokenType);
        candidates.addAll(parseAndCollectTokenSuggestions(trans.target, tokenizationResult, tokenListIndex + 1));
      } else {
        logger.fine("Token " + nextToken + " NOT following transition: " + parserWrapper.toString(trans) + " to " + transitionTokenType);
      }
    }
    return Collections.unmodifiableSet(candidates);
  }

  private Set<String> suggestNextTokensForParserState(ATNState parserState, LexerWrapper.TokenizationResult tokenizationResult) {
    Set<Integer> transitionLabels = new HashSet<>();
    fillParserTransitionLabels(parserState, transitionLabels, new HashSet<>());
    TokenSuggester tokenSuggester = new TokenSuggester(tokenizationResult.untokenizedText, lexerWrapper);
    Collection<String> suggestions = tokenSuggester.suggest(transitionLabels);
    logger.fine("WILL SUGGEST TOKENS FOR STATE: " + parserState);
    return parseSuggestionsAndAddValidOnes(parserState, suggestions, tokenizationResult);
  }

  private void fillParserTransitionLabels(ATNState parserState, Collection<Integer> result, Set<TransitionWrapper> visitedTransitions) {
    for (Transition trans : parserState.getTransitions()) {
      TransitionWrapper transWrapper = new TransitionWrapper(parserState, trans);
      if (visitedTransitions.contains(transWrapper)) {
        logger.fine("Not following visited " + transWrapper);
        continue;
      }
      if (trans.isEpsilon()) {
        try {
          visitedTransitions.add(transWrapper);
          fillParserTransitionLabels(trans.target, result, visitedTransitions);
        } finally {
          visitedTransitions.remove(transWrapper);
        }
      } else if (trans instanceof AtomTransition) {
        int label = ((AtomTransition) trans).label;
        if (label >= 1) { // EOF would be -1
          result.add(label);
        }
      } else if (trans instanceof SetTransition) {
        for (Interval interval : trans.label().getIntervals()) {
          for (int i = interval.a; i <= interval.b; ++i) {
            result.add(i);
          }
        }
      }
    }
  }

  private Set<String> parseSuggestionsAndAddValidOnes(ATNState parserState, Collection<String> suggestions, LexerWrapper.TokenizationResult tokenizationResult) {
    Set<String> candidates = new HashSet<>();
    for (String suggestion : suggestions) {
      logger.fine("CHECKING suggestion: " + suggestion);
      Token addedToken = getAddedToken(suggestion, tokenizationResult);
      if (isParseableWithAddedToken(parserState, addedToken, new HashSet<>())) {
        candidates.add(suggestion);
      } else {
        logger.fine("DROPPING non-parseable suggestion: " + suggestion);
      }
    }
    return Collections.unmodifiableSet(candidates);
  }

  private Token getAddedToken(String suggestedCompletion, LexerWrapper.TokenizationResult tokenizationResult) {
    String completedText = this.input + suggestedCompletion;
    List<? extends Token> completedTextTokens = this.lexerWrapper.tokenizeNonDefaultChannel(completedText).tokens;
    if (completedTextTokens.size() <= tokenizationResult.tokens.size()) {
      return null; // Completion didn't yield whole token, could be just a token fragment
    }
    logger.fine("TOKENS IN COMPLETED TEXT: " + completedTextTokens);
    return completedTextTokens.get(completedTextTokens.size() - 1);
  }

  private boolean isParseableWithAddedToken(ATNState parserState, Token newToken, Set<TransitionWrapper> visitedTransitions) {
    if (newToken == null) {
      return false;
    }
    for (Transition parserTransition : parserState.getTransitions()) {
      if (parserTransition.isEpsilon()) { // Recurse through any epsilon transitionsStr
        TransitionWrapper transWrapper = new TransitionWrapper(parserState, parserTransition);
        if (visitedTransitions.contains(transWrapper)) {
          continue;
        }
        visitedTransitions.add(transWrapper);
        try {
          if (isParseableWithAddedToken(parserTransition.target, newToken, visitedTransitions)) {
            return true;
          }
        } finally {
          visitedTransitions.remove(transWrapper);
        }
      } else if (parserTransition instanceof AtomTransition) {
        AtomTransition parserAtomTransition = (AtomTransition) parserTransition;
        int transitionTokenType = parserAtomTransition.label;
        if (transitionTokenType == newToken.getType()) {
          return true;
        }
      } else if (parserTransition instanceof SetTransition) {
        SetTransition parserSetTransition = (SetTransition) parserTransition;
        for (int transitionTokenType : parserSetTransition.label().toList()) {
          if (transitionTokenType == newToken.getType()) {
            return true;
          }
        }
      } else {
        throw new IllegalStateException("Unexpected: " + parserWrapper.toString(parserTransition));
      }
    }
    return false;
  }
}
