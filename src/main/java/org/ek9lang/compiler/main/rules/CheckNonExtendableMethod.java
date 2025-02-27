package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Aimed at methods in services and a program (which is a method).
 * These constructs cannot be extended from, hence the idea of overrides and abstract does not make sense.
 * So this is the compromise of making the grammar more general and reusing stuff, we now must write a rule to
 * stop something that has been allowed in the grammar. But this gives us the opportunity to give more specific
 * error feed back.
 */
public class CheckNonExtendableMethod implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final ErrorListener errorListener;

  public CheckNonExtendableMethod(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(MethodSymbol methodSymbol, EK9Parser.MethodDeclarationContext methodDeclarationContext) {
    final var message = "'" + methodDeclarationContext.identifier().getText() + "':";
    if (methodSymbol.isOverride()) {
      errorListener.semanticError(methodDeclarationContext.OVERRIDE().getSymbol(), message,
          ErrorListener.SemanticClassification.OVERRIDE_INAPPROPRIATE);
    }
    if (methodSymbol.isMarkedAbstract()) {
      errorListener.semanticError(methodDeclarationContext.ABSTRACT().getSymbol(), message,
          ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
    }
  }
}
