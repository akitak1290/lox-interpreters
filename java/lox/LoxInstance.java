package lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
	private LoxClass klass;
	// States/Properties
	private final Map<String, Object> fields = new HashMap<>();

	LoxInstance(LoxClass klass) {
		this.klass = klass;
	}

	// Property getter
	Object get(Token name) {
		// Look for field
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}

		// else look for method
		LoxFunction method = klass.findMethod(name.lexeme);
		if (method != null) return method.bind(this);

		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}

	@Override
	public String toString() {
		return klass.name + " instance";
	}
}
