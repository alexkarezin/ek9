package org.ek9lang.compiler.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.support.TypeDefResolver;
import org.ek9lang.compiler.symbol.support.AggregateFactory;
import org.junit.jupiter.api.BeforeEach;

public class AbstractSymbolTestBase {

  private final SymbolTable global = new SymbolTable();

  protected final AggregateFactory support = new AggregateFactory();

  protected IScope symbolTable = new SymbolTable();

  protected ISymbol ek9Integer = new AggregateSymbol("Integer", global);
  protected ISymbol ek9Float = new AggregateSymbol("Float", global);
  protected ISymbol ek9String = new AggregateSymbol("String", global);
  protected ISymbol ek9Boolean = new AggregateSymbol("Boolean", global);
  protected ISymbol ek9Date = new AggregateSymbol("Date", global);
  protected ISymbol ek9Void = new AggregateSymbol("Void", global);
  protected ISymbol ek9Duration = new AggregateSymbol("Duration", global);

  protected ISymbol ek9Dimension = new AggregateSymbol("Dimension", global);

  protected ISymbol ek9Time = new AggregateSymbol("Time", global);

  @BeforeEach
  public void setupBasicSymbols() {
    symbolTable = new SymbolTable();
    symbolTable.define(ek9Integer);
    symbolTable.define(ek9Float);
    symbolTable.define(ek9String);
    symbolTable.define(ek9Boolean);
    symbolTable.define(ek9Date);
    symbolTable.define(ek9Void);
    symbolTable.define(ek9Duration);
    symbolTable.define(ek9Dimension);
    symbolTable.define(ek9Time);
  }

  protected void assertResolution(final String typeDefForm, final ISymbol.SymbolCategory expectedCategory) {
    var typeDefResolver = new TypeDefResolver(symbolTable);
    var resolved = typeDefResolver.typeDefToSymbol(typeDefForm);
    assertTrue(resolved.isPresent());
    assertEquals(expectedCategory, resolved.get().getCategory());
  }
}
