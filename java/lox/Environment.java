package lox;

import java.util.HashMap;
import java.util.Map;

/*
 * Implements storing variables/identifiers declaration
 *
 * Design choice:
 * 	- A variable statement can redefine an exisiting
 * 	variable instead of throwing an error.
 */
class Environment {
	final Environment enclosing;
	// A hashmap to hold the identifiers and their values
	// All identifiders with the same name must refer
	// to the same value so we use raw string for keys
	private final Map<String, Object> values = new HashMap<>();

	// For global scope env
	Environment() {
		enclosing = null;
	}

	// For local scope
	// @enclosing Environment outer scope
	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}
	/*
	 * Variable look up method
	 * Returns the object allows the caller to
	 * use it in another expression before the object
	 * is defined (but must be declared)
	 *
	 * @return Object expression that @name points to
	 * @throw RuntimError if @name is not found
	 */
	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		// Search outer scope if not found in this scope
		if (enclosing != null) return enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/*
	 * Variable assignment
	 * Assign new value to existing name
	 * 
	 * @throw RuntimeError if assign no name that
	 * doesn't exist
	 */
	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}

		// Search outer scope if not found in this scope
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/*
	 * Variable definition
	 * Binds new name
	 * Always bind to innermost scope
	 * !Don't check if key exists to
	 * allow redefining identifiers
	 */
	void define(String name, Object value) {
		values.put(name, value);
	}

	/*
	 * Variable loop up method at a certain
	 * scope
	 *
	 * ! This class has faith that the variable exist
	 * ! as Resolver should have found it!
	 *
	 * @return Object expression that @name points
	 * 	   to at scope of @distance
	 */
	Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}
	/*
	 * Variable assignment at a specific scope
	 */
	void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}

	/*
	 * Return the parent environment that is
	 * @disntance away from the current one
	 *
	 * @return Environment
	 */
	Environment ancestor(int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}

		return environment;
	}
}
