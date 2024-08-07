package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	/*
	 * Store variables/identifier, exist while interpreter
	 * is running
	 * Refers to the current environment.
	 */
	final Environment globals = new Environment();
	private Environment environment = globals;
	/*
	 * Resolved identifiers, populated by the Resolve class
	 */
	private final Map<Expr, Integer> locals = new HashMap<>();


	/*
	 * Define native functions
	 */
	Interpreter() {
		globals.define("clock", new LoxCallable() {
			@Override
			public int arity() { return 0; }

			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				return (double)System.currentTimeMillis() / 1000.0;
			}

			@Override
			public String toString() { return "<native fn>"; }
		});
		globals.define("Array", new LoxCallable() {
			@Override
			public int arity() { return 1; } // 1 argument: array length
			
			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				// cast to double first because Lox only have float
				Double size = scaryCastNumber(arguments.get(0));	
				if (size == null) {
					return new LoxArray(arguments.get(0));
				} else {
					return new LoxArray(size.intValue());
				}
			}

			@Override
			public String toString() { return "<array>"; }
		});
		globals.define("clear", new LoxCallable() {
			@Override
			public int arity() { return 0; }

			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				System.out.print("\033[H\033[J");
				System.out.flush();
				return true;
			}

			@Override
			public String toString() { return "<native fn>"; }
		});
		globals.define("sleep", new LoxCallable() {
			@Override
			public int arity() { return 1; }

			@Override
			public Object call(Interpreter interpreter,
						List<Object> arguments) {
				try {
					// TODO: okay this really should report a runtime error...
					// figure out how native functions can report runtime error
					Double time = scaryCastNumber(arguments.get(0));	
					if (time != null) Thread.sleep(time.intValue()); 

					return true;
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
	}

	// #82332d5 accept an expression that represetns an ast
	// 	    and evaluate it
	// accept a list of statements that represents
	// a Lox script and execute them
	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	/*
	 * Legacy interpret for debugging
	 */
	void interpretExpression(Expr expression) {
		Object value = evaluate(expression);
		System.out.println(stringify(value));
	}


	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);
			
		if (expr.operator.type == TokenType.OR) {
			if (isTruthy(left)) return left;
		} else {
			if (!isTruthy(left)) return left;
		}

		return evaluate(expr.right);
	}
	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if (!(object instanceof LoxInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}

		Object value = evaluate(expr.value);
		((LoxInstance)object).set(expr.name, value);
		return value;
	}
	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr);

		LoxClass superclass = (LoxClass)environment.getAt(distance, "super");
		// This hack works because we specify this order in Resolver
		LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");

		LoxFunction method = superclass.findMethod(expr.method.lexeme);
		
		if (method == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
		}

		// bind the superclass's method with the current instance
		return method.bind(object);
	}
	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		// Evaluate the nested expression
		return evaluate(expr.expression);
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case BANG:
				return !isTruthy(right);
			case MINUS:
				checkNumberOperand(expr.operator, right);	
				return -(double)scaryCastNumber(right);
		}

		return null;
	}

	/*
	 * Helper method
	 * Check if an operand is a number
	 *
	 * @operator Token
	 * @operand Object
	 *
	 * @throw RuntimeError
	*/
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double || operand instanceof Integer) return;
		throw new RuntimeError(operator, "Operand must be a number");
	}

	/*
	 * Helper method
	 * Check if 2 operands are numbers,
	 * cast, and return them
	 *
	 * @operator Token
	 * @left Object
	 * @right Object
	 *
	 * @throw RuntimeError
	*/
	private List<Double> checkNumberOperands(Token operator, Object left, Object right) {
		if ((left instanceof Double || left instanceof Integer) &&
		     (right instanceof Double || right instanceof Integer)) {
			List<Double> values = new ArrayList<>();

			values.add(scaryCastNumber(left));
			values.add(scaryCastNumber(right));

			return values;
		}
		throw new RuntimeError(operator, "Operands must be numbers");
	}
	/*
	 * Cast to double from Integer or Double,
	 * if none, then returns null
	 *
	 * Scary because caller has to manually check if the @value
	 * is either an int or double, wlse it will returns null
	 * @return Double | null
	 */
	private Double scaryCastNumber(Object value) {
		if (value instanceof Double) return (double)value;
		else if (value instanceof Integer) return ((Integer)value).doubleValue();
		else return null; // this should never be reached
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		// Maybe there is a pattern that makes this better...
		List<Double> values;
		switch (expr.operator.type) {
			case GREATER:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) > (double)(values.get(1));
			case GREATER_EQUAL:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) >= (double)(values.get(1));
			case LESS:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) < (double)(values.get(1));
			case LESS_EQUAL:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) <= (double)(values.get(1));
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case MINUS:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) - (double)(values.get(1));
			case PLUS:
				if (left instanceof String && right instanceof Number) {
					String text = scaryCastNumber(right).toString();
					if (text.endsWith(".0")) text = text.substring(0, text.length() -2);
					return (String)left + text;
				} else if (left instanceof Number && right instanceof String) {
					String text = scaryCastNumber(left).toString();
					if (text.endsWith(".0")) text = text.substring(0, text.length() -2);
					return left + (String)right;
				} else if (left instanceof Number && right instanceof Number) {
					values = checkNumberOperands(expr.operator, left, right);
					return (double)(values.get(0)) + (double)(values.get(1));
				} else if (left instanceof String && right instanceof String)  {
					return (String)left + (String)right;
				}
				
				throw new RuntimeError(expr.operator, "Operands must be numbers or strings.");
			case SLASH:
				values = checkNumberOperands(expr.operator, left, right);
				if ((double)values.get(1) == 0) throw new RuntimeError(expr.operator, "Divide by zero");
				
				return (double)(values.get(0)) / (double)(values.get(1));
			case MODULO:
				values = checkNumberOperands(expr.operator, left, right);
				if ((double)values.get(1) == 0) throw new RuntimeError(expr.operator, "Divide by zero");
				
				return (double)(values.get(0)) % (double)(values.get(1));
			case STAR:
				values = checkNumberOperands(expr.operator, left, right);
				return (double)(values.get(0)) * (double)(values.get(1));
		}

		// unreachable
		return null;
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}

		// if callee if not a function identifier
		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren,
				"Can only call functions and classes.");
		}

		LoxCallable function = (LoxCallable)callee;

		// The Python approach, to throw a runtime error
		// instead of auto-passing undefined to empty
		// arguments like JS...
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, "Expected " +
				function.arity() + " arguments but got " +
				arguments.size() + ".");
		}

		return function.call(this, arguments);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof LoxInstance) {
			// Loop up the correct property
			return ((LoxInstance) object).get(expr.name);
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	/*
	 * Helper method
	 * Define logic for comparing 2 objects 
	 * is equal or not equal
	 *
	 * @left Object
	 * @right Object
	 * @return boolean
	 */
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false; // other case is eval next

		if (a instanceof Number && b instanceof Number) {
			return (double)(scaryCastNumber(a)) == (double)(scaryCastNumber(b));
		}	
		
		return a.equals(b); // java and lox have same logic here
	}

	/*
	 * Helper method
	 * Convert a Lox's object that represents
	 * the result of evaluating an expression
	 * into a string
	 *
	 * @object Object
	 * @return String
	 */
	private String stringify(Object object) {
		// Convert java's types to lox's types
		if (object == null) return "nil";

		if (object instanceof Double || object instanceof Integer) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				// Hack of the decimal part
				// for integer, nicer looking
				text = text.substring(0, text.length() -2);
			}
			return text;
		}
		return object.toString();
	}

	/*
	 * Helper method
	 * To define what is truthy and what is falsey
	 * 
	 * @object Object
	 * @return boolean
	 */
	private boolean isTruthy(Object object) {
		if (object == null) return false;
		if (object instanceof Boolean) return (boolean)object;
		return true;
	}

	/*
	 * Helper method
	 * Call the correct visitor method from
	 * this class
	 *
	 * @expr Expr
	 * @return Object
	 */
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	/*
	 * Helper method
	 * Like evaluate, but for statements
	 * Call the correct visitor method from
	 * this class
	 *
	 * @stmt Stmt
	 */
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	/*
	 * Helper method
	 * Store the scope distance from the current
	 * scope and the scope @expr is defined as
	 * @depth
	 * 
	 * @expr Expr
	 * @depth int the distance
	 */
	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

	/*
	 * Helper method
	 * Execute a block statement
	 * This can call to another visitBlockStmt to
	 * run an inner block, hence why we need to
	 * update the env state, run it, and restore
	 * it to the previous state.
	 *
	 * @statements List<Stmt> list of statements
	 * 			  declared in the scope
	 * @environment Environment the current env
	 */
	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;

			for (Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			this.environment = previous;
		}
	}

	// ##################################################################
	// Evaluate statements

	/*
	 * Evaluate/execute a block statement
	 */
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	/*
	 * Evaluate a class statement
	 */
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		// Evaluate superclass
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass); // var expr
			if (!(superclass instanceof LoxClass)) { // check if also a class
				throw new RuntimeError(stmt.superclass.name,
					"Superclass must be a class.");
			}
		}

		// Evaluate current class
		environment.define(stmt.name.lexeme, null);
		
		if (stmt.superclass != null) {
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			LoxFunction function = new LoxFunction(method, environment,
								method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}
		
		LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

		if (superclass != null) {
			environment = environment.enclosing;
		}
		// Define before assign allows for self reference
		// within the class
		environment.assign(stmt.name, klass);
		return null;
	}

	/*
	 * Evaluate a variable declaration statement,
	 * assign null as default value and add to
	 * the enviroment.
	 */
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);
		return null;
	}

	/*
	 * Evaluate the condition statement and
	 * execute the body statement if valid
	 */
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while(isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);
		// environment.assign(expr.name, value);
		Integer distance = locals.get(expr);
		if (distance != null) {
			// Assign the identifier at a certain scope
			// defined by the Resolver class
			environment.assignAt(distance, expr.name, value);
		} else {
			globals.assign(expr.name, value);
		}
		return value;
	}

	/*
	 * Evaluate a variable expression (get value
	 * from a variable)
	 */
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		//return environment.get(expr.name);
		return lookUpVariable(expr.name, expr);
	}
	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	/*
	 * Evaluate a function declaration,
	 * function call is a visitCallStmt...
	 */
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// !Don't forget the current env to define
		// the closure
		LoxFunction function = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override 
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) value = evaluate(stmt.value);

		throw new Return(value);
	}

}
