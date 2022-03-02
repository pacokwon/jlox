package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: generate_ast <base name> <output directory>");
      System.exit(64);
    }

    String baseName = args[0];
    String outputDir = args[1];
    List<String> types = fetchTypes(baseName);

    defineAst(outputDir, baseName, types);

    System.out.printf("%s/%s.java successfully created.\n", outputDir, baseName);
  }

  private static List<String> fetchTypes(String baseName) {
    if (baseName.equals("Expr"))
      return Arrays.asList(
        "Assign     : Token name, Expr expr",
        "Logical    : Expr left, Token operator, Expr right",
        "Binary     : Expr left, Token operator, Expr right",
        "Grouping   : Expr expression",
        "Literal    : Object value",
        "Unary      : Token operator, Expr right",
        "Call       : Expr callee, Token paren, List<Expr> arguments",
        "Variable   : Token name"
      );
    else if (baseName.equals("Stmt"))
      return Arrays.asList(
        "Expression : Expr expression",
        "Function   : Token name, List<Token> params, List<Stmt> body",
        "Assert     : Token assertion, Expr expression",
        "Print      : Expr expression",
        "Var        : Token name, Expr initializer",
        "Block      : List<Stmt> statements",
        "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
        "While      : Expr condition, Stmt body",
        "Return     : Token keyword, Expr value"
      );
    else
      throw new RuntimeException("Invalid baseName. Must be either \"Expr\" or \"Stmt\"");
  }

  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = String.format("%s/%s.java", outputDir, baseName);
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.printf("abstract class %s {\n", baseName);

    defineVisitor(writer, baseName, types);

    // accept method. every subclass should implement this method for a visitor
    writer.println();
    writer.printf("  abstract <R> R accept(Visitor<R> visitor);\n");

    writer.println();
    for (String type : types) {
      String[] splitted = type.split(":");

      String className = splitted[0].trim();
      String fields = splitted[1].trim();
      defineType(writer, baseName, className, fields);
    }

    writer.println("}");
    writer.close();
  }

  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");

    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.printf("    R visit%s%s(%s %s);\n", typeName, baseName, typeName, baseName.toLowerCase());
    }
    writer.println("  }");
  }

  private static void defineType(
      PrintWriter writer, String baseName,
      String className, String fieldList) {
    writer.printf("  static class %s extends %s {\n", className, baseName);

    // fields
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      writer.printf("    final %s;\n", field);
    }
    writer.println();

    // constructor
    writer.printf("    %s(%s) {\n", className, fieldList);

    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.printf("      this.%s = %s;\n", name, name);
    }
    writer.println("    }");

    // override accept function
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.printf ("      return visitor.visit%s%s(this);\n", className, baseName);
    writer.println("    }");

    writer.println("  }");
  }
}
