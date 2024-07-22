package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/*
 * Locate where variable declarations are in
 * the environments stack
 *
 * This class squeezes between the parsing phase and the
 * interpreting phase to resolve declarations and binds
 * those to variable calls
 *
 * The MEAT of this class is in declare() and
 * resolveLocal()
 *
 * The class is only used for block scopes and not global
 * scope as it doesn't causes problems.
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	// key: name of varaible, value: initialized state, false
	// means not initialized.
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	// We want this class to also track invalid return statement by
	// checking if it is currently in a function
	private FunctionType currentFunction = FunctionType.NONE;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	private enum FunctionType {
		NONE,
		FUNCTION,
		INITIALIZER,
		METHOD
	}
	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}
	private ClassType currentClass = ClassType.NONE;

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}
	
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS; // to account for nested class

		declare(stmt.name);
		define(stmt.name);

		// Disallow inherit from self
		if (stmt.superclass != null && 
		    stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
			Lox.error(stmt.superclass.name, "A class can't inherit from itself.");	
		}

		if (stmt.superclass != null) {
			resolve(stmt.superclass); // to account for nested class
		}

		if (stmt.superclass != null) {
			beginScope();
			scopes.peek().put("super", true);
		}

		beginScope();
		scopes.peek().put("this", true); // `this` acts like closure var

		// resolve methods
		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}
		
		endScope();

		if (stmt.superclass != null) endScope();

		currentClass = enclosingClass;
		return null;
	}

	/*
	 * Resolve initializer for variable
	 * declaration
	 *
	 * Resolve initializer first 'casuse
	 * not such thing as a recursive var,
	 * only recursive func
	 */
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		// Add the varaible into inner most scope
		// so it shadows outer ones
		declare(stmt.name);
		// Resolve intializer
		if (stmt.initializer != null) {
			// if the inner most scoped
			// variable's initialized state
			// is false, throw error.
			resolve(stmt.initializer);
		}
		// Update state of the variable as 
		// initialized and ready to be used
		define(stmt.name);
		return null;
	}
	/*
	 * Resolve call to variable
	 */
	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
			Lox.error(expr.name, "Can't read local variable in its own initializer.");
		}
		resolveLocal(expr, expr.name);
		return null;
	}
	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		define(stmt.name);

		// Resolve later to enable recursion
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		resolve(stmt.condition);
    		resolve(stmt.thenBranch);
    		if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    		return null;
	}
	@Override
  	public Void visitPrintStmt(Stmt.Print stmt) {
		resolve(stmt.expression);
		return null;
	}
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}
		
		if (stmt.value != null) {
			// Constructor can't have explicit return
			if (currentFunction == FunctionType.INITIALIZER) {
				Lox.error(stmt.keyword, "Can't return a value from an initializer.");
			}

			resolve(stmt.value);
		}

		return null;
 	}
	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}
	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);

		for (Expr argument : expr.arguments) {
			resolve(argument);
		}

		return null;
	}
	@Override
	public Void visitGetExpr(Expr.Get expr) {
		resolve(expr.object);
		return null;
	}
	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}
	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}
	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	@Override
	public Void visitSetExpr(Expr.Set expr) {
		resolve(expr.value); // the value to set
		resolve(expr.object); // the obect with property being set
		return null;
	}
	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		resolveLocal(expr, expr.keyword);
		return null;
	}
	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword, "Can't use 'this' outside a class.");
			return null;
		}

		resolveLocal(expr, expr.keyword); // resolve like a variable
		return null;
	}
	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}
	void resolve(List<Stmt> statements) {
		for (Stmt statement : statements) {
			resolve(statement);
		}
	}
	// #######################################################################################
	// Helper methods
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}
	private void resolve(Expr expr) {
		expr.accept(this);
	}
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}
	private void endScope() {
		scopes.pop();
	}
	/*
	 * Add new varaible to inner most scope
	 */
	private void declare(Token name) {
		if (scopes.isEmpty()) return;

		Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexeme, false);
	}
	/*
	 * Set variable's initialized state to true
	 */
	private void define(Token name) {
		if (scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	/*
	 * Find the current scope level that contains
	 * the variable name and add it to the Interpreter
	 * class
	 */
	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				// current scope is 0, next outer scope is 1 ...
				interpreter.resolve(expr, scopes.size() - 1 - i);
			}
		}
		// Variable must be global, let interpreter handles that
	}
	/*
	 * Create new inner scope for function's body before resolving it
	 */
	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type; // to account for local nested funcitons

		beginScope();
		for (Token param : function.params) {
			declare(param);
			define(param);
		}
			resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}
}
