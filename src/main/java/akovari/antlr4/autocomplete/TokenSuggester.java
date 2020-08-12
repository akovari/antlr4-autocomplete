package akovari.antlr4.autocomplete;

import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.javatuples.Triplet;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Given an ATN state and the lexer ATN, suggests auto-completion texts.
 */
class TokenSuggester {
  private static final Logger logger = Logger.getLogger(Antlr4Completer.class.getName());

  private final LexerWrapper lexerWrapper;

  private final Set<Integer> visitedLexerStates = new HashSet<>();
  private final String origPartialToken;

  public TokenSuggester(String origPartialToken, LexerWrapper lexerWrapper) {
    this.origPartialToken = origPartialToken;
    this.lexerWrapper = lexerWrapper;
  }

  public Collection<String> suggest(Collection<Integer> nextParserTransitionLabels) {
    Set<String> suggestions = new HashSet<>();
    logTokensUsedForSuggestion(nextParserTransitionLabels);
    for (int nextParserTransitionLabel : nextParserTransitionLabels) {
      int nextTokenRuleNumber = nextParserTransitionLabel - 1; // Count from 0 not from 1
      ATNState lexerState = this.lexerWrapper.findStateByRuleNumber(nextTokenRuleNumber);
      suggestions.addAll(suggest("", lexerState, origPartialToken));
    }
    return Collections.unmodifiableSet(suggestions);
  }

  private void logTokensUsedForSuggestion(Collection<Integer> ruleIndices) {
    if (!logger.isLoggable(Level.FINE)) {
      return;
    }
    String ruleNames = ruleIndices.stream().map(r -> lexerWrapper.getRuleNames()[r - 1]).collect(Collectors.joining(" "));
    logger.fine("Suggesting tokens for lexer rules: " + ruleNames);
  }

  private Set<String> suggest(String tokenSoFar, ATNState lexerState, String remainingText) {
    return lexerWrapper.tokenSuggestionCache.computeIfAbsent(new Triplet<>(tokenSoFar, lexerState, remainingText), this::suggest);
  }

  private Set<String> suggest(Triplet<String, ATNState, String> args) {
    Set<String> suggestions = new HashSet<>();
    String tokenSoFar = args.getValue0();
    ATNState lexerState = args.getValue1();
    String remainingText = args.getValue2();

    int stateNumber = lexerState.stateNumber;
    String stateName = lexerWrapper.stateToString(lexerState);

    if (!lexerWrapper.isValidSuggestion(stateName)) {
      return suggestions;
    }

    if (visitedLexerStates.contains(stateNumber)) {
      return suggestions; // avoid infinite loop and stack overflow
    }

    logger.fine(
        "SUGGEST: tokenSoFar=" + tokenSoFar + " remainingText=" + remainingText + " lexerState=" + stateName);

    visitedLexerStates.add(stateNumber);

    try {
      Transition[] transitions = lexerState.getTransitions();
      boolean tokenNotEmpty = tokenSoFar.length() > 0;
      boolean noMoreCharactersInToken = (transitions.length == 0);

      if (tokenNotEmpty && noMoreCharactersInToken) {
        suggestions.add(addSuggestedToken(tokenSoFar));
        return suggestions;
      }

      for (Transition trans : transitions) {
        suggestions.addAll(suggestViaLexerTransition(tokenSoFar, remainingText, trans));
      }
    } finally {
      visitedLexerStates.remove(stateNumber);
    }

    return Collections.unmodifiableSet(suggestions);
  }

  private Set<String> suggestViaLexerTransition(String tokenSoFar, String remainingText, Transition trans) {
    Set<String> suggestions = new HashSet<>();
    if (trans.isEpsilon()) {
      suggestions.addAll(suggest(new Triplet<>(tokenSoFar, trans.target, remainingText)));
    } else if (trans instanceof AtomTransition) {
      String newTokenChar = lexerWrapper.getAddedTextFor((AtomTransition) trans);

      if (remainingText.isEmpty() || remainingText.startsWith(newTokenChar)) {
        logger.fine("LEXER TOKEN: " + newTokenChar + " remaining=" + remainingText);
        suggestions.addAll(suggestViaNonEpsilonLexerTransition(tokenSoFar, remainingText, newTokenChar, trans.target));
      } else {
        logger.fine("NONMATCHING LEXER TOKEN: " + newTokenChar + " remaining=" + remainingText);
      }
    } else if (trans instanceof SetTransition) {
      List<Integer> symbols = trans.label().toList();

      for (Integer symbol : symbols) {
        char[] charArr = Character.toChars(symbol);
        String charStr = new String(charArr);

        if (remainingText.isEmpty() || remainingText.startsWith(charStr)) {
          suggestions.addAll(suggestViaNonEpsilonLexerTransition(tokenSoFar, remainingText, charStr, trans.target));
        }
      }
    }
    return Collections.unmodifiableSet(suggestions);
  }

  private Set<String> suggestViaNonEpsilonLexerTransition(String tokenSoFar, String remainingText,
                                                          String newTokenChar, ATNState targetState) {
    String newRemainingText = (remainingText.length() > 0) ? remainingText.substring(1) : remainingText;
    return suggest(new Triplet<>(tokenSoFar + newTokenChar, targetState, newRemainingText));
  }

  private String addSuggestedToken(String tokenToAdd) {
    return chopOffCommonStart(tokenToAdd, this.origPartialToken);
  }

  private String chopOffCommonStart(String a, String b) {
    int charsToChopOff = Math.min(b.length(), a.length());
    return a.substring(charsToChopOff);
  }
}
