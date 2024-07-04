package lox;

/*
 * A printer class mainly use to debug
 * the abstract syntax tree class
 */
class AstPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	// AstPrinter implements the visitor interface
	// so we need to overide all of it's method,
	// namely the visit meothod for each expression types
	
	@Override
  	public String visitBinaryExpr(Expr.Binary expr) {
    		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  	}

  	@Override
  	public String visitGroupingExpr(Expr.Grouping expr) {
    		return parenthesize("group", expr.expression);
  	}	

  	@Override
  	public String visitLiteralExpr(Expr.Literal expr) {
    		if (expr.value == null) return "nil";
    		return expr.value.toString();
  	}	

  	@Override
  	public String visitUnaryExpr(Expr.Unary expr) {
    		return parenthesize(expr.operator.lexeme, expr.right);
  	}

	// Dummy methods to keep the compiler happy
	// TODO: fix this!
	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return "";
	}
	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		return "";
	}	

	/*
	 * A helper method to wraps a name and a
	 * list of subexpressions in parentheses
	 * e.g (+ 1 2)
	 */
	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		// Name of the expression
		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}
		builder.append(")");
		return builder.toString();
	}

	/*
	 * A tmp method to help debug the Ast
	 */
	public static void main(String[] args) {
		Expr expression = new Expr.Binary(
        		new Expr.Unary(
            			new Token(TokenType.MINUS, "-", null, 1),
            			new Expr.Literal(123)),
        		new Token(TokenType.STAR, "*", null, 1),
        		new Expr.Grouping(
            			new Expr.Literal(45.67)));

    		System.out.println(new AstPrinter().print(expression));
	}
}
