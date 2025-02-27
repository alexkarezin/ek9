package org.ek9lang.compiler.main.rules;

import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Ensures that a try statement/expression is used correctly in or out of an expression.
 */
public abstract class CheckReturns {

  protected final ErrorListener errorListener;

  protected CheckReturns(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  protected void check(final boolean isStatement, final Token parentToken,
                       final EK9Parser.ReturningParamContext returningParamCtx) {

    if (isStatement && returningParamCtx != null) {
      //We do not allow a return section. As it makes no sense
      errorListener.semanticError(returningParamCtx.LEFT_ARROW().getSymbol(), "",
          ErrorListener.SemanticClassification.RETURNING_REDUNDANT);
    } else if (!isStatement && returningParamCtx == null) {
      //In this case it only makes sense if a return section is included.
      errorListener.semanticError(parentToken, "",
          ErrorListener.SemanticClassification.RETURNING_REQUIRED);
    }
  }
}
