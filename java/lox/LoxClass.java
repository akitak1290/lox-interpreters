package lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
	final String name;
	private final Map<String, LoxFunction> methods;

	LoxClass(String name, Map<String, LoxFunction> methods) {
		this.name = name;
		this.methods = methods;
	}

	LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}
		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		// "Create a new class" is just create a LoxInstance object
		// and return it.
		LoxInstance instance = new LoxInstance(this);
		
		LoxFunction initializer = findMethod("init");
		if (initializer != null) {
			// Call constructor
			// bind the constuctor to the current object and call it
			initializer.bind(instance).call(interpreter, arguments);
		}
		return instance;
	}

	@Override
	public int arity() {
		// How many arguments required for creating a Lox class
		// is how many arguments required for the init method
		LoxFunction initializer = findMethod("init");
		if (initializer == null) return 0;
		return initializer.arity();
	}
}
