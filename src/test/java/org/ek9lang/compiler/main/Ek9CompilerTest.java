package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.testsupport.PathToSourceFromName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * OK so here it is, the first test of the compiler.
 * Obviously in an agile manner I'll have to build this up in stages.
 * Then eventually I'll need a fuzzer.
 * But first lets just take single source file and parse it.
 * Then we can add further stages.
 */
class Ek9CompilerTest {

  private static final Supplier<Workspace> validEk9Workspace = () -> {
    var fullPath = new PathToSourceFromName().apply("/examples/basics/HelloWorld.ek9");
    Workspace rtn = new Workspace();
    rtn.addSource(fullPath);
    return rtn;
  };

  private static final Supplier<Workspace> inValidEk9Workspace = () -> {
    var fullPath = new PathToSourceFromName().apply("/badExamples/basics/missingModuleKeyWord.ek9");
    Workspace rtn = new Workspace();
    rtn.addSource(fullPath);
    return rtn;
  };

  @Test
  void testInvalidListenerAndFlags() {
    assertThrows(java.lang.IllegalArgumentException.class, () -> new Ek9Compiler(null, null));
  }

  @Test
  void testInvalidListener() {
    assertThrows(java.lang.IllegalArgumentException.class,
        () -> new Ek9Compiler(null, new CompilerFlags()));
  }

  @Test
  void testInvalidFlags() {
    assertThrows(java.lang.IllegalArgumentException.class,
        () -> new Ek9Compiler((phase, source) -> {
        }, null));
  }

  @ParameterizedTest
  @MethodSource("allCompilationPhases")
  void testSimpleSuccessfulParsing(final CompilationPhase upToPhase) {
    CompilationListener listener = (phase, source) -> {
    };
    var compiler = new Ek9Compiler(listener, new CompilerFlags(upToPhase));

    assertTrue(compiler.compile(validEk9Workspace.get()));
  }

  @Test
  void testSimpleUnSuccessfulParsing() {
    //Not concerned with what the error is.
    CompilationListener listener = (phase, source) -> {
    };
    var compiler = new Ek9Compiler(listener, new CompilerFlags());

    assertFalse(compiler.compile(inValidEk9Workspace.get()));
  }

  private static Stream<CompilationPhase> allCompilationPhases() {
    return Stream.of(CompilationPhase.values());
  }
}
