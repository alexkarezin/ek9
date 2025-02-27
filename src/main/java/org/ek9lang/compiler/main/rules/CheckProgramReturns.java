package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.AggregateFactory;

/**
 * A program can only return an Integer (exit code).
 */
public class CheckProgramReturns implements BiConsumer<Token, MethodSymbol> {
  private final ErrorListener errorListener;

  public CheckProgramReturns(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final Token token, final MethodSymbol methodSymbol) {

    if (methodSymbol.isReturningSymbolPresent()) {
      var returningSymbol = methodSymbol.getReturningSymbol();
      var returningSymbolsType = returningSymbol.getType();
      if (returningSymbolsType.isEmpty() || !AggregateFactory.EK9_INTEGER.equals(
          returningSymbolsType.get().getFullyQualifiedName())) {
        errorListener.semanticError(returningSymbol.getSourceToken(), "",
            ErrorListener.SemanticClassification.PROGRAM_CAN_ONLY_RETURN_INTEGER);
      }
    }
  }
}
