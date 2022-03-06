package lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  Stmt.Function declaration;
  Environment closure;

  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment env = new Environment(closure);
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
