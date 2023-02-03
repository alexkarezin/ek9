package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad swtich usage.
 */
class BadSwitchFullCompilationTest extends FullCompilationTest {


  public BadSwitchFullCompilationTest() {
    super("/examples/parseButFailCompile/badSwitchUse");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(5, numberOfErrors);
    var alpha = program.getParsedModules("bad.switches.use");
    assertNotNull(alpha);
  }
}
