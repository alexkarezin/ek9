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
 * SINGLE THREADED
 * Now just create all the nodes that are generated from generic types
 * So these will be the set of concrete template classes we are using throughout the program all
 * modules!
 * TODO need to think about where the concrete templates should be recorded.
 * They will be recorded in the same module as their generic type - clearly we will need to
 * add functionality to detect this and generate, as the generic type might be an 'extern'.
 * The generic base they are created from has to be revisited which means we need the right
 * parsed module.
 */
public class Ek9Phase8IRTemplateGeneration
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  private static final CompilationPhase thisPhase = CompilationPhase.TEMPLATE_IR_GENERATION;
  
  public Ek9Phase8IRTemplateGeneration(Consumer<CompilationEvent> listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {

    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

}
