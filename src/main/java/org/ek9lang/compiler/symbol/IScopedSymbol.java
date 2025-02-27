package org.ek9lang.compiler.symbol;

/**
 * For symbols that are both a pure symbol but can also define a scope.
 */
public interface IScopedSymbol extends IScope, ISymbol {
  IScopedSymbol clone(IScope withParentAsAppropriate);
}
