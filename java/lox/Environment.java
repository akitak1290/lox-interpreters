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
	// A hashmap to hold the identifiers and their values
	// All identifiders with the same name must refer
	// to the same value so we use raw string for keys
	private final Map<String, Object> values = new HashMap<>();

	/*
	 * Variable loop up method
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

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	/*
	 * Variable definition
	 * Binds new name to a value
	 * !Don't check if key exists to
	 * allow redefining identifiers
	 */
	void define(String name, Object value) {
		values.put(name, value);
	}
}
