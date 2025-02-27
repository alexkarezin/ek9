package org.ek9lang.compiler.main.rules;

import static org.ek9lang.compiler.symbol.support.SymbolFactory.HTTP_VERB;
import static org.ek9lang.compiler.symbol.support.SymbolFactory.URI_PROTO;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;

/**
 * Examines all the service operations on a service and looks at the verbs used and the uti proto paths.
 * Checks for duplications and issues errors if paths (irrespective of variable names) are duplicated.
 */
public class CheckDuplicatedServicePaths extends RuleSupport implements Consumer<IAggregateSymbol> {

  public CheckDuplicatedServicePaths(
      SymbolAndScopeManagement symbolAndScopeManagement,
      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IAggregateSymbol service) {
    final Map<String, Map<String, ServiceOperationSymbol>> verbToPathToOperation = new HashMap<>();

    getServiceOperations(service).forEach(operation -> {
      var verb = operation.getSquirrelledData(HTTP_VERB);
      var path = removedParameterNames(operation.getSquirrelledData(URI_PROTO));

      var verbMap = verbToPathToOperation.get(verb);
      if (verbMap == null) {
        verbMap = new HashMap<>();
        verbMap.put(path, operation);
        verbToPathToOperation.put(verb, verbMap);
      } else {
        var existingPathToService = verbMap.get(path);
        if (existingPathToService == null) {
          verbMap.put(path, operation);
        } else {
          emitDuplicatePathError(existingPathToService, operation);
        }
      }
    });
  }

  private void emitDuplicatePathError(final ServiceOperationSymbol existingPathToService,
                                      final ServiceOperationSymbol operation) {

    var verb = existingPathToService.getSquirrelledData(HTTP_VERB);
    var msg = "HTTP verb: '"
        + verb
        + "' '"
        + existingPathToService.getSquirrelledData(URI_PROTO)
        + "' and '"
        + operation.getSquirrelledData(URI_PROTO)
        + "':";

    //Output two errors one of each of the clashing values.
    errorListener.semanticError(existingPathToService.getSourceToken(), msg,
        ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_DUPLICATED);
    errorListener.semanticError(operation.getSourceToken(), msg,
        ErrorListener.SemanticClassification.SERVICE_HTTP_PATH_DUPLICATED);
  }

  private Stream<ServiceOperationSymbol> getServiceOperations(final IAggregateSymbol service) {
    return service.getAllNonAbstractMethods()
        .stream()
        .filter(ServiceOperationSymbol.class::isInstance)
        .map(ServiceOperationSymbol.class::cast);
  }

  private String removedParameterNames(final String utiProtoPath) {
    return utiProtoPath.replaceAll("\\{.*?}", "{}");
  }
}
