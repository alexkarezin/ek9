package org.ek9lang.cli;

import org.ek9lang.cli.support.CompilationContext;

/**
 * Update/Upgrade the Ek9 compiler itself.
 */
public class Up extends E {
  public Up(CompilationContext compilationContext) {
    super(compilationContext);
  }

  @Override
  protected String messagePrefix() {
    return "Update  : ";
  }

  protected boolean doRun() {
    log("Would check compiler version and available version and update.");
    return true;
  }
}
