package org.ek9lang.compiler.symbol;

/**
 * EK9 'for' type symbol - we need a scope because we declare a new variable as the loop variable.
 * The for loop can be used to iterator over a collection or increment/decrement over a range.
 */
public class ForSymbol extends ScopedSymbol {

  public ForSymbol(IScope enclosingScope) {
    super("For", enclosingScope);
    super.setCategory(SymbolCategory.CONTROL);
  }

  @Override
  public ForSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoForSymbol(new ForSymbol(withParentAsAppropriate));
  }

  protected ForSymbol cloneIntoForSymbol(ForSymbol newCopy) {
    super.cloneIntoScopeSymbol(newCopy);
    return newCopy;
  }

}