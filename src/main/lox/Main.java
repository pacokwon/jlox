package lox;

public class Main {
  public static void main(String[] args) {
    Expr expr = new Expr.Binary(
      new Expr.Unary(
        new Token(TokenType.MINUS, "-", null, 1),
        new Expr.Literal(123)
      ),
      new Token(TokenType.STAR, "*", null, 1),
      new Expr.Grouping(new Expr.Literal(45.67))
    );
    System.out.println(new AstPrinter().print(expr));
  }
}
