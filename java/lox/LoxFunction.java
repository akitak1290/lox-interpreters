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

	private final boolean isInitializer; // is the function a constructor?

	LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
		this.isInitializer = isInitializer;
		this.closure = closure;
		this.declaration = declaration;
	}

	/*
	 * Create a new LoxFunction from current Stmt.Function
	 * in a sub-environment
	 * closure -> sub-environemtn -> new function's environment
	 * The new sub-environment will have one identifier "this"
	 * that points to @instance
	 */
	LoxFunction bind(LoxInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(declaration, environment, isInitializer);
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
			if (isInitializer) return closure.getAt(0, "this");

			return returnValue.value;
		}

		// A special case for constructor to always return `this`
		if (isInitializer) return closure.getAt(0, "this");
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
