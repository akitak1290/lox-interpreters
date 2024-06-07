package com.craftinginterpreters.lox;

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

	/*
	 * There are 2 ways to run a lox file
	 * - 1: Pass in the path to the file and this will execute it
	 * - 2: Pass in nothing and type the lox code one line at a time
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64); // standard UNIX exit code
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
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
	 * Error reporter
	 */
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}
}	
