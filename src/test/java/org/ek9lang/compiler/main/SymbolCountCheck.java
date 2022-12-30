package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Predicate;
import org.ek9lang.compiler.internals.CompilableProgram;

public class SymbolCountCheck implements Predicate<CompilableProgram> {

  private final String forModuleName;
  private final int expectedSymbolCount;

  public SymbolCountCheck(final String forModuleName, final int expectedSymbolCount) {
    this.forModuleName = forModuleName;
    this.expectedSymbolCount = expectedSymbolCount;

  }

  @Override
  public boolean test(CompilableProgram compilableProgram) {
    var modules = compilableProgram.getParsedModules(forModuleName);
    assertNotNull(modules);
    //only ever expect 1 during these tests
    assertEquals(1, modules.size());
    var parsedModule = modules.get(0);
    assertEquals(expectedSymbolCount, parsedModule.getModuleScope().getSymbolsForThisScope().size());

    return true;
  }
}
