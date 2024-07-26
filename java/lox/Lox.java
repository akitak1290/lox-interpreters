package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	/*
	 * A gateway to the interpreting logics 
	 * Is static so global variables stored by it
	 * persists in REPL mode
	 */
	private static final Interpreter interpreter = new Interpreter();
	/* 
	 * Flags to stop the interpreter when there is an error
	 */
	static boolean hadError = false;
	static boolean hadRuntimeError = false;

	// For running tests on individual parts
	private enum Component {
		SCANNER,
		PARSER,
		EVALUATOR
	}

	/*
	 * There are 2 ways to run a lox file
	 * - 1: Pass in the path to the file and this will execute it
	 * - 2: Pass in nothing and type the lox code one line at a time
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			runPrompt();
		} else if (args.length == 1) {
			runFile(args[0], null);
		} else if (args.length == 3 && args[1].equals("--single")) {
			try {
				Component onlyComponent = Component.valueOf(args[2].toUpperCase());
				runFile(args[0], onlyComponent);
			} catch (IllegalArgumentException e) {
				System.out.println("Usage: Options for --single includes" +
						   " 'scanner', 'parser', 'or 'evaluator'.");
				System.exit(64); // standard UNIX exit code
			}
		} else {
			System.out.println("Usage: jlox [script] --debug [flag]");
			System.exit(64); // standard UNIX exit code
		}
	}

	/*
	 * To run a lox file
	 */
	private static void runFile(String path, Component onlyComponent) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		runner(new String(bytes, Charset.defaultCharset()), onlyComponent);

		// Some etiquette when the interpreter quits
		// with and error
		if (hadError) System.exit(65);
		if (hadRuntimeError) System.exit(70);
	}
	
	/*
	 * Read a line, Evaluate it, Print the result, then Loop (REPL)
	 * To create an interactive prompt
	 */
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);
		
		// infinite loop until EOF signal
		// kill with Ctr-D
		for (;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) break;
			runner(line, null);

			// reset flag
			hadError = false;
		}
	}

	private static void runner(String source, Component onlyComponent) {
		if (onlyComponent == null) run(source);
		else if (onlyComponent == Component.SCANNER) runScanner(source);
		else if (onlyComponent == Component.PARSER) runParser(source);
		else if (onlyComponent == Component.EVALUATOR) runExprInterpreter(source);
	}

	/*
	 * Main parser function
	 */
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		if (hadError) return; // check for Parser error

		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);

		if (hadError) return; // check for Resolver error

		interpreter.interpret(statements);
	}

	/*
	 * Helper method, each run a part of 
	 * the interpreter
	 */
	private static void runExprInterpreter(String source) {	
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		Expr expression = parser.parseExpression();

		if (hadError) return;

		interpreter.interpretExpression(expression);
	}
	private static void runParser(String source) {	
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		Expr expression = parser.parseExpression();

		if (hadError) return;

		System.out.println(new AstPrinter().print(expression));
	}
	private static void runScanner(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		for (Token token : tokens) {
			System.out.println(token);
		}		
	}
	
	/*
	 * Call to error reporter
	 */
	static void error(int line, String message) {
		report(line, "", message);
	}

	/*
	 * Error reporter for the interpreter
	 */
	static void runtimeError(RuntimeError error) {
		System.err.println("[line " + error.token.line + "] Runtime error: " + error.getMessage());
		hadRuntimeError = true;
	}

	/*
	 * Overload error for parser error
	 */
	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}

	/*
	 * Error reporter
	 */
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
}	
