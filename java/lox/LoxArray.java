package lox;

import java.util.List;

// This class is weird because it can't be the same as user
// defined LoxClass. The latter is built using ast with Stmt
// and Expr... that would be a hassle to simulate...
//
// This class tries to do just that without
// ast
class LoxArray extends LoxInstance {
	private final Object[] elements;

	LoxCallable getElement;
	LoxCallable setElement;

	LoxArray(int size) {
		super(null);
		// Would this needs to check for OutOfMemoryError?
		elements = new Object[size];

		initCallables();
	}

	LoxArray(Object first) {
		super(null);
		elements = new Object[1];
		elements[0] = first;
		initCallables();
	}

	private void initCallables() {	
		getElement = new LoxCallable() {
			@Override
			public int arity() {
				return 1;
			}
			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				Double index = scaryCastNumber(arguments.get(0));
				return index == null ? null : elements[index.intValue()];
			}
		};
		setElement = new LoxCallable() {
			@Override
			public int arity() {
				return 2;
			}
			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				Double index = scaryCastNumber(arguments.get(0));

				if (index == null) return index;

				Object value = arguments.get(1);
				return elements[index.intValue()] = value;
			}
		};
	}

	private Double scaryCastNumber(Object value) {
		if (value instanceof Double) return (double)value;
		else if (value instanceof Integer) return ((Integer)value).doubleValue();
		else return null; // this should never be reached
	}

	@Override
	Object get(Token name) {
		if (name.lexeme.equals("get")) {
			return getElement;
		} else if (name.lexeme.equals("set")) {
			return setElement;
		} else if (name.lexeme.equals("length")) {
			return (double) elements.length;
		}

		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}

	@Override
	void set(Token name, Object value) {
		// Unlike LoxClass's object, we don't
		// allow adding new fields;
		// Because this is not okay.. arr.age = 10
		throw new RuntimeError(name, "Can't add properties to arrays.");
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(); // a mutable string
		buffer.append("[");
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) buffer.append("nil");
			else if (elements[i] instanceof Double || elements[i] instanceof Integer) {
				String text = elements[i].toString();
				if (text.endsWith(".0")) {
					text = text.substring(0, text.length() - 2);
				}
				buffer.append(text);
			} else buffer.append(elements[i]);
			
			if (i != elements.length - 1) buffer.append(", ");
		}
		buffer.append("]");
		return buffer.toString();
	}
}
