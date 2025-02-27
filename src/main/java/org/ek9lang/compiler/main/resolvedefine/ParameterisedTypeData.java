package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * To be used when trying to define/resolve a parameterised type.
 */
public record ParameterisedTypeData(Token location, ISymbol genericTypeSymbol, List<ISymbol> typeArguments) {
}
