package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Ensures that a try statement/expression is used correctly in or out of an expression.
 */
public class CheckTryReturns extends CheckReturns implements Consumer<EK9Parser.TryStatementExpressionContext> {

  public CheckTryReturns(final ErrorListener errorListener) {
    super(errorListener);
  }

  @Override
  public void accept(EK9Parser.TryStatementExpressionContext ctx) {
    check(ctx.parent instanceof EK9Parser.StatementContext, ctx.start, ctx.returningParam());
  }
}
