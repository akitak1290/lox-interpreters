package lox;

class Interpreter implements Expr.Visitor<Object> {
	void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
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



}
