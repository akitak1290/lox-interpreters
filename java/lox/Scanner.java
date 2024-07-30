package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

class Scanner {
	// Raw lox source code as a string
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	// Trackers for the source code
	private int start = 0; // start of a lexeme
	private int current = 0;
	private int line = 1;

	// Reserved keywords
	private static final Map<String, TokenType> keywords;

	static {
		keywords = new HashMap<>();
		keywords.put("and",    AND);
    		keywords.put("class",  CLASS);
    		keywords.put("else",   ELSE);
    		keywords.put("false",  FALSE);
    		keywords.put("for",    FOR);
    		keywords.put("fun",    FUN);
    		keywords.put("if",     IF);
    		keywords.put("nil",    NIL);
    		keywords.put("or",     OR);
    		keywords.put("print",  PRINT);
    		keywords.put("return", RETURN);
    		keywords.put("super",  SUPER);
    		keywords.put("this",   THIS);
    		keywords.put("true",   TRUE);
    		keywords.put("var",    VAR);
    		keywords.put("while",  WHILE);
	}

	Scanner(String source) {
		this.source = source;
	}

	List<Token> scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}

		// Append EOF after the last token
		tokens.add(new Token(EOF, "", null, line));
		return tokens;
	}

	/*
	 * Scan for a single token 
	 */
	private void scanToken() {
		char c = advance();
		switch (c) {
			// 1 character lexeme
			case '(': addToken(LEFT_PAREN); break;
			case ')': addToken(RIGHT_PAREN); break;
			case '{': addToken(LEFT_BRACE); break;
			case '}': addToken(RIGHT_BRACE); break;
			case ',': addToken(COMMA); break;
			case '.': addToken(DOT); break;
			case '-': addToken(MINUS); break;
			case '+': addToken(PLUS); break;
			case ';': addToken(SEMICOLON); break;
			case '*': addToken(STAR); break;
			case '%': addToken(MODULO); break;

			// 2 characers lexemes
			case '!':
				addToken(match('=') ? BANG_EQUAL : BANG);
				break;
      			case '=':
        			addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        			break;
      			case '<':
        			addToken(match('=') ? LESS_EQUAL : LESS);
        			break;
      			case '>':
        			addToken(match('=') ? GREATER_EQUAL : GREATER);
        			break;
			// A bit more special long lexemes
			case '/':
				if(match('/')) {
					// consume the entire line as it is a comment
					while (peek() != '\n' && !isAtEnd()) advance();
				} else {
					addToken(SLASH);
				}
				break;
			// Whitespace and newlines
			case ' ':
			case '\r':
			case '\t':
				// Innore whilespace, let it fallthrough 
				break;
			case '\n':
				line++;
				break;
			// String literal
			case '"': string(); break;
			default:
				// ! The case for numbers is put here for conveniency sake
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Lox.error(line, "Unexpected character.");
				}
				break;
		}
	}

	/*
	 * Read until the end of the identifier
	 */
	private void identifier() {
		while (isAlphaNumeric(peek())) advance();

		String text = source.substring(start, current);
		TokenType type = keywords.get(text);
		if (type == null) type = IDENTIFIER;
		addToken(type);
	}

	/*
	 * Read the content inside a double-quote pair
	 * and add them as a lexeme string literal
	 */
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			// lox support multiline string
			// this is to handle that
			if (peek() == '\n') line++;
			advance();
		}

		// Special error case with dangling double-quote
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
			return;
		}
		// Consume the closing double-quote	
		advance();

		// Trim the surrounding quotes
		String value = source.substring(start + 1, current - 1);
		
		// Parse special sequence
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) != '\\') {
				result.append(value.charAt(i));
				continue;
			}	
				
			if (i + 1 >= value.length()) {
				Lox.error(line, "Unclosed string literal.");
				return;
			}

			char next = value.charAt(i + 1);
			switch (next) {
				case 'n': 
					result.append('\n'); 
					i += 1;
					break;
				case 'r': 
					result.append('\r'); 
					i += 1;
					break;
				case 't': 
					result.append('\t'); 
					i += 1;
					break;
				case 'b': 
					result.append('\b'); 
					i += 1;
					break;
				case '\\': 
					result.append('\\'); 
					i += 1;
					break;
				case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7':
					// Should I allow null?
					int octal = 0;
					int j = i + 1;
					while (j < value.length() && j < i + 4 &&
						Character.isDigit(value.charAt(j)) &&
						value.charAt(j) <= '7') {
							octal = octal * 8 + (value.charAt(j) - '0');
							j++;
					}
					if (octal < 0 || octal > 255) {
						Lox.error(line, "Octal escape sequence out of bounds.");
						return;
					}
					result.append((char) octal);
					i = j - 1; // all octal characters processed, jump over
					break;
				// TODO: add handle for hex
				default:
					// ignore the slash
					result.append(next);
					break;
			}
		}
		
		addToken(STRING, result.toString());
	}

	/*
	 * Read from the first digit to the last
	 * and add them as a lexeme number
	 */
	private void number() {
		while(isDigit(peek())) advance();

		boolean isDouble = false;

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Comsume the "."
			advance();

			while(isDigit(peek())) advance();

			isDouble = true;
		}
		
		if (isDouble) addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
		else addToken(NUMBER, Integer.parseInt(source.substring(start, current)));
	}

	// Helper methods -----------------------------------------
	
	/*
	 * Help to read 2 characters operators as 1 character
	 * move current tracker if it is an operator
	 * It's like a conditional advance() (consume)
	 * Shold be ifCurrentMatch()
	 */
	private boolean match(char expected) {
		if (isAtEnd()) return false;
		if (source.charAt(current) != expected) return false;

		current++;
		return true;
	}

	/*
	 * A lookahead
	 * like advance() but don't consume the character
	 */
	private char peek() {
		if (isAtEnd()) return '\0';
		return source.charAt(current);
	}

	/*
	 * A lookahead lookahead
	 * (look at the character after next)
	 */
	private char peekNext() {
		if (current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}

	/*
	 * Check if the character is an alphabet character
	 * by checking it's ASCII code
	 */
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
		       (c >= 'A' && c <= 'Z') ||
		        c == '_';
	}

	/*
	 * Check if the character is an alphabet character 
	 * or numeric character
	 */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	/*
	 * Check if a character is a number
	 * by checking it's ASCII code
	 * Use instead of Character.isDigit()
	 * because that also allow weird numbers!
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/* Consume the next character */
	private char advance() {
		return source.charAt(current++);
	}

	/* Create a new token from the current lexeme */
	private void addToken(TokenType type) {
		addToken(type, null);
	}

	/* Create a new token from the current lexeme 
	 * Overload for literal values 
	 */
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}
}
