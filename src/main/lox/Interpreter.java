package lox;

import java.util.List;

import static lox.TokenType.*;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
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

  private Object evaluate(Expr expr) {
    return expr.accept(this);
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
        checkNumberOperand(expr.operator, left, right);
        return isEqual(left, right);
      case BANG_EQUAL:
        checkNumberOperand(expr.operator, left, right);
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
