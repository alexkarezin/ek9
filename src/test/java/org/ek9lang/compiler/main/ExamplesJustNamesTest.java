package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesJustNamesTest extends FullCompilationTest {

  public ExamplesJustNamesTest() {
    super("/examples/justNames");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("just.names.check", 2).test(program);
  }
}
