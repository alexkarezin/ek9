package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.CompilationEvent;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED.
 * At this point all sources have parsed, all symbols resolved and IR is built.
 * During the creation of the IR some analysis will have already taken place;
 * this is aimed at 'failing early'. This enables the developer to address issues in
 * large code bases much earlier.
 * However, there is some analysis that can only take place once we have the whole IR in place.
 * That's what this phase is for, analysing the whole IR and doing any and all final checks before
 * optimisation takes place.
 */
public class Ek9Phase9IRAnalysis implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.IR_ANALYSIS;

  public Ek9Phase9IRAnalysis(Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true, compilerFlags.getCompileToPhase() == thisPhase);
  }
}
