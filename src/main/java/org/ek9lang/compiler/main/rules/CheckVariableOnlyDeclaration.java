package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.VariableSymbol;

/**
 * Checks on whether the '?' can be used as a modifier
 * then check the parent of this context. Error if developer has; must state that this variable can be
 * uninitialised.
 * For incoming parameters it is not appropriate to use '!', '?'.
 * For a returning parameter it is not appropriate to use '!' i.e. injection nor a web correlation.
 * For aggregate properties and block statements one of ! or ? is needed, but web correlation is not allowed.
 * Note that this does modify the variable and mark it up in various ways.
 */
public class CheckVariableOnlyDeclaration implements
    BiConsumer<EK9Parser.VariableOnlyDeclarationContext, VariableSymbol> {
  private final ErrorListener errorListener;

  public CheckVariableOnlyDeclaration(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.VariableOnlyDeclarationContext ctx, VariableSymbol variableSymbol) {

    if (ctx.getParent() instanceof EK9Parser.ArgumentParamContext) {
      checkArgumentParam(ctx, variableSymbol);
    } else if (ctx.getParent() instanceof EK9Parser.ReturningParamContext) {
      checkReturningParam(ctx, variableSymbol);
    } else {
      checkBlockAndAggregateProperty(ctx, variableSymbol);
    }
  }

  private void checkBlockAndAggregateProperty(final EK9Parser.VariableOnlyDeclarationContext ctx,
                                              VariableSymbol variableSymbol) {
    //So for aggregateProperty and Block statements
    if (ctx.webVariableCorrelation() != null) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.SERVICE_HTTP_ACCESS_NOT_SUPPORTED);
    }

    if (ctx.BANG() == null && ctx.QUESTION() == null) {
      //Then we have a variable only declaration that is not initialised.
      //Developer must use injection or must acknowledge this is uninitialised.
      errorListener.semanticError(ctx.start, "variable",
          ErrorListener.SemanticClassification.NOT_INITIALISED_IN_ANY_WAY);
    }
    if (ctx.BANG() != null) {
      //Then it is to be injected to mark as initialised = by self through injection
      variableSymbol.setInitialisedBy(ctx.start);
    }
  }

  private void checkArgumentParam(final EK9Parser.VariableOnlyDeclarationContext ctx, VariableSymbol variableSymbol) {
    if (ctx.BANG() != null) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.COMPONENT_INJECTION_NOT_POSSIBLE);
    }
    if (ctx.QUESTION() != null) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.DECLARED_AS_NULL_NOT_NEEDED);
    }
    //So mark up and the check on passing in guff (uninitialised values) is a check in the calling block.
    //As an incoming parameter it is assumed it will have been initialised to something.
    variableSymbol.setIncomingParameter(true);
    variableSymbol.setInitialisedBy(ctx.start);
  }

  private void checkReturningParam(final EK9Parser.VariableOnlyDeclarationContext ctx, VariableSymbol variableSymbol) {
    if (ctx.BANG() == null && ctx.QUESTION() == null) {
      //Then we have a variable only declaration that is not initialised.
      //Developer must use injection or must acknowledge this is uninitialised.
      errorListener.semanticError(ctx.start, "variable",
          ErrorListener.SemanticClassification.NOT_INITIALISED_IN_ANY_WAY);
    }

    if (ctx.webVariableCorrelation() != null) {
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.SERVICE_HTTP_ACCESS_NOT_SUPPORTED);
    }
    variableSymbol.setReturningParameter(true);
    //But for returning it is up to the method/function to ensure initialisation.
  }
}