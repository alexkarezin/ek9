package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationAggregates;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationConstants;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationFunctions;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationPreparation;
import org.ek9lang.compiler.main.phases.Ek9Phase11CodeOptimisation;
import org.ek9lang.compiler.main.phases.Ek9Phase12Packaging;
import org.ek9lang.compiler.main.phases.Ek9Phase12PackagingPostProcessing;
import org.ek9lang.compiler.main.phases.Ek9Phase12PluginLinkage;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Supplies the compiler phases for the back end. Everything after IR.
 */
public class BackEndSupplier extends PhaseSupplier {

  /**
   * Create a new supplier of back-end compiler phases.
   */
  public BackEndSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                         CompilationPhaseListener listener,
                         CompilerReporter reporter) {
    super(compilableProgramAccess, listener, reporter);
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {
    return List.of(
        new Ek9Phase10CodeGenerationPreparation(listener, reporter),
        new Ek9Phase10CodeGenerationAggregates(listener, reporter),
        new Ek9Phase10CodeGenerationConstants(listener, reporter),
        new Ek9Phase10CodeGenerationFunctions(listener, reporter),
        new Ek9Phase11CodeOptimisation(listener, reporter),
        new Ek9Phase12PluginLinkage(listener, reporter),
        new Ek9Phase12Packaging(listener, reporter),
        new Ek9Phase12PackagingPostProcessing(listener, reporter));
  }
}
