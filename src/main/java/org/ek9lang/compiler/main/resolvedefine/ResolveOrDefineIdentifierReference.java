package org.ek9lang.compiler.main.resolvedefine;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;

/**
 * The bulk of the processing is in the abstract base.
 * This just defines the entry point for identifierReference context.
 */
public class ResolveOrDefineIdentifierReference extends ResolveOrDefineTypes
    implements Function<EK9Parser.IdentifierReferenceContext, Optional<ISymbol>> {

  /**
   * A bit of a complex constructor - for a function.
   */
  public ResolveOrDefineIdentifierReference(final SymbolAndScopeManagement symbolAndScopeManagement,
                                            final SymbolFactory symbolFactory, final ErrorListener errorListener,
                                            final boolean errorIfNotDefinedOrResolved) {
    super(symbolAndScopeManagement, symbolFactory, errorListener, errorIfNotDefinedOrResolved);
  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.IdentifierReferenceContext ctx) {

    if (ctx == null) {
      return Optional.empty();
    }
    return resolveSimpleTypeByIdentifierReference(ctx);
  }
}
