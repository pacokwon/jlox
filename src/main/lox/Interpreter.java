package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double) System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() {
        return "<native function clock>";
      }
    });
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  private Object lookupVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null)
      return environment.getAt(distance, name.lexeme);
    else
      return globals.get(name);
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements,
                            Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt stmt : statements)
        execute(stmt);
    } finally {
      this.environment = previous;
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Void visitAssertStmt(Stmt.Assert stmt) {
    Object val = evaluate(stmt.expression);
    if (!isTruthy(val))
      throw new RuntimeError(stmt.assertion, String.format("%s is not truthy", stringify(val)));
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    // this is for methods, so that they have a mapping to this class.
    environment.define(stmt.name.lexeme, null);

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function = new LoxFunction(method, environment);
      methods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
    environment.assign(stmt.name, klass);
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object val = null;
    if (stmt.initializer != null) {
      val = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, val);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object val = evaluate(stmt.expression);
    System.out.println(stringify(val));
    return null;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(this.environment));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null)
      value = evaluate(stmt.value);

    throw new Return(value);
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // capture the current environment
    LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    Object condVal = evaluate(stmt.condition);

    if (isTruthy(condVal))
      execute(stmt.thenBranch);
    else if (stmt.elseBranch != null)
      execute(stmt.elseBranch);

    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition)))
      execute(stmt.body);
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object val = evaluate(expr.expr);
    Integer distance = locals.get(expr);

    if (distance != null)
      environment.assignAt(distance, expr.name, val);
    else
      globals.assign(expr.name, val);

    return val;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object lval = evaluate(expr.left);

    if (expr.operator.type == OR) {
      if (isTruthy(lval))
        return lval;
    } else {
      if (!isTruthy(lval))
        return lval;
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case PLUS:
        if (left instanceof Double && right instanceof Double)
          return (double)left + (double)right;
        if (left instanceof String && right instanceof String)
          return (String)left + (String)right;
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case MINUS:
        checkNumberOperand(expr.operator, left, right);
        return (double)left - (double)right;
      case STAR:
        checkNumberOperand(expr.operator, left, right);
        return (double)left * (double)right;
      case SLASH:
        checkNumberOperand(expr.operator, left, right);
        return (double)left / (double)right;
      case GREATER:
        checkNumberOperand(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperand(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperand(expr.operator, left, right);
        return (double)left <= (double)right;
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case BANG_EQUAL:
        return !isEqual(left, right);
    }

    throw new RuntimeError(expr.operator, "Unsupported operator.");
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr arg : expr.arguments)
      arguments.add(evaluate(arg));

    if (!(callee instanceof LoxCallable))
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");

    LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity())
      throw new RuntimeError(expr.paren, String.format("Expected %d arguments but got %d.", function.arity(), arguments.size()));

    return function.call(this, arguments);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }

    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookupVariable(expr.name, expr);
  }

  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance)
      return ((LoxInstance) object).get(expr.name);

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  @Override
  public Object visitSetExpr(Expr.Set expr) {
    Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance))
      throw new RuntimeError(expr.name, "Only instances have fields.");

    Object value = evaluate(expr.value);
    ((LoxInstance)object).set(expr.name, value);
    return value;
  }

  // only false and nil are falsy. all others are truthy
  private boolean isTruthy(Object val) {
    if (val == null) return false;
    if (val instanceof Boolean) return (boolean)val;
    return true;
  }

  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;

    return left.equals(right);
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperand(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private String stringify(Object value) {
    if (value == null) return "nil";
    if (value instanceof Double) {
      String text = value.toString();
      if (text.endsWith(".0"))
        text = text.substring(0, text.length() - 2);
      return text;
    }
    return value.toString();
  }
}
