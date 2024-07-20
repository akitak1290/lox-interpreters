package lox;

import java.util.Arrays;
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
			if (match(CLASS)) return classDeclaration();
			if (match(FUN)) return function("function");
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
	 * Rule for class declaration
	 * "class" is parsed by the caller
	 * "class" IDENTIFIER "{" function* "}";
	 *
	 * @return Stmt
	 */
	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expect class name.");
		consume(LEFT_BRACE, "Expect '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();
		while(!check(RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}

		consume(RIGHT_BRACE, "Expect '}' after class body.");
		return new Stmt.Class(name, methods);
	}

	/*
	 * Rule for function declarationfunction
	 * "fun" is parsed by the caller
	 * "fun" IDENTIFIER "(" parameters? ")" block;
	 * kinda similar for the function handling
	 * function call...
	 *
	 * @params String kind kind of declaration
	 * @return Stmt.Function
	 */
	private Stmt.Function function(String kind) {
		Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
		consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 255) {
					error(peek(), "Can't have more than 255 parameters.");
				}

				parameters.add(consume(IDENTIFIER, "Expect parameter name."));
			} while (match(COMMA));
		}
		consume(RIGHT_PAREN, "Expect ')' after parameters.");

		consume(LEFT_BRACE, "Expect '{' befpre " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}

	/*
	 * Rule for variable declaration
	 * "var" is parsed by the caller
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
		if (match(FOR)) return forStatement();
		if (match(IF)) return ifStatement();
		if (match(PRINT)) return printStatement();
		if (match(RETURN)) return returnStatement();
		if (match(WHILE)) return whileStatement();
		// !shouldn't block handles wrapping statements
		// in Stmt.Block()?
		// TODO: check this later.
		if (match(LEFT_BRACE)) return new Stmt.Block(block());

		return expressionStatement();
	
	}

	/*
	 * Rule for for loop statement
	 * "for" "(" ( varDecl | exprStmt | ";" )
	 * express? ";" expression? ")"
	 * statement
	 * Allow all three clauses to be empty (for(;;){})
	 *
	 * return Stmt
	 */
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'for'.");

		// initializer clause
		Stmt initializer;
		if (match(SEMICOLON)) {
			initializer = null;
		} else if (match(VAR)) {
			initializer = varDeclaration();
		} else {
			// expr stmt instead of exmp to
			// ensure that it is a stmt.
			initializer = expressionStatement();
		}

		// condition clause
		Expr condition = null;
		if (!check(SEMICOLON)) {
			condition = expression();
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");

		// increment clause
		Expr increment = null;
		if (!check(RIGHT_PAREN)) {
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after for clauses.");

		// body statement
		Stmt body = statement();
		if (increment != null) { // safeguarding 
			body = new Stmt.Block(
				Arrays.asList(
					body,
					new Stmt.Expression(increment)));
		}

		// desugar to a while loop, something
		// we already have
		if (condition == null) condition = new Expr.Literal(true);
		body = new Stmt.While(condition, body);

		if (initializer != null) {
			// create the initializer first, then run the
			// loop.
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	/*
	 * Rule for branching control statement
	 * semicolon is the 'stop anchor'
	 * "if" "(" expression ")" statement ( "else" statement )?;
	 *
	 * return Stmt
	 */
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after if condition.");
		Stmt thenBranch = statement();
		Stmt elseBranch = null;

		// Greedy, the inner most if will grab the next 'else'
		// if it sees one.
		if (match(ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	/*
	 * Rule for print statement
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
	 * Rule for return statement
	 * semicolon is the 'stop anchor'
	 *
	 * @return Stmt
	 */
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}

		consume(SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}

	/*
	 * Rule for while statement
	 * "while" "(" expression ")" statement;
	 *
	 * @return Stmt
	 */
	private Stmt whileStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	/*
	 * Rule for block statement
	 * Parse a list of statements
	 * closing curly brace is the
	 * 'stop anchor'
	 * '{' is consumed by the caller.
	 *
	 * @return Stmt statement wrapper
	 */
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		consume(RIGHT_BRACE, "Expect '}' after block.");
		return statements;
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
		return assignment();
	}

	/*
	 * Rule for assignment
	 * IDENTIFIER "=" assignment | equality
	 * Because the assignment target can be
	 * a complex string of tokens, we parse
	 * it as a r-value and cast it to a Variable
	 *
	 * @return Expr
	 */
	private Expr assignment() {
		// parse as a r-value
		Expr expr = or();

		if (match(EQUAL)) {
			Token equals = previous();

			// Parse r-value 
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				// Cast expr to a l-value and get
				// it's binding name
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			} else if (expr instanceof Expr.Get) {
				// Is l-value of `call '.'` expression
				Expr.Get get = (Expr.Get)expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(equals, "Invalid assignment target.");
		}
		return expr;
	}

	/*
	 * Rule for logical 'or'
	 * logic_and ( "or" logic_and )*;
	 *
	 * @return Expr
	 */
	private Expr or() {
		Expr expr = and();

		while (match(OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
	}

	/*
	 * Rule for logical 'and'
	 * equality ( "and" equality )*;
	 *
	 * @return Expr
	 */
	private Expr and() {
		Expr expr = equality();

		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}
		return expr;
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

		return call();
	}

	/*
	 * Extension of function call
	 * Rule for function call operator
	 *
	 * @return Expr
	 */
	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) { // check if arguments
			do {
				if (arguments.size() >= 255) {
					// report the error and keep running
					error(peek(), "Can't have more than 255 arguments.");
				}
				// parse each argument expression
				arguments.add(expression());
			} while (match(COMMA));
		}
		Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
		return new Expr.Call(callee, paren, arguments);
	}

	/*
	 * Rule for call (function call)
	 * primary ( "(" arguments? ")" )*;
	 *
	 * @return Expr
	 */
	private Expr call() {
		// Parse the first expression
		Expr expr = primary();

		// Parse the returned expr again
		// if the result is called
		// this means: fn(1)(2)(3)
		while (true) {
			if (match(LEFT_PAREN)) {
				// Parse the call expression
				// using the callee expr
				expr = finishCall(expr);
			} else if (match(DOT)) {
				Token name = consume(IDENTIFIER, "Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
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

		if (match(THIS)) return new Expr.This(previous());

		if (match(IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		// A token that can't start an expression
		throw error(peek(), "Expect expression.");
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
