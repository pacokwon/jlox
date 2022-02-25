package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }

    String outputDir = args[0];
    String baseName = "Expr";
    defineAst(outputDir, baseName, Arrays.asList(
      "Binary     : Expr left, Token operator, Expr right",
      "Grouping   : Expr expression",
      "Literal    : Object value",
      "Unary      : Token operator, Expr right"
    ));

    System.out.printf("%s/%s.java successfully created.\n", outputDir, baseName);
  }

  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    String path = String.format("%s/%s.java", outputDir, baseName);
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package lox");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.printf("abstract class %s {\n", baseName);

    for (String type : types) {
      String[] splitted = type.split(":");

      String className = splitted[0].trim();
      String fields = splitted[1].trim();
      defineType(writer, baseName, className, fields);
    }

    writer.println("}");
    writer.close();
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
      writer.printf("      this.%s = %s\n", name, name);
    }
    writer.println("    }");

    writer.println("  }");
  }
}
