package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolver class.
 * The resolver runs after the parser, and before the interpreter.
 * Its role is to resolve all variables once, before actual evaluation happens.
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  // `scopes` is initially empty. which means that the global scope is
  // not considered by this variable.
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }
  private FunctionType currentFunction = FunctionType.NONE;
  private enum ClassType {
    NONE,
    CLASS
  }
  private ClassType currentClass = ClassType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  void resolve(Expr expr) {
    expr.accept(this);
  }

  void resolve(Stmt statement) {
    statement.accept(this);
  }

  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements)
      resolve(statement);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();

    // `false` means that we are NOT finished resolving this variable
    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    Map<String, Boolean> scope = scopes.peek();

    // `true` means that we are finished resolving this variable.
    // it is fully initialized and available for use.
    scope.put(name.lexeme, true);
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        // how many levels do we have to search upwards
        // to find this variable?
        // if it's at the top of the stack, it will be 0,
        // the next 1, and so on..
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void resolveFunction(Stmt.Function stmt, FunctionType type) {
    FunctionType enclosing = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : stmt.params) {
      declare(param);
      define(param);
    }

    resolve(stmt.body);
    endScope();

    currentFunction = enclosing;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    // declare and define are always ran,
    // whether there is an initializer or not.
    // it is in between `declare` and `define`, that
    // the value of stmt.name is initialized as `false`.
    // and that is when initializer is being resolved.
    declare(stmt.name);
    if (stmt.initializer != null)
      resolve(stmt.initializer);

    define(stmt.name);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitAssertStmt(Stmt.Assert stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null)
      resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction != FunctionType.FUNCTION)
      Lox.error(stmt.keyword, "Can't return from top-level code.");

    if (stmt.value != null) {
      if (currentFunction != FunctionType.INITIALIZER)
        Lox.error(stmt.keyword, "Can't return a value from an initializer.");

      resolve(stmt.value);
    }
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    // class names can be used as values too,
    // so we declare and define it.
    declare(stmt.name);
    define(stmt.name);

    beginScope();
    scopes.peek().put("this", true);
    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.equals("init"))
        declaration = FunctionType.INITIALIZER;
      resolveFunction(method, declaration);
    }
    endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.expr);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr arg : expr.arguments)
      resolve(arg);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE)
      // variable is declared but not defined.
      Lox.error(expr.name, "Can't read local variables in its own initializer.");

    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    resolveLocal(expr, expr.keyword);
    return null;
  }
}
