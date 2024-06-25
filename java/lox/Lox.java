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
	 * Class method to act as flag to stop
	 * the interpreter when there is an error
	 */
	static boolean hadError = false;

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
			System.out.println("Usage: jlox [script] --debug [test folder]");
			System.exit(64); // standard UNIX exit code
		}
	}

	/*
	 * To run a lox file
	 */
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

		if (hadError) System.exit(65);
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

		switch (DEBUG_FLAG) {
			case "":
				// No debug flag, let fallthrough to
				// the most recent added part.
				// TODO: refactor this when done with the
				// interpreter...
			case "parsing":
				// Check Scanner and Parser
		  		Parser parser = new Parser(tokens);
				Expr expression = parser.parse();

				// Flag is set when the parser calls
				// Lox's static mothod to report error
				if (hadError) return;

				System.out.println(new AstPrinter().print(expression));
				break;
			case "scanning":

				// Flag is set when parser call Lox.error()
				// static method to report error.
				// Only check Scanner
				for (Token token : tokens) {
		  			System.out.println(token);
				}
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
