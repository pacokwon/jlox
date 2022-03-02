package lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  Stmt.Function declaration;

  LoxFunction(Stmt.Function declaration) {
    this.declaration = declaration;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment env = new Environment(interpreter.globals);
    for (int i = 0; i < declaration.params.size(); i++)
      env.define(declaration.params.get(i).lexeme, arguments.get(i));

    try {
      interpreter.executeBlock(declaration.body, env);
    } catch (Return returnValue) {
      return returnValue.value;
    }
    return null;
  }
}
