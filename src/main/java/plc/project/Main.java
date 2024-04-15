package plc.project;

import java.util.Scanner;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

    static String inputPath = "./plclang/src/";
    static String outputPath = "./plclang/build/";
    static String testPath = "./plclang/test/";

    public static void main(String[] args) {
        if (args[0].equals("-i") || args[0].equals("--interpret")) {
            interpret(args[1]);
        }
        else {
            if (args[0].equals("-t") || args[0].equals("--test")) {
                compile(args[1], true);
            }
            else {
                compile(args[0], false);
            }
        }
    }
    
    static void interpret(String filename) {
        try {
            File input = new File(inputPath + filename);
            Scanner scanner = new Scanner(input).useDelimiter("\\Z");
            String source = scanner.next();
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens);
            Ast.Source parsedSource = parser.parseSource();
            Interpreter interpreter = new Interpreter(null);
            Object evaluatedResult = interpreter.visit(parsedSource).getValue();
            System.out.println("File " + filename + " Returned: " + evaluatedResult);
            scanner.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static void compile(String filename, boolean testFlag) {
        try {
            File input = new File(inputPath + filename);
            Scanner scanner = new Scanner(input).useDelimiter("\\Z");
            String source = scanner.next();
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens);
            Ast.Source parsedSource = parser.parseSource();
            Analyzer analyzer = new Analyzer(null);
            analyzer.visit(parsedSource);
            StringWriter srcStream = new StringWriter();
            PrintWriter output = new PrintWriter(srcStream);
            Generator generator = new Generator(output);
            generator.visit(parsedSource);
            output.flush();
            System.out.print(srcStream.toString());
            String fileBaseName = filename.split("\\.")[0];
            FileWriter outputFile = new FileWriter(outputPath + fileBaseName + ".java");
            outputFile.write(srcStream.toString());
            outputFile.close();
            scanner.close();
            if (testFlag) {
                File testFile = new File(testPath + fileBaseName + ".java");
                Scanner testScanner = new Scanner(testFile).useDelimiter("\\Z");
                String testSrc = testScanner.next();
                testScanner.close();
                if (!testSrc.equals(srcStream.toString())) {
                    throw new RuntimeException("GENERATED FILE DIFFERS FROM TEST FILE.");
                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
