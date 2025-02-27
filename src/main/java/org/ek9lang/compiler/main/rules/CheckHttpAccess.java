package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_ACCESS;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_HEADER;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_PATH;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_QUERY;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_SOURCE;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Checks HTTP Access for service operations because some require HTTP_SOURCE, but others do not support it.
 */
public class CheckHttpAccess extends RuleSupport implements Consumer<ISymbol> {
  public CheckHttpAccess(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final ISymbol param) {
    //When it is a path variable, must check that the path contains the variable.
    var access = param.getSquirrelledData(HTTP_ACCESS);
    var sourceName = param.getSquirrelledData(HTTP_SOURCE);

    if ((HTTP_PATH.equals(access) || HTTP_QUERY.equals(access) || HTTP_HEADER.equals(access))) {
      if (sourceName == null) {
        //These need a qualifier, so we know when path/query/header value to extract and use
        var msg = "'" + access + "' requires additional qualifier name:";
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PARAM_NEEDS_QUALIFIER);
      }
    } else {
      //The other types of access do not and cannot be further qualified
      if (sourceName != null) {
        var msg = "'" + access + "' does not require qualifier '" + sourceName + "':";
        errorListener.semanticError(param.getSourceToken(), msg,
            ErrorListener.SemanticClassification.SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED);

      }
    }
  }
}
