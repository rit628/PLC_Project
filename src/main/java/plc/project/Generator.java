package plc.project;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    private String cleanEscapes(String literal) {
        literal = literal.replace("\\", "\\\\");
        literal = literal.replace("\b", "\\b");
        literal = literal.replace("\n", "\\n");
        literal = literal.replace("\r", "\\r");
        literal = literal.replace("\t", "\\t");
        literal = literal.replace("\'", "\\'");
        literal = literal.replace("\"", "\\\"");
        return literal;
    }

    private void printGlobals(List<Ast.Global> globals) {
        int topIndent = indent;
        indent++;
        for (Ast.Global global : globals) {
            newline(indent);
            print(global);
        }
        if (globals.size() > 0) {
            newline(0);
        }
        indent = topIndent;
    }

    private void printMain() {
        int topIndent = indent;
        indent++;
        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent + 1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");
        newline(0);
        indent = topIndent;
    }

    private void printFunctions(List<Ast.Function> functions) {
        int topIndent = indent;
        indent++;
        for (Ast.Function function : functions) {
            newline(indent);
            print(function);
            newline(0);
        }
        indent = topIndent;
    }

    private void printBlock(List<? extends Ast.Statement> statements) {
        int topIndent = indent;
        indent++;
        for (Ast.Statement statement : statements) {
            newline(indent);
            print(statement);
        }
        indent = topIndent;
    }

    private void printBracedBlock(List<? extends Ast.Statement> statements) {
        print(" {");
        printBlock(statements);
        if (statements.size() > 0) {
            newline(indent);
        }
        print("}");
    }

    private void printCommaSeperatedList(List<? extends Object> elements) {
        for (int i = 0; i < elements.size(); i++) {
            print(elements.get(i));
            if (i != elements.size() - 1) {
                print(", ");
            }
        }
    }

    private void printFunctionParameters(List<String> params, List<Environment.Type> types) {
        List<String> paramList = new ArrayList<String>();
        for (int i = 0; i < params.size(); i++) {
            paramList.add(types.get(i).getJvmName() + ' ' + params.get(i));
        }
        printCommaSeperatedList(paramList);
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        printGlobals(ast.getGlobals());
        printMain();
        printFunctions(ast.getFunctions());
        newline(0);
        print("}");        
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        String typeName = ast.getVariable().getType().getJvmName();
        String variableName = ast.getVariable().getJvmName();
        if (ast.getValue().isEmpty()) {
            print(typeName, ' ', variableName, ';');
            return null;
        }
        Ast.Expression value = ast.getValue().get();
        if (value instanceof Ast.Expression.PlcList) {
            print(typeName, "[] ", variableName, " = ", value);
        }
        else if (ast.getMutable()) {
            print(typeName, ' ', variableName, " = ", value);
        }
        else{
            print("final ",  typeName,  ' ', variableName,  " = ",  value);
        }
        print(';');
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        Environment.Function function = ast.getFunction();
        List<Environment.Type> paramTypes = function.getParameterTypes();
        List<String> params = ast.getParameters();
        print(function.getReturnType().getJvmName(), ' ', function.getJvmName(), '(');
        printFunctionParameters(params, paramTypes);
        print(")");
        printBracedBlock(ast.getStatements());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";"); 
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String typeName = ast.getVariable().getType().getJvmName();
        String variableName = ast.getVariable().getJvmName();
        print(typeName, ' ', variableName);
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ';');
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (", ast.getCondition(), ")");
        printBracedBlock(ast.getThenStatements());

        if(!ast.getElseStatements().isEmpty()){
            print(" else");
            printBracedBlock(ast.getElseStatements());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (", ast.getCondition(), ")");
        printBracedBlock(ast.getCases());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if(ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
            printBlock(ast.getStatements());
            newline(indent + 1);
            print("break;");
        }
        else{
            print("default:");
            printBlock(ast.getStatements());
        }
        return null;
    }


   @Override
   public Void visit(Ast.Statement.While ast) {
        print("while (", ast.getCondition(), ")");
        printBracedBlock(ast.getStatements());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ';');
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
    
        if(ast.getType() == Environment.Type.STRING){
            print("\"",  cleanEscapes((String)ast.getLiteral()), "\"");
        }
        else if(ast.getType() == Environment.Type.CHARACTER){
            print("\'", cleanEscapes(((Character)ast.getLiteral()).toString()), "\'");
        }
        else if (ast.getType() == Environment.Type.NIL) {
            print("null");
        }
        else{
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print('(', ast.getExpression(), ')'); 
        return null;
     }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")) {
            print("Math.pow(", ast.getLeft(), ", ", ast.getRight(), ')');
            return null;
        }
        print(ast.getLeft(), ' ', ast.getOperator(), ' ', ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if (ast.getOffset().isPresent()) {
            print('[', ast.getOffset().get(), ']');
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = ast.getFunction();
        print(function.getJvmName(), '(');
        printCommaSeperatedList(ast.getArguments());
        print(')');
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print('{');
        printCommaSeperatedList(ast.getValues());
        print('}');
        return null;
    }

}
