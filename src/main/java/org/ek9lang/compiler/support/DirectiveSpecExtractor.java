package org.ek9lang.compiler.support;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.main.phases.CompilationPhase;

/**
 * Just extract the spec from the free format data in the directive.
 */
public class DirectiveSpecExtractor implements Function<EK9Parser.DirectiveContext, DirectiveSpec> {

  private final DirectivesNextLineNumber directivesNextLineNumber = new DirectivesNextLineNumber();
  private final DirectivesCompilationPhase directivesCompilationPhase = new DirectivesCompilationPhase();

  private final DirectivesSymbolCategory directivesSymbolCategory = new DirectivesSymbolCategory();

  private final DirectivesSymbolName directivesSymbolName = new DirectivesSymbolName(2);
  private final DirectivesSymbolName directivesAdditionalSymbolName = new DirectivesSymbolName(3);

  @Override
  public DirectiveSpec apply(EK9Parser.DirectiveContext ctx) {
    int numParams = ctx.directivePart().size();
    if (numParams != 3 && numParams != 4) {
      throw new IllegalArgumentException(
          "Expecting, compilerPhase: symbolCategory: \"type/function\": {\"type/function\"}");
    }

    var applyToLine = directivesNextLineNumber.apply(ctx);
    CompilationPhase compilerPhase = directivesCompilationPhase.apply(ctx);
    var category = directivesSymbolCategory.apply(ctx);
    var symbolName = directivesSymbolName.apply(ctx);

    String additionalName = numParams == 4 ? directivesAdditionalSymbolName.apply(ctx) : null;

    return new DirectiveSpec(ctx.start, compilerPhase, category, symbolName, additionalName, applyToLine);
  }
}
