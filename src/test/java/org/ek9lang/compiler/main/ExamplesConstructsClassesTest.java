package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Just test simple classes all compile.
 */
class ExamplesConstructsClassesTest extends FullCompilationTest {

  public ExamplesConstructsClassesTest() {
    super("/examples/constructs/classes");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.classes", 7).test(program);

    new SymbolCountCheck("net.customer.shapes", 19).test(program);

    new SymbolCountCheck("net.customer", 28).test(program);

    new SymbolCountCheck("com.customer.just", 12).test(program);

    new SymbolCountCheck("net.customer.assertions", 4).test(program);

    var coordinateSymbol = program.resolveByFullyQualifiedSearch(new TypeSymbolSearch("com.customer.just::Coordinate"));
    assertTrue(coordinateSymbol.isPresent());
  }

}
