package akovari.antlr4.autocomplete.impl;

import akovari.antlr4.autocomplete.Antlr4Completer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.logging.Logger;

public class ParserWrapper {
  private static final Logger logger = Logger.getLogger(Antlr4Completer.class.getName());
  private final Vocabulary lexerVocabulary;

  private final ATN parserAtn;
  private final String[] parserRuleNames;

  public ParserWrapper(ParserFactory parserFactory, Vocabulary lexerVocabulary) {
    this.lexerVocabulary = lexerVocabulary;

    Parser parserForAtnOnly = parserFactory.createParser(null);
    this.parserAtn = parserForAtnOnly.getATN();
    this.parserRuleNames = parserForAtnOnly.getRuleNames();
    logger.fine("Parser rule names: " + StringUtils.join(parserForAtnOnly.getRuleNames(), ", "));
  }

  public String toString(ATNState parserState) {
    return this.parserRuleNames[parserState.ruleIndex];
  }

  public String toString(Transition t) {
    String nameOrLabel = t.getClass().getSimpleName();
    if (t instanceof AtomTransition) {
      nameOrLabel += ' ' + this.lexerVocabulary.getDisplayName(((AtomTransition) t).label);
    }
    return nameOrLabel + " -> " + toString(t.target);
  }

  public String transitionsStr(ATNState state) {
    return StringUtils.join(Arrays.stream(state.getTransitions()).map(this::toString).iterator(), ", ");
  }

  public ATNState getAtnState(int stateNumber) {
    return parserAtn.states.get(stateNumber);
  }
}
