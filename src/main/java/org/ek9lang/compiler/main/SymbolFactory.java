package org.ek9lang.compiler.main;

import java.util.Optional;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.internals.ParsedModule;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.AggregateWithTraitsSymbol;
import org.ek9lang.compiler.symbol.ConstantSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.IScopedSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.ServiceOperationSymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.utils.UniqueIdGenerator;

/**
 * Just a factory for all types of EK9 symbol.
 * Ensures that newly created symbols are initialised correctly.
 */
public class SymbolFactory {

  private final ParsedModule parsedModule;

  private final Consumer<Object> checkContextNotNull = ctx -> AssertValue.checkNotNull("CTX cannot be null", ctx);

  /**
   * Create a new symbol factory for use with the parsedModule.
   */
  public SymbolFactory(ParsedModule parsedModule) {
    AssertValue.checkNotNull("Parsed Module cannot be null", parsedModule);
    this.parsedModule = parsedModule;
  }

  /**
   * Create a new aggregate that represents an EK9 program.
   */
  public AggregateSymbol newProgram(EK9Parser.MethodDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate program name", ctx.identifier());

    //TODO Need to deal with the application setup part.
    //We underscore to class name and main processing method don't clash and look like a constructor.
    String programName = "_" + ctx.identifier().getText();
    AggregateSymbol program = new AggregateSymbol(programName, parsedModule.getModuleScope());
    configureAggregate(program, ctx.start);
    //Not sure what genus to set this to at the moment.
    return program;
  }

  /**
   * Create a new aggregate that represents an EK9 class.
   */
  public AggregateWithTraitsSymbol newClass(EK9Parser.ClassDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate class name", ctx.Identifier());
    String className = ctx.Identifier().getText();
    final var newClass = newAggregateWithTraitsSymbol(className, ctx.start, parsedModule.getModuleScope());
    newClass.setOpenForExtension(ctx.ABSTRACT() != null);
    newClass.setGenus(ISymbol.SymbolGenus.CLASS);

    return newClass;
  }

  /**
   * Create a new aggregate that represents an EK9 component.
   */
  public AggregateWithTraitsSymbol newComponent(EK9Parser.ComponentDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate component name", ctx.Identifier());
    String componentName = ctx.Identifier().getText();
    var component = newAggregateWithTraitsSymbol(componentName, ctx.start, parsedModule.getModuleScope());
    component.setOpenForExtension(ctx.ABSTRACT() != null);
    component.setGenus(ISymbol.SymbolGenus.COMPONENT);

    return component;
  }

  /**
   * Create a new aggregate that represents an EK9 trait.
   */
  public AggregateWithTraitsSymbol newTrait(EK9Parser.TraitDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate trait name", ctx.Identifier());

    String traitName = ctx.Identifier().getText();
    AggregateWithTraitsSymbol trait = newAggregateWithTraitsSymbol(traitName, ctx.start, parsedModule.getModuleScope());
    configureAggregate(trait, ctx.start);

    trait.setGenus(ISymbol.SymbolGenus.CLASS_TRAIT);
    //All traits are designed to be open to extending and use/override.
    trait.setOpenForExtension(true);

    return trait;
  }

  /**
   * Create a new aggregate that represents an EK9 record.
   */
  public AggregateSymbol newRecord(EK9Parser.RecordDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate record name", ctx.Identifier());
    String recordName = ctx.Identifier().getText();
    AggregateSymbol newRecord = new AggregateSymbol(recordName, parsedModule.getModuleScope());
    configureAggregate(newRecord, ctx.start);
    newRecord.setGenus(ISymbol.SymbolGenus.RECORD);
    return newRecord;
  }

  /**
   * Create a new aggregate that represents an EK9 function.
   */
  public FunctionSymbol newFunction(EK9Parser.FunctionDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    AssertValue.checkNotNull("Failed to locate function name", ctx.Identifier());

    String functionName = ctx.Identifier().getText();
    FunctionSymbol function = new FunctionSymbol(functionName, parsedModule.getModuleScope());
    configureSymbol(function, ctx.start);

    //A function can be both pure and abstract - in this case it is establishing a 'contract' that the
    //implementation must also be pure!
    //This is so that uses of abstract concepts can be used to ensure that there are no side effects.
    if (ctx.PURE() != null) {
      function.setMarkedPure(true);
    }

    //While it maybe a function we need to know if it is abstract or not
    if (ctx.ABSTRACT() == null) {
      function.setGenus(ISymbol.SymbolGenus.FUNCTION);
    } else {
      function.setGenus(ISymbol.SymbolGenus.FUNCTION_TRAIT);
      function.setMarkedAbstract(true);
    }

    //make a note of this - could be null
    function.setReturningParamContext(ctx.operationDetails().returningParam());

    return function;
  }

  /**
   * Create a new aggregate that represents EK9 text construct.
   */
  public AggregateSymbol newText(EK9Parser.TextDeclarationContext ctx, String forLanguage) {
    //an error will have been created for the developer as language does not conform.
    if (forLanguage != null) {
      String textName = ctx.Identifier().getText() + "_" + forLanguage;
      AggregateSymbol text = new AggregateSymbol(textName, parsedModule.getModuleScope());
      configureAggregate(text, ctx.start);
      text.setGenus(ISymbol.SymbolGenus.TEXT);
      text.putSquirrelledData("LANG", forLanguage);

      return text;
    }
    //return a place holder
    return new AggregateSymbol(ctx.Identifier().getText(), parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 text body - this is represented by a method.
   */
  public MethodSymbol newTextBody(EK9Parser.TextBodyDeclarationContext ctx, IScope scope) {
    String methodName = ctx.Identifier().getText();

    MethodSymbol method = new MethodSymbol(methodName, scope);
    configureSymbol(method, ctx.start);

    method.setOverride(true);
    method.setMarkedAbstract(false);
    method.setMarkedPure(true);

    method.setOperator(false);
    //Always returns a String - no need or ability to declare it.
    method.setType(scope.resolve(new TypeSymbolSearch("org.ek9.lang::String")));

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 service.
   */
  public AggregateSymbol newService(EK9Parser.ServiceDeclarationContext ctx) {
    String serviceName = ctx.Identifier().getText();
    AggregateSymbol service = new AggregateSymbol(serviceName, parsedModule.getModuleScope());
    configureAggregate(service, ctx.start);
    service.setGenus(ISymbol.SymbolGenus.SERVICE);
    return service;
  }

  /**
   * Create a new aggregate that represents an EK9 service operation - a specialised method.
   */
  public ServiceOperationSymbol newServiceOperation(EK9Parser.ServiceOperationDeclarationContext ctx, IScope scope) {
    boolean operator = false;
    String methodName = null;
    if (ctx.operator() != null) {
      methodName = ctx.operator().getText();
      operator = true;
    } else if (ctx.identifier() != null) {
      methodName = ctx.identifier().getText();
    }

    ServiceOperationSymbol serviceOperation = new ServiceOperationSymbol(methodName, scope);
    configureSymbol(serviceOperation, ctx.start);

    serviceOperation.setOverride(false);
    serviceOperation.setMarkedAbstract(false);
    serviceOperation.setMarkedPure(false);
    serviceOperation.setOperator(operator);

    serviceOperation.putSquirrelledData("URIPROTO", ctx.Uriproto().getText());
    return serviceOperation;
  }

  /**
   * Create a new aggregate that represents an EK9 application.
   */
  public AggregateSymbol newApplication(EK9Parser.ApplicationDeclarationContext ctx) {
    String applicationName = ctx.Identifier().getText();
    AggregateSymbol application = new AggregateSymbol(applicationName, parsedModule.getModuleScope());
    configureAggregate(application, ctx.start);

    //By default a general program application
    application.setGenus(ISymbol.SymbolGenus.GENERAL_APPLICATION);
    EK9Parser.ApplicationBlockContext applicationBlockContext = (EK9Parser.ApplicationBlockContext) ctx.getParent();

    if (applicationBlockContext.appType != null && applicationBlockContext.appType.getType() == EK9Parser.SERVICE) {
      application.setGenus(ISymbol.SymbolGenus.SERVICE_APPLICATION);
    }

    return application;
  }

  /**
   * Create a new aggregate that represents an EK9 dynamic class.
   */
  public AggregateWithTraitsSymbol newDynamicClass(EK9Parser.DynamicClassDeclarationContext ctx) {
    //Name is optional - if not present then generate a dynamic value.
    if (ctx.Identifier() != null) {
      return newAggregateWithTraitsSymbol(ctx.Identifier().getText(), ctx.start, parsedModule.getModuleScope());
    }

    //So if not named - we generate a dynamic unique name and use that.
    return newAggregateWithTraitsSymbol("_Class_" + UniqueIdGenerator.getNewUniqueId(), ctx.start,
        parsedModule.getModuleScope());
  }

  /**
   * Create a new aggregate that represents an EK9 operator, uses a method for this.
   */
  public MethodSymbol newOperator(EK9Parser.OperatorDeclarationContext ctx, IScopedSymbol scopedSymbol) {
    String methodName = ctx.operator().getText();
    MethodSymbol method = new MethodSymbol(methodName, scopedSymbol);

    configureSymbol(method, ctx.start);

    method.setOverride(ctx.OVERRIDE() != null);
    method.setMarkedAbstract(ctx.ABSTRACT() != null);
    method.setMarkedPure(ctx.PURE() != null);

    //Set this as default unless we have a returning section
    method.setType(scopedSymbol.resolve(new TypeSymbolSearch("org.ek9.lang::Void")));

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 class/component method.
   */
  public MethodSymbol newMethod(EK9Parser.MethodDeclarationContext ctx, IScopedSymbol scopedSymbol) {
    //So now we should have an aggregate we are adding this method into.
    String methodName = ctx.identifier().getText();
    MethodSymbol method = new MethodSymbol(methodName, scopedSymbol);

    configureSymbol(method, ctx.start);

    method.setOverride(ctx.OVERRIDE() != null);
    method.setMarkedAbstract(ctx.ABSTRACT() != null);
    method.setMarkedPure(ctx.PURE() != null);

    if (ctx.accessModifier() != null) {
      method.setAccessModifier(ctx.accessModifier().getText());
    }

    if (method.getName().equals(scopedSymbol.getScopeName())) {
      //looks like a constructor
      method.setConstructor(true);
      method.setType(scopedSymbol);
    } else {
      //Set this as default unless we have a returning section
      method.setType(scopedSymbol.resolve(new TypeSymbolSearch("org.ek9.lang::Void")));
    }

    return method;
  }

  /**
   * Create a new aggregate that represents an EK9 type, constrained or enumeration.
   */
  public AggregateSymbol newType(EK9Parser.TypeDeclarationContext ctx) {
    String newTypeName = ctx.Identifier().getText();
    AggregateSymbol clazz = new AggregateSymbol(newTypeName, parsedModule.getModuleScope());
    configureAggregate(clazz, ctx.start);

    if (ctx.typeDef() != null) {
      clazz.setGenus(ISymbol.SymbolGenus.CLASS_CONSTRAINED);
    } else if (ctx.enumerationDeclaration() != null) {
      clazz.setGenus(ISymbol.SymbolGenus.CLASS_ENUMERATION);
      //TODO addSyntheticEnumerationMethods
    }
    return clazz;
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable.
   */
  public VariableSymbol newLoopVariable(EK9Parser.ForLoopContext ctx) {
    checkContextNotNull.accept(ctx);
    return newLoopVariable(ctx.identifier());
  }

  /**
   * Create a new aggregate that represents an EK9 loop variable for a range.
   */
  public VariableSymbol newLoopVariable(EK9Parser.ForRangeContext ctx) {
    checkContextNotNull.accept(ctx);
    return newLoopVariable(ctx.identifier());
  }

  private VariableSymbol newLoopVariable(EK9Parser.IdentifierContext identifier) {
    checkContextNotNull.accept(identifier);
    var variable = newVariable(identifier, false);
    //Make note is a loop variable and EK9 will initialise it, developer cannot mutate it.
    variable.setLoopVariable(true);
    variable.setInitialisedBy(identifier.start);
    variable.setNotMutable();
    return variable;
  }

  /**
   * Just a declaration of a variable by itself - i.e. without an assignment.
   */
  public VariableSymbol newVariable(EK9Parser.VariableOnlyDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);
    return newVariable(ctx.identifier(), ctx.QUESTION() != null);
  }

  /**
   * Create and initialise a new variable symbol.
   * Typically, a variable like name <- "Steve", so 'name' is the variable.
   */
  public VariableSymbol newVariable(final EK9Parser.VariableDeclarationContext ctx) {
    checkContextNotNull.accept(ctx);

    return newVariable(ctx.identifier(), ctx.QUESTION() != null);
  }

  private VariableSymbol newVariable(final EK9Parser.IdentifierContext identifier, final boolean nullAllowed) {
    AssertValue.checkNotNull("Failed to locate variable name", identifier);

    VariableSymbol variable = new VariableSymbol(identifier.getText());
    configureSymbol(variable, identifier.start);
    variable.setNullAllowed(nullAllowed);
    return variable;
  }

  /**
   * Create a new aggregate that represents an EK9 literal value.
   */
  public ConstantSymbol newLiteral(Token start, String name) {
    AssertValue.checkNotNull("Start token cannot be null", start);
    AssertValue.checkNotNull("Name cannot be null", name);

    ConstantSymbol literal = new ConstantSymbol(name, true);
    configureSymbol(literal, start);
    //You cannot set this to any other value.
    literal.setNotMutable();

    return literal;
  }

  private AggregateWithTraitsSymbol newAggregateWithTraitsSymbol(String className, Token start, IScope scope) {
    AggregateWithTraitsSymbol clazz = new AggregateWithTraitsSymbol(className, scope);
    configureAggregate(clazz, start);
    clazz.setGenus(ISymbol.SymbolGenus.CLASS);
    return clazz;
  }

  private void configureAggregate(AggregateSymbol aggregate, Token start) {
    aggregate.setModuleScope(parsedModule.getModuleScope());
    configureSymbol(aggregate, start);
  }

  private void configureSymbol(ISymbol symbol, Token start) {
    symbol.setParsedModule(Optional.of(parsedModule));
    symbol.setSourceToken(start);
    if (parsedModule.isExternallyImplemented()) {
      symbol.putSquirrelledData("EXTERN", "TRUE");
    }
  }

}
