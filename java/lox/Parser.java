package lox;

import java.util.ArrayList;
import java.util.List;

import static lox.TokenType.*;

class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens;
	private int current = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	/*
	 * The main method
	 * #7081c8f partial parsing for 1 line arithmetic
	 *
	 * @return Stmt[]
	 */
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while(!isAtEnd()) {
			statements.add(declaration());
		}

		return statements;
	}

	/*
	 * Legacy parse() for testing purpose
	 *
	 * @return Expr
	 */
	Expr parseExpression() {
		return expression();
	}


	/* 
	 * The hierarchy is written in a way that
	 * allow fall-through from lower precedence
	 * expr to higher
	 */
	private Stmt declaration() {
		try {
			if (match(VAR)) return varDeclaration();

			return statement();
		} catch (ParseError error) {
			// Read until next statement to
			// skip current error and parse
			// next statement
			synchronize();
			return null;
		}
	}

	/*
	 * Rule for variable declaration statement
	 * "var" is parsed by the caller statement function
	 * "var" IDENTIFIER ( "=" expression )? ";"
	 *
	 * @return Stmt
	 */
	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect variable name.");

		Expr initializer = null;
		if (match(EQUAL)) {
			initializer = expression();
		}

		consume(SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	// ##################################################################
	// Statements
	
	/*
	 * Main statements parse
	 *
	 * @return Stmt
	 */
	private Stmt statement() {
		if (match(PRINT)) return printStatement();

		return expressionStatement();
	
	}
	/*
	 * Rule for print expression
	 * semicolon is the 'stop anchor'
	 * "print" expression ";"
	 *
	 * @return Stmt statement wrapper
	 */
	private Stmt printStatement() {
		Expr value = expression();
		consume(SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	/*
	 * Rule for expression statement
	 * semicolon is the 'stop anchor'
	 * expression ";"
	 *
	 * @return Stmt statement wrapper
	 */
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	// ##################################################################
	// Expressions

	/*
	 * Rule for expression
	 */
	private Expr expression() {
		return equality();	
	}

	/*
	 * Rule for equality
	 * comparison ( ( "!=" | "==" ) comparison )*
	 * Return the new expression or a
	 * comparision expression to match
	 * expression of higher precedence
	 *
	 * @return Expr an expression string
	 */
	private Expr equality() {
		Expr expr = comparision();
		
		// Exit when next operator is not
		// and equality operator.
		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparision();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}

	/*
	 * Rule for comparision
	 * term ( ( ">" | ">=" | "<" | "<=" ) term )*
	 * Identical to the equality rule
	 *
	 * @return Expr an expression string
	 */
	private Expr comparision() {
		Expr expr = term();

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/*
	 * Rule for term
	 * factor ( ( "-" | "+" ) factor )*
	 * Identical to other binary operator rules
	 *
	 * @return Expr an expression string 
	 */
	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/*
	 * Rule for factor
	 * unary ( ( "/" | "*" ) unary )*
	 * Identical to other binary operator rules
	 *
	 * @return Expr an expression string
	 */
	private Expr factor() {
		Expr expr = unary();

		while (match(SLASH, STAR)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/*
	 * Rule for unary
	 * ("!" | "-") unary | primary
	 *
	 * @return Expr an expression string
	 */
	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	/*
	 * Rule for primary
	 * NUMBER | STRING | "true" | "false" | "nil" |"(" expression ")"
	 *
	 * @return Expr an expression string
	 */
	private Expr primary() {
		if (match(FALSE)) return new Expr.Literal(false);
		if (match(TRUE)) return new Expr.Literal(true);
		if (match(NIL)) return new Expr.Literal(null);

		if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if (match(IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		// A token that can't start an expression
		throw error(peek(), "Expect expresson.");
	}

	// ##################################################################
	// Helpers

	/*
	 * Helper method
	 * Check if the next token if of type @type
	 * if yes, consume the token, else throw an error
	 *
	 * @return Token the current token
	 * @throw RuntimeException 
	 */
	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();

		throw error(peek(), message);
	}

	/*
	 * Helper method
	 * Report the error using Lox's static method
	 * Create and return a parse error object
	 */
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	/*
	 * Helper method
	 * Keep consuming token until a statement boundary or
	 * is at the end of the file
	 * Also use for error handling
	 */
	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return;

			switch (peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
			}

			advance();
		}
	}

	/*
	 * Helper method
	 * Check current tokens' types to match 
	 * any types in @types. If found, return
	 * true and consume the token.
	 *
	 * @param types TokenType (zero or many)
	 * @return boolean
	 */
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	/*
	 * Helper method
	 * Check curren token type to match
	 * @type. If found, return true,
	 * doesn't comsume the token.
	 *
	 * @param type TokenType
	 * @return boolean
	 */
	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}

	/*
	 * Helper method
	 * Consume the current token and
	 * return it (the current index).
	 *
	 * @return integer
	 */
	private Token advance() {
		if(!isAtEnd()) current++;
		return previous();
	}

	/*
	 * Helper method
	 * Check if all the tokens has
	 * been red.
	 *
	 * @return boolean
	 */
	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	/* 
	 * Helper method
	 * Get current token
	 *
	 * @return Token
	 */
	private Token peek() {
		return tokens.get(current);
	}
	
	/*
	 * Helper method
	 * Get the previous token
	 *
	 * @return Token
	 */
	private Token previous() {
		return tokens.get(current - 1);
	}
}
