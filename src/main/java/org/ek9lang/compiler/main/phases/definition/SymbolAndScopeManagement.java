package org.ek9lang.compiler.main.phases.definition;

import java.util.Optional;
import org.antlr.v4.runtime.tree.ParseTree;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.compiler.symbol.ScopeStack;
import org.ek9lang.compiler.symbol.StackConsistencyScope;
import org.ek9lang.compiler.symbol.support.SymbolChecker;
import org.ek9lang.core.exception.AssertValue;

/**
 * Used as external helper to record symbols and scopes across the parser.
 * They are held in the parsedModule against specific nodes in the parseTree.
 * This enabled later phases to look up the symbols and scopes when it encounters
 * the same node in the parseTree. In this way the symbols get 'fleshed out'
 * with more details in each of the compiler phases.
 * This does push scopes on to the scope stack - that the listener uses.
 * IMPORTANT concepts here:
 * 1. The ParsedModule keeps a record of a Symbol against a ParseTree node
 * 2. The ParsedModule keeps a record of a Scope against a ParseTree node
 * 3. The scopeStack keeps a dynamic position of the current scope stuff is being added to.
 * But the scopeStack is ephemeral - it grows and shrinks based on the language constructs the
 * listener encounters, as stuff is added in - that scope become richer. But when the end of the
 * scope is encountered - it is popped off the scopeStack.
 * All would be lost! - But for the fact that the scope was registered against a ParseTree node in the
 * ParsedModule. Hence, it lives on - with all the sub scopes and symbols.
 * This information in the ParsedModule will be enriched further in additional and different passes.
 */
public class SymbolAndScopeManagement {
  private final ParsedModule parsedModule;
  private final ScopeStack scopeStack;

  /**
   * Create a new instance for symbol and scope management.
   */
  public SymbolAndScopeManagement(ParsedModule parsedModule, ScopeStack scopeStack) {
    AssertValue.checkNotNull("ParsedModule cannot be null", parsedModule);
    AssertValue.checkNotNull("ScopeStack cannot be null", scopeStack);

    this.parsedModule = parsedModule;
    this.scopeStack = scopeStack;
  }

  /**
   * To be used to ensure that a scope has been pushed on to the scopeStack.
   */
  public void enterScope(IScope scope) {
    scopeStack.push(scope);
  }

  /**
   * Normally called at the point where the parser listener exits a scope.
   * The scope will be popped off the scopeStack to reveal the parent scope.
   */
  public void exitScope() {
    scopeStack.pop();
  }

  /**
   * Provides access to the top of the scope stack.
   */
  public IScope getTopScope() {
    return scopeStack.peek();
  }

  /**
   * Navigates back up the scope stack to find the first match of the scope type passed in.
   */
  public Optional<IScope> traverseBackUpStack(final IScope.ScopeType scopeType) {
    return scopeStack.traverseBackUpStack(scopeType);
  }

  public Optional<ISymbol> resolveOrDefine(final PossibleGenericSymbol parameterisedSymbol) {
    return parsedModule.getModuleScope().resolveOrDefine(parameterisedSymbol);
  }

  /**
   * A new symbol has been encountered and is defined within the current scope in the scope stack
   * and recorded in the parsedModule against the appropriate node.
   */
  public void enterNewSymbol(ISymbol symbol, ParseTree node) {
    scopeStack.peek().define(symbol);
    recordSymbol(symbol, node);
  }

  public ISymbol getRecordedSymbol(final ParseTree node) {
    return parsedModule.getRecordedSymbol(node);
  }

  public IScope getRecordedScope(final ParseTree node) {
    return parsedModule.getRecordedScope(node);
  }

  public void recordSymbol(ISymbol symbol, ParseTree node) {
    parsedModule.recordSymbol(node, symbol);
  }

  /**
   * For literals we only record in the parsedModule.
   */
  public void enterNewLiteral(ISymbol symbol, ParseTree node) {
    recordSymbol(symbol, node);
  }

  /**
   * Create a new constant as declared in the constants section and records the symbol.
   */
  public void enterNewConstant(final ISymbol symbol, final ParseTree node, final SymbolChecker symbolChecker) {
    var moduleScope = parsedModule.getModuleScope();
    if (moduleScope.defineOrError(symbol, symbolChecker)) {
      recordSymbol(symbol, node);
    }
  }

  /**
   * To be used when defining a high level symbol at module scope.
   */
  public void enterModuleScopedSymbol(final IScopedSymbol symbol, final ParseTree node,
                                      final SymbolChecker symbolChecker) {
    var scopeType = symbol.getScopeType();
    var moduleScope = parsedModule.getModuleScope();
    if (moduleScope.defineOrError(symbol, symbolChecker)) {
      enterNewScopedSymbol(symbol, node);
    } else {
      recordScopeForStackConsistency(new StackConsistencyScope(moduleScope, scopeType), node);
    }
  }

  /**
   * Record a new scoped symbol in the current scope on the stack.
   * Also records the symbol as both a scope and a symbol in the parsedModule.
   */
  public void defineScopedSymbol(IScopedSymbol symbol, ParseTree node) {
    AssertValue.checkNotNull("Looks like your enter/exits are not balanced", scopeStack.peek());

    scopeStack.peek().define(symbol);
    enterNewScopedSymbol(symbol, node);
  }

  /**
   * Enter a new scoped symbol and record in parsed module as a symbol and as a scope.
   * Then push the scope on to the working scope stack.
   */
  public void enterNewScopedSymbol(IScopedSymbol symbol, ParseTree node) {
    recordSymbol(symbol, node);
    enterNewScope(symbol, node);
  }

  /**
   * Enter a new scope and record in both the parsed module and push on to the working scope stack.
   */
  public void enterNewScope(IScope scope, ParseTree node) {
    recordScopeForStackConsistency(scope, node);
  }

  /**
   * There are times in parsing/listening when symbols are already defined (due to a developer error).
   * During this situation we cannot define a new node, but we can detect the duplicate (display errors).
   * But so the scope stack does not get corrupted - it is important to still push the existing scope on
   * to the stack. In this way when the stack is popped everything works out.
   */
  public void recordScopeForStackConsistency(IScope scope, ParseTree node) {
    parsedModule.recordScope(node, scope);
    enterScope(scope);
  }
}
