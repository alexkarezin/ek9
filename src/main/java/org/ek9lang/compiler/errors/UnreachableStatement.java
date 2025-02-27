package org.ek9lang.compiler.errors;

import java.util.function.BiConsumer;
import org.antlr.v4.runtime.Token;

/**
 * To be called when an unreachable statement is encountered.
 * The first token is the point that is unreachable, the second token is
 * the cause of why this point is unreachable.
 */
public class UnreachableStatement implements BiConsumer<Token, Token> {

  private final ErrorListener errorListener;

  public UnreachableStatement(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final Token unreachablePoint, final Token reasonExceptionPoint) {
    final var message = String.format("Unreachable, because of '%s' on line %d:",
        reasonExceptionPoint.getText(),
        reasonExceptionPoint.getLine());

    errorListener.semanticError(unreachablePoint, message,
        ErrorListener.SemanticClassification.STATEMENT_UNREACHABLE);
  }
}
