package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/*
 * A standalone application (suppose to be a script)
 * used to generate the classes representing the
 * abstract syntax tree to define the grammar for
 * Lox's expression
 *
 * TODO: double check the format of the files to
 * be generated, think white spaces are looking
 * kinda ugly
 */

public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];

		// Define name and fields for each expression class
		defineAst(outputDir, "Expr", Arrays.asList(
			"Binary	  : Expr left, Token operator, Expr right",
			"Grouping : Expr expression",
			"Literal  : Object value",
			"Unary    : Token operator, Expr right",
			"Variable : Token name"
		));
		
		// Define name and fields for each statement class
		defineAst(outputDir, "Stmt", Arrays.asList(
			"Expression : Expr expression",
			"Print	    : Expr expression",
			"Var	    : Token name, Expr initializer"
		));
	}

	/* 
	 * Function to generate the expression class
	 * and subclasses, because writing boilerplate
	 * code is boring...
	 */
	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package lox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println("abstract class " + baseName + " {");

		defineVisitor(writer, baseName, types);
				
		// This is why we need this class to generate
		// the classes
		// The AST classes
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}

		// (readmore about the visitor pattern in note)
		// The baseclass's accept method
		writer.println();
		writer.println("	abstract <R> R accept(Visitor<R> visitor);");

		writer.println("}");
		writer.close();
	}

	/*
	 * Generate the visitor interface,
	 * declare a visit method for each
	 * expression subclass
	 * (read more about the visitor pattern in note)
	 */
	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println(" interface Visitor<R> {");

		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("	R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}

		writer.println(" }");
	}
	
	/*
	 * Refactored function to be a part of defineAst()
	 * to generate each AST classes
	 */
	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println(" static class " + className + " extends " + baseName + " {");

		// Constructor
		writer.println("	" + className + "(" + fieldList + "){");

		// Store parameters in fields.
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("	this." + name + " = " + name + ";");
		}

		writer.println("	}");

		// Visotor pattern.
		// (read more about the visitor pattern in the note)
		// each subclass override the accept method and call the
		// right visit method for its own type
		writer.println();
		writer.println("	@Override");
		writer.println("	<R> R accept(Visitor<R> visitor) {");
		writer.println("		return visitor.visit" + className + baseName + "(this);");
		writer.println("	}");

		// Fields
		writer.println();
		for (String field : fields) {
			writer.println("	final " + field + ";");
		}

		writer.println("	}");
	}
}
