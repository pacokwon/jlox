package lox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;

public class Lox {
  static boolean hadError = false;
  static boolean hadRuntimeError = false;
  private static final Interpreter interpreter = new Interpreter();

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));

    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null)
        return;
      run(line);

      hadError = false;
    }
  }

  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();
    Parser parser = new Parser(tokens);
    Expr expr = parser.parse();

    if (hadError) return;

    interpreter.interpret(expr);
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF)
      report(token.line, " at end", message);
    else
      report(token.line, " at '" + token.lexeme + "'", message);
  }

  private static void report(int line, String where, String message) {
    System.err.printf("[line %d] Error %s: %s\n", line, where, message);
    hadError = true;
  }

  static void runtimeError(RuntimeError error) {
    System.err.printf("%s\n[line %d]\n", error.getMessage(), error.token.line);
    hadRuntimeError = true;
  }
}
