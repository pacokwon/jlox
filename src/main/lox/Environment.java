package lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  private final Map<String, Object> values = new HashMap<>();
  final Environment enclosing;

  Environment() {
    this.enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  void define(String name, Object value) {
    values.put(name, value);
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme))
      return values.get(name.lexeme);

    if (enclosing != null)
      return enclosing.get(name);

    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  private Environment ancestor(int distance) {
    Environment env = this;

    for (int i = 0; i < distance; i++)
      env = env.enclosing;

    return env;
  }

  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }
}
