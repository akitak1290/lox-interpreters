package lox;

import java.util.List;

/*
 * Implement Lox's Function class
 * This doesn't care about the name of the function,
 * only the environment of the caller needs to know that
 * (see in interpreter...)
 */
class LoxFunction implements LoxCallable {
	// Cool, a has-a relationship with Stmt.Function
	private final Stmt.Function declaration;
	private final Environment closure;

	LoxFunction(Stmt.Function declaration, Environment closure) {
		this.closure = closure;
		this.declaration = declaration;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(closure);
		for (int i = 0; i < declaration.params.size(); i++) {
			// Bind the argument name from the func decl
			// and the value passed to this method.
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}

		
		// Handles return statement
		try {
			// ExecuteBlock would discard the environment when its
			// done executing and retore the higher one
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			return returnValue.value;
		}
		return null;
	}

	@Override
	public int arity() {
		return declaration.params.size();
	}

	@Override
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
