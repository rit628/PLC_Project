package plc.project;

import java.util.Scanner;
import java.util.List;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            String path = "./plclang/";
            File input = new File(path + args[0]);
            Scanner scanner = new Scanner(input).useDelimiter("\\Z");
            String source = scanner.next();
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.lex();
            Parser parser = new Parser(tokens);
            Ast.Source parsedSource = parser.parseSource();
            Interpreter interpreter = new Interpreter(null);
            Object evaluatedResult = interpreter.visit(parsedSource).getValue();
            System.out.println("File " + args[0] + " Returned: " + evaluatedResult);
            scanner.close();
        }
        catch (IOException e) {
            System.err.println("UH OH");
        }
    }
}
