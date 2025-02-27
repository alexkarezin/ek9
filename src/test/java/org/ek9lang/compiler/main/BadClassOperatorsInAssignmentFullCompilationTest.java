package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad class operators in assignments usage.
 */
class BadClassOperatorsInAssignmentFullCompilationTest extends FullCompilationTest {

  public BadClassOperatorsInAssignmentFullCompilationTest() {
    super("/examples/parseButFailCompile/badClassAssignments");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.classassignment.use").isEmpty());
  }
}
