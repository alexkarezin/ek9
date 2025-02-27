package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.DEFAULTED;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;

/**
 * Checks operators from various contexts, typically this is delegated to other functions.
 * Those functions do the detail check. This does not check service operations that are marked as operators.
 */
public class CheckOperator extends RuleSupport
    implements BiConsumer<MethodSymbol, EK9Parser.OperatorDeclarationContext> {

  private static final String OPERATOR_SEMANTICS = "operator semantics:";
  private final CheckTraitMethod checkTraitMethod;
  private final CheckNonTraitMethod checkNonTraitMethod;
  private final CheckIfExtendableByContext checkIfExtendableByContext;
  private final CheckInappropriateBody checkInappropriateBody;
  private final Map<String, Consumer<MethodSymbol>> operatorChecks = new HashMap<>();
  private final CheckOverrideAndAbstract checkOverrideAndAbstract;

  /**
   * Create a new operation checker.
   */
  public CheckOperator(final SymbolAndScopeManagement symbolAndScopeManagement,
                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    populateOperatorChecks();

    checkNonTraitMethod = new CheckNonTraitMethod(errorListener);
    checkIfExtendableByContext = new CheckIfExtendableByContext(symbolAndScopeManagement, errorListener);
    checkTraitMethod = new CheckTraitMethod();
    checkInappropriateBody = new CheckInappropriateBody(symbolAndScopeManagement, errorListener);
    checkOverrideAndAbstract = new CheckOverrideAndAbstract(symbolAndScopeManagement, errorListener);
  }

  private void populateOperatorChecks() {

    final Map<String, Consumer<MethodSymbol>> logicalOperatorChecks = Map.of(
        "<", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<=", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        ">", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        ">=", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "==", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<>", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "<=>", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "<~>", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "!=", addPureCheck(this::emitBadNotEqualsOperator));

    final Map<String, Consumer<MethodSymbol>> simpleOperatorChecks = Map.of(
        "sqrt", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "!", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "?", addPureCheck(this::testAcceptNoArgumentsReturnBoolean),
        "~", addPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "+", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "-", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "*", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "/", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "^", addPureCheck(this::testAcceptOneArgumentsReturnAnyType)
    );

    final Map<String, Consumer<MethodSymbol>> noArgumentWithReturnChecks = Map.of(
        "#^", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "$$", addPureCheck(this::testAcceptNoArgumentsReturnJson),
        "$", addPureCheck(this::testAcceptNoArgumentsReturnString),
        "#?", addPureCheck(this::testAcceptNoArgumentsReturnInteger),
        "#<", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "#>", addPureCheck(this::testAcceptNoArgumentsReturnAnyType),
        "not", addPureCheck(this::emitBadNotOperator),
        "abs", addPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "empty", addPureCheck(this::testAcceptNoArgumentsReturnBoolean),
        "length", addPureCheck(this::testAcceptNoArgumentsReturnInteger)
    );

    final Map<String, Consumer<MethodSymbol>> oneArgumentWithReturnChecks = Map.of(
        ">>", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "<<", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "and", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "or", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "xor", addPureCheck(this::testAcceptOneArgumentsReturnAnyType),
        "mod", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "rem", addPureCheck(this::testAcceptOneArgumentsReturnInteger),
        "contains", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "matches", addPureCheck(this::testAcceptOneArgumentsReturnBoolean),
        "close", addPureCheck(this::testAcceptNoArgumentsNoReturn)
    );

    operatorChecks.putAll(logicalOperatorChecks);
    operatorChecks.putAll(simpleOperatorChecks);
    operatorChecks.putAll(noArgumentWithReturnChecks);
    operatorChecks.putAll(oneArgumentWithReturnChecks);

    //Now for each of those operators tag on the is pure check.
    final Map<String, Consumer<MethodSymbol>> mutatorChecks = Map.of(
        ":~:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        ":^:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        ":=:", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "|", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "+=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "-=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "*=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "/=", addNonPureCheck(this::testAcceptOneArgumentsNoReturn),
        "++", addNonPureCheck(this::testAcceptNoArgumentsReturnConstructType),
        "--", addNonPureCheck(this::testAcceptNoArgumentsReturnConstructType)
    );
    operatorChecks.putAll(mutatorChecks);
  }

  @Override
  public void accept(final MethodSymbol methodSymbol, final EK9Parser.OperatorDeclarationContext ctx) {

    //Operator can be defaulted and so compiler will create implementation
    var defaulted = "TRUE".equals(methodSymbol.getSquirrelledData(DEFAULTED));

    if (!defaulted) {
      operatorChecks.getOrDefault(ctx.operator().getText(), method -> {}).accept(methodSymbol);
    }

    if (ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext) {
      checkTraitMethod.accept(methodSymbol, ctx.operationDetails());
    }

    if (!(ctx.getParent().getParent() instanceof EK9Parser.TraitDeclarationContext)) {
      checkNonTraitMethod.accept(methodSymbol, ctx.operationDetails());
    }

    checkIfExtendableByContext.accept(methodSymbol, ctx);
    checkInappropriateBody.accept(methodSymbol, ctx.operationDetails());
    checkOverrideAndAbstract.accept(methodSymbol);
  }

  private Consumer<MethodSymbol> addPureCheck(final Consumer<MethodSymbol> check) {
    return check.andThen(this::testPure);
  }

  private Consumer<MethodSymbol> addNonPureCheck(final Consumer<MethodSymbol> check) {
    return check.andThen(this::testNotPure);
  }

  private void testAcceptNoArgumentsReturnAnyType(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testAnyReturnType(methodSymbol);
  }

  private void testAcceptNoArgumentsReturnBoolean(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testReturnIsBoolean(methodSymbol);
  }

  private void testAcceptNoArgumentsReturnInteger(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testReturnIsInteger(methodSymbol);
  }

  private void testAcceptNoArgumentsReturnString(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testReturnType(methodSymbol, "String", ErrorListener.SemanticClassification.MUST_RETURN_STRING);
  }

  private void testAcceptNoArgumentsReturnJson(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testReturnType(methodSymbol, "JSON", ErrorListener.SemanticClassification.MUST_RETURN_JSON);
  }

  private void testAcceptOneArgumentsReturnInteger(final MethodSymbol methodSymbol) {
    testSingleArgument(methodSymbol);
    testReturnIsInteger(methodSymbol);
  }

  private void testAcceptOneArgumentsReturnBoolean(final MethodSymbol methodSymbol) {
    testSingleArgument(methodSymbol);
    testReturnIsBoolean(methodSymbol);
  }

  private void testReturnIsBoolean(final MethodSymbol methodSymbol) {
    testReturnType(methodSymbol, "Boolean", ErrorListener.SemanticClassification.MUST_RETURN_BOOLEAN);
  }

  private void testReturnIsInteger(MethodSymbol methodSymbol) {
    testReturnType(methodSymbol, "Integer", ErrorListener.SemanticClassification.MUST_RETURN_INTEGER);
  }


  private void testAcceptOneArgumentsReturnAnyType(final MethodSymbol methodSymbol) {
    testSingleArgument(methodSymbol);
    testAnyReturnType(methodSymbol);
  }

  private void testAcceptOneArgumentsNoReturn(final MethodSymbol methodSymbol) {
    testSingleArgument(methodSymbol);
    testNoReturn(methodSymbol);
  }

  private void testAcceptNoArgumentsNoReturn(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    testNoReturn(methodSymbol);
  }

  private void testPure(final MethodSymbol methodSymbol) {
    if (!methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_MUST_BE_PURE);
    }
  }

  private void testNotPure(final MethodSymbol methodSymbol) {
    if (methodSymbol.isMarkedPure()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.OPERATOR_CANNOT_BE_PURE);
    }
  }

  private void testAcceptNoArgumentsReturnConstructType(final MethodSymbol methodSymbol) {
    testNoArguments(methodSymbol);
    var parentScope = methodSymbol.getParentScope();
    var returnType = testAnyReturnType(methodSymbol);
    returnType.ifPresent(theType -> {
      if (!theType.isExactSameType((ISymbol) parentScope)) {
        var parentTypeName = parentScope.getFriendlyScopeName().startsWith("_Class")
            ? "DYNAMIC CLASS" : parentScope.getFriendlyScopeName();
        var msg = "'" + theType.getFriendlyName() + "' is not '" + parentTypeName + "':";
        errorListener.semanticError(methodSymbol.getSourceToken(), msg,
            ErrorListener.SemanticClassification.MUST_RETURN_SAME_TYPE);
      }
    });
  }

  private Optional<ISymbol> testAnyReturnType(final MethodSymbol methodSymbol) {
    if (!methodSymbol.isReturningSymbolPresent()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.RETURNING_MISSING);
      return Optional.empty();
    }
    return methodSymbol.getType();
  }

  private void testNoReturn(final MethodSymbol methodSymbol) {
    if (methodSymbol.isReturningSymbolPresent() && methodSymbol.getReturningSymbol().getType().isPresent()) {
      var theType = methodSymbol.getReturningSymbol().getType().get();
      errorListener.semanticError(methodSymbol.getSourceToken(), "'" + theType.getFriendlyName() + "'",
          ErrorListener.SemanticClassification.RETURN_VALUE_NOT_SUPPORTED);
    }
  }

  private void testReturnType(final MethodSymbol methodSymbol,
                              final String expectedType,
                              final ErrorListener.SemanticClassification errorIfInvalid) {
    var validReturnType = false;
    if (methodSymbol.isReturningSymbolPresent()) {
      var theType = methodSymbol.resolve(new TypeSymbolSearch(expectedType));
      var returningType = methodSymbol.getReturningSymbol().getType();
      if (returningType.isPresent() && theType.isPresent()) {
        validReturnType = returningType.get().isExactSameType(theType.get());
      }
    }

    if (!validReturnType) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS, errorIfInvalid);
    }
  }

  private void testNoArguments(final MethodSymbol methodSymbol) {
    if (!methodSymbol.getSymbolsForThisScope().isEmpty()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_MANY_ARGUMENTS);
    }
  }

  private void testSingleArgument(final MethodSymbol methodSymbol) {
    if (methodSymbol.getSymbolsForThisScope().isEmpty()) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_FEW_ARGUMENTS);
    } else if (methodSymbol.getSymbolsForThisScope().size() > 1) {
      errorListener.semanticError(methodSymbol.getSourceToken(), OPERATOR_SEMANTICS,
          ErrorListener.SemanticClassification.TOO_MANY_ARGUMENTS);
    }
  }

  private void emitBadNotEqualsOperator(final MethodSymbol methodSymbol) {
    errorListener.semanticError(methodSymbol.getSourceToken(), "alternative:",
        ErrorListener.SemanticClassification.BAD_NOT_EQUAL_OPERATOR);
  }

  private void emitBadNotOperator(final MethodSymbol methodSymbol) {
    errorListener.semanticError(methodSymbol.getSourceToken(), "alternative:",
        ErrorListener.SemanticClassification.BAD_NOT_OPERATOR);
  }
}