package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.symbol.AbstractSymbolTestBase;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.Symbol;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommonTypeSuperOrTraitTest extends AbstractSymbolTestBase {

  private final ErrorListener errorListener = new ErrorListener("test");
  private final CommonTypeSuperOrTrait underTest = new CommonTypeSuperOrTrait(errorListener);

  @BeforeEach
  void clearErrors() {
    errorListener.reset();
  }

  @Test
  void testEmptyList() {
    var commonSymbol = underTest.apply(new SyntheticToken(), List.of());
    assertTrue(commonSymbol.isEmpty());
  }

  @Test
  void testUntypedTyped() {
    var sym1 = new Symbol("Sym1");
    sym1.setSourceToken(new SyntheticToken());
    assertNoSymbolDetected(List.of(sym1), false);
  }

  @Test
  void testIncompatibleTypes() {
    var sym1 = new Symbol("Sym1", Optional.of(ek9Integer));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(ek9String));
    sym2.setSourceToken(new SyntheticToken());
    assertNoSymbolDetected(List.of(sym1, sym2), true);
  }

  @Test
  void testListWithOneVariable() {
    var symbol = new Symbol("Sym", Optional.of(ek9Integer));
    symbol.setSourceToken(new SyntheticToken());
    assertSymbolType(ek9Integer, List.of(symbol));
  }

  @Test
  void testListWithMultipleVariables() {
    var sym1 = new Symbol("Sym1", Optional.of(ek9Integer));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(ek9Integer));
    sym2.setSourceToken(new SyntheticToken());
    assertSymbolType(ek9Integer, List.of(sym1, sym2));
  }

  @Test
  void testListWithMultipleVariablesUsingCommonSuperAggregateAsType() {
    var base = new AggregateSymbol("base", symbolTable);
    var agg1 = new AggregateSymbol("agg1", symbolTable);
    agg1.setSuperAggregateSymbol(base);
    var agg2 = new AggregateSymbol("agg2", symbolTable);
    agg2.setSuperAggregateSymbol(base);

    var sym1 = new Symbol("Sym1", Optional.of(agg1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(agg2));
    sym2.setSourceToken(new SyntheticToken());
    assertSymbolType(base, List.of(sym1, sym2));
  }

  @Test
  void testListWithMultipleVariablesUsingCommonTraitsAggregateAsType() {
    var base = new AggregateWithTraitsSymbol("baseTrait", symbolTable);
    var agg1 = new AggregateWithTraitsSymbol("agg1", symbolTable);
    agg1.addTrait(base);
    var agg2 = new AggregateWithTraitsSymbol("agg2", symbolTable);
    agg2.addTrait(base);

    var sym1 = new Symbol("Sym1", Optional.of(agg1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(agg2));
    sym2.setSourceToken(new SyntheticToken());
    assertSymbolType(base, List.of(sym1, sym2));
  }

  @Test
  void testListWithMultipleVariablesUsingNoCommonSuperAggregateAsType() {
    var base = new AggregateSymbol("base", symbolTable);
    var agg1 = new AggregateSymbol("agg1", symbolTable);
    agg1.setSuperAggregateSymbol(base);
    var agg2 = new AggregateSymbol("agg2", symbolTable);

    var sym1 = new Symbol("Sym1", Optional.of(agg1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(agg2));
    sym2.setSourceToken(new SyntheticToken());
    assertNoSymbolDetected(List.of(sym1, sym2), true);
  }

  @Test
  void testListWithMultipleVariablesUsingFunctionAsType() {
    var f1 = new FunctionSymbol("f1", symbolTable);
    var sym1 = new Symbol("Sym1", Optional.of(f1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(f1));
    sym2.setSourceToken(new SyntheticToken());
    assertSymbolType(f1, List.of(sym1, sym2));
  }

  @Test
  void testListWithIncompatibleTypes() {
    var f1 = new FunctionSymbol("f1", symbolTable);
    var agg1 = new AggregateSymbol("agg1", symbolTable);

    var sym1 = new Symbol("Sym1", Optional.of(f1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(agg1));
    sym2.setSourceToken(new SyntheticToken());

    //Check Both ways
    assertNoSymbolDetected(List.of(sym1, sym2), true);
    assertNoSymbolDetected(List.of(sym2, sym1), true);
  }

  @Test
  void testListWithMultipleVariablesUsingCommonSuperFunctionAsType() {
    var base = new FunctionSymbol("base", symbolTable);
    var f1 = new FunctionSymbol("f1", symbolTable);
    f1.setSuperFunctionSymbol(base);
    var f2 = new FunctionSymbol("f2", symbolTable);
    f2.setSuperFunctionSymbol(base);

    var sym1 = new Symbol("Sym1", Optional.of(f1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(f2));
    sym2.setSourceToken(new SyntheticToken());
    assertSymbolType(base, List.of(sym1, sym2));
  }

  @Test
  void testListWithMultipleVariablesUsingNoCommonSuperFunctionAsType() {
    var base = new FunctionSymbol("base", symbolTable);
    var f1 = new FunctionSymbol("f1", symbolTable);
    f1.setSuperFunctionSymbol(base);
    var f2 = new FunctionSymbol("f2", symbolTable);

    var sym1 = new Symbol("Sym1", Optional.of(f1));
    sym1.setSourceToken(new SyntheticToken());
    var sym2 = new Symbol("Sym2", Optional.of(f2));
    sym2.setSourceToken(new SyntheticToken());
    assertNoSymbolDetected(List.of(sym1, sym2), true);
  }

  private void assertNoSymbolDetected(final List<ISymbol> arguments, boolean expectErrors) {
    var commonSymbol = underTest.apply(new SyntheticToken(), arguments);
    assertTrue(commonSymbol.isEmpty());
    assertEquals(expectErrors, errorListener.hasErrors());
    //errorListener.getErrors().forEachRemaining(System.out::println);
  }

  private void assertSymbolType(final ISymbol symbolType, final List<ISymbol> arguments) {
    var commonSymbol = underTest.apply(new SyntheticToken(), arguments);
    assertTrue(commonSymbol.isPresent());
    assertEquals(symbolType, commonSymbol.get());
  }
}
