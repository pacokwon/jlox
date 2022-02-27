package lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
    R visitVarStmt(Var stmt);
    R visitBlockStmt(Block stmt);
  }

  abstract <R> R accept(Visitor<R> visitor);

  static class Expression extends Stmt {
    final Expr expression;

    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }
  static class Print extends Stmt {
    final Expr expression;

    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }
  static class Var extends Stmt {
    final Token name;
    final Expr initializer;

    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }
  }
  static class Block extends Stmt {
    final List<Stmt> statements;

    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }
}
