package org.ek9lang.compiler.main.rules;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ICanBeGeneric;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Checks methods from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check.
 */
public class CheckMethod extends RuleSupport implements BiConsumer<MethodSymbol, EK9Parser.MethodDeclarationContext> {

  private final CommonMethodChecks commonMethodChecks;
  private final CheckTraitMethod checkTraitMethod;
  private final CheckNonTraitMethod checkNonTraitMethod;
  private final CheckNonExtendableMethod checkNonExtendableMethod;
  private final CheckNotDispatcherMethod checkNotDispatcherMethod;
  private final CheckGenericConstructor checkGenericConstructor;
  private final CheckProgramReturns checkProgramReturns;
  private final CheckProgramArguments checkProgramArguments;
  private final CheckForImplementation checkForImplementation;
  private final CheckNormalTermination checkNormalTermination;
  private final CheckNoMethodReturn checkNoMethodReturn;


  /**
   * Create a new method checker.
   */
  public CheckMethod(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    commonMethodChecks = new CommonMethodChecks(errorListener);
    checkNonTraitMethod = new CheckNonTraitMethod(errorListener);
    checkNonExtendableMethod = new CheckNonExtendableMethod(errorListener);
    checkTraitMethod = new CheckTraitMethod();
    checkNotDispatcherMethod = new CheckNotDispatcherMethod(errorListener);
    checkGenericConstructor = new CheckGenericConstructor(errorListener);
    checkProgramReturns = new CheckProgramReturns(errorListener);
    checkProgramArguments = new CheckProgramArguments(errorListener);
    checkForImplementation = new CheckForImplementation(errorListener);
    checkNormalTermination = new CheckNormalTermination(errorListener);
    checkNoMethodReturn = new CheckNoMethodReturn(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {

    if (method.isConstructor()) {
      checkAsConstructor(method, ctx);
    }

    if (ctx.getParent() instanceof EK9Parser.ProgramBlockContext) {
      checkAsProgram(method, ctx);
    }

    if (ctx.getParent() instanceof EK9Parser.ServiceDeclarationContext) {
      checkAsServiceMethod(method, ctx);
    }

    if (ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext) {
      checkTraitMethod.accept(method, ctx);
    }

    //If not in a class then method must not be marked as dispatcher.
    if (!(ctx.getParent().getParent() instanceof EK9Parser.ClassDeclarationContext)
        && !(ctx.getParent().getParent() instanceof EK9Parser.DynamicClassDeclarationContext)) {
      checkNotDispatcherMethod.accept(method, ctx);
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      checkNonTraitMethod.accept(method, ctx);
    }

    commonMethodChecks.accept(method, ctx);

    checkIfExtensionIsAllowedByContext(method, ctx);

  }

  private void checkIfExtensionIsAllowedByContext(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {
    var containingConstructCtx = ctx.getParent().getParent();
    if (containingConstructCtx instanceof EK9Parser.ClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.DynamicClassDeclarationContext
        || containingConstructCtx instanceof EK9Parser.ComponentDeclarationContext) {
      var parent = symbolAndScopeManagement.getRecordedSymbol(containingConstructCtx);
      var constructLine = containingConstructCtx.start.getLine();
      if (parent instanceof ICanBeGeneric possibleGeneric
          && !possibleGeneric.isOpenForExtension() && method.isMarkedAbstract()) {
        var message = "containing construct, on line " + constructLine + ", is not open for extension, '"
            + method.getFriendlyName() + "':";
        errorListener.semanticError(ctx.ABSTRACT().getSymbol(), message,
            ErrorListener.SemanticClassification.CANNOT_BE_ABSTRACT);
      }
    }
  }

  private void checkAsConstructor(final MethodSymbol method, final EK9Parser.MethodDeclarationContext ctx) {
    checkNormalTermination.accept(ctx.start, method);
    checkGenericConstructor.accept(ctx.start, method);
    checkNoMethodReturn.accept(method, ctx);
  }

  private void checkAsProgram(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {
    checkProgramReturns.accept(ctx.start, method);
    checkProgramArguments.accept(ctx.start, method);
    checkNonExtendableMethod.accept(method, ctx);
  }

  private void checkAsServiceMethod(MethodSymbol method, EK9Parser.MethodDeclarationContext ctx) {
    checkForImplementation.accept(ctx);
    checkNonExtendableMethod.accept(method, ctx);
  }
}
