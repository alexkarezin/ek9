package org.ek9lang.compiler.main.phases.definition;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.main.resolvedefine.CheckMethodOverrides;
import org.ek9lang.compiler.main.resolvedefine.CheckPossibleFieldDelegate;
import org.ek9lang.compiler.main.resolvedefine.DynamicCaptureAndDefinition;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.CaptureScope;

/**
 * This is a really critical point, because this attempts to ensure that ANY expression
 * results in a symbol that has been 'typed'.
 * But it also has to 'resolve' types or 'generic types' and for inferred declarations must
 * work out what the types are (hence previous expressions must now be 'typed').
 * It will also trigger the creations of new parameterised types, from generic types and type arguments,
 * but 'just in time'.
 * Due to the ways tha antlr employs listeners, breaking up the processing of listener events in to
 * separate listeners in a type hierarchy seems to make most sense.
 * But the bulk of any real actual processing is pulled out to separate functions and classes.
 * So that these large and quite complex 'flows' can just focus on the event cycles and manage the scope stacks.
 */
public class ResolveDefineInferredTypeListener extends ExpressionsListener {

  private final DynamicCaptureAndDefinition dynamicCaptureAndDefinition;
  private final CheckPossibleFieldDelegate checkPossibleFieldDelegate;

  private final CheckMethodOverrides checkMethodOverrides;

  /**
   * Create a new instance to define or resolve inferred types.
   */
  public ResolveDefineInferredTypeListener(ParsedModule parsedModule) {
    super(parsedModule);
    this.dynamicCaptureAndDefinition =
        new DynamicCaptureAndDefinition(symbolAndScopeManagement, errorListener, symbolFactory);

    this.checkPossibleFieldDelegate = new CheckPossibleFieldDelegate(symbolAndScopeManagement, errorListener);
    this.checkMethodOverrides = new CheckMethodOverrides(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void enterDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    scope.setOpenToEnclosingScope(true);
    super.enterDynamicVariableCapture(ctx);
  }

  @Override
  public void exitDynamicVariableCapture(EK9Parser.DynamicVariableCaptureContext ctx) {
    CaptureScope scope = (CaptureScope) symbolAndScopeManagement.getRecordedScope(ctx);
    dynamicCaptureAndDefinition.accept(ctx);
    scope.setOpenToEnclosingScope(false);
    super.exitDynamicVariableCapture(ctx);
  }

  @Override
  public void exitAggregateProperty(EK9Parser.AggregatePropertyContext ctx) {
    var field = symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkPossibleFieldDelegate.accept(field);

    super.exitAggregateProperty(ctx);
  }

  @Override
  public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodOverrides.accept(symbol);

    super.exitClassDeclaration(ctx);
  }

  @Override
  public void exitTraitDeclaration(EK9Parser.TraitDeclarationContext ctx) {
    var symbol = (AggregateWithTraitsSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodOverrides.accept(symbol);
    super.exitTraitDeclaration(ctx);
  }

  @Override
  public void exitComponentDeclaration(EK9Parser.ComponentDeclarationContext ctx) {
    var symbol = (AggregateSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    checkMethodOverrides.accept(symbol);
    super.exitComponentDeclaration(ctx);
  }
}
