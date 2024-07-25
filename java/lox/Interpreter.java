package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				return -(double)right;
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
		if (operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number");
	}

	/*
	 * Helper method
	 * Check if 2 operands are numbers
	 *
	 * @operator Token
	 * @left Object
	 * @right Object
	 *
	 * @throw RuntimeError
	 */
	 private void checkNumberOperands(Token operator, Object left, Object right) {
		 if (left instanceof Double && right instanceof Double) return;
		 throw new RuntimeError(operator, "Operands must be numbers");
	 }

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left >= (double)right;
			case LESS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left <= (double)right;
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left - (double)right;
			case PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double)left + (double)right;
				}

				if (left instanceof String && right instanceof String) {
					return (String)left + (String)right;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				return (double)left / (double)right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double)left * (double)right;
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

		if (object instanceof Double) {
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
