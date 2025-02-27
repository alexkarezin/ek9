package org.ek9lang.compiler.errors;

import java.util.function.Consumer;

/**
 * Error if a construct and a reference conflict with each other.
 */
public class ConstructAndReferenceConflict implements Consumer<ConflictingTokens> {
  private final String constructType;
  private final ErrorListener errorListener;
  private final ErrorListener.SemanticClassification classification;

  /**
   * Create consumer with specific details for the error.
   */
  public ConstructAndReferenceConflict(final String constructType,
                                       final ErrorListener errorListener,
                                       final ErrorListener.SemanticClassification classification) {
    this.constructType = constructType;
    this.errorListener = errorListener;
    this.classification = classification;
  }

  @Override
  public void accept(ConflictingTokens conflict) {

    var message = String.format(
        "defines %s, but '%s' line %d in %s already defines a references to '%s' line %d in %s;",
        constructType,
        conflict.firstUse().getText(),
        conflict.firstUse().getLine(),
        errorListener.getShortNameOfSourceFile(conflict.firstUse()),
        conflict.symbol().getFriendlyName(),
        conflict.symbol().getSourceToken().getLine(),
        errorListener.getShortNameOfSourceFile(conflict.symbol().getSourceToken())
    );
    errorListener.semanticError(conflict.tokenInError(), message, classification);
  }
}
