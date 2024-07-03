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
	static String DEBUG_FLAG = "";	

	/*
	 * There are 2 ways to run a lox file
	 * - 1: Pass in the path to the file and this will execute it
	 * - 2: Pass in nothing and type the lox code one line at a time
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			runPrompt();
		} else if (args.length == 1) {
			runFile(args[0]);
		} else if (args.length == 3 && args[1].equals("--debug")) {
			DEBUG_FLAG = args[2];
			runFile(args[0]);
		} else {
			System.out.println("Usage: jlox [script] --debug [flag]");
			System.exit(64); // standard UNIX exit code
		}
	}

	/*
	 * To run a lox file
	 */
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

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
			run(line);

			// reset flag
			hadError = false;
		}
	}

	/*
	 * Main parser function
	 */
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = null;
		Expr expression = null;
		// hadError is set when the parser calls
		// Lox's static mothod to report error
		switch (DEBUG_FLAG) {
			case "":
				// No debug flag, let fallthrough to
				// the most recent added part.
				// TODO: refactor this when done with the interpreter...
			/*
			case "interpreting":
				parser = new Parser(tokens);
				List<Stmt> statements = parser.parse();

				if (hadError) return;

				interpreter.interpret(statements);
				break;
				*/
			case "evaluating":
				parser = new Parser(tokens);
				expression = parser.parseExpression();

				if (hadError) return;

				interpreter.interpretExpression(expression);
				break;
			case "parsing":
				parser = new Parser(tokens);
				expression = parser.parseExpression();

				if (hadError) return;

				System.out.println(new AstPrinter().print(expression));
				break;
			case "scanning":
				for (Token token : tokens) {
		  			System.out.println(token);
				}
				break;
			default:
				// This should be handled in main()
				// before it reaches here
				// TODO: refactor this
				return;
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
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
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
