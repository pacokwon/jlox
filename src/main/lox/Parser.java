package lox;

import java.util.List;
import java.util.ArrayList;

import static lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd())
      statements.add(declaration());
    return statements;
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      // consume if matches
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;

    return peek().type == type;
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type))
      return advance();

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (peek().type == SEMICOLON) return;

      switch (peek().type) {
        case ASSERT:
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private Stmt declaration() {
    if (match(VAR)) return varDeclaration();

    return statement();
  }

  /**
   * statement <- exprStmt
   *            | ifStmt
   *            | assertStmt (*exclusive to this project)
   *            | printStmt
   *            | block
   */
  private Stmt statement() {
    if (match(IF)) return ifStatement();
    if (match(ASSERT)) return assertion();
    if (match(PRINT)) return printStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd())
      statements.add(declaration());

    consume(RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr init = null;
    if (match(EQUAL))
      init = expression();

    consume(SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, init);
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;

    if (match(ELSE))
      elseBranch = statement();

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt assertion() {
    Token assertion = previous();
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after assertion.");
    return new Stmt.Assert(assertion, expr);
  }

  private Stmt printStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(expr);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  private Expr expression() {
    return assignment();
  }

  private Expr assignment() {
    // assignment <- IDENTIFIER = assignment | logic_or
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr right = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, right);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    // logic_or <- logic_and (or logic_or)*
    Expr left = and();

    if (match(OR)) {
      Token operator = previous();
      Expr right = or();
      return new Expr.Logical(left, operator, right);
    }

    return left;
  }

  private Expr and() {
    // logic_and <- equality (and logic_and)*
    Expr left = equality();

    if (match(AND)) {
      Token operator = previous();
      Expr right = and();
      return new Expr.Logical(left, operator, right);
    }

    return left;
  }

  private Expr equality() {
    // equality <- comparison ((!= | ==) comparison)*
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      // is right associative
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    // comparison <- term ((> | >= | < | <=) term)*
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    // term <- factor ((- | +) factor)*
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    // factor <- unary ((/ | *) unary)*
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    // unary <- (! | -) unary | primary
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    // primary <- NUMBER | STRING | true | false | nil | ( expression )
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING))
      return new Expr.Literal(previous().literal);

    if (match(IDENTIFIER))
      return new Expr.Variable(previous());

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }
}
