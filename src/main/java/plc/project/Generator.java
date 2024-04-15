package plc.project;

import java.io.PrintWriter;
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

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        indent++;
        for (Ast.Global global : ast.getGlobals()) {
            newline(indent);
            print(global);
        }
        if (ast.getGlobals().size() > 0) {
            newline(0);
        }
        newline(indent);
        int funIndent = indent;
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent = funIndent;
        newline(indent);
        print("}");
        newline(0);
        for (Ast.Function function : ast.getFunctions()) {
            newline(indent);
            print(function);
            newline(0);
        }
        newline(0);
        print("}");        
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList) {
            print(ast.getVariable().getType().getJvmName(), "[] ", ast.getVariable().getJvmName(), " = ", ast.getValue().get());
        }
        else if (ast.getMutable()) {
            print(ast.getVariable().getType().getJvmName(), ' ', ast.getVariable().getJvmName());
            if (ast.getValue().isPresent()) {
                print(" = ", ast.getValue().get());
            }
        }
        else{
            print("final ",  ast.getVariable().getType().getJvmName(),  ' ');
            print(ast.getVariable().getJvmName(),  " = ",  ast.getValue().get());
        }
        print(';');
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        Environment.Function function = ast.getFunction();
        print(function.getReturnType().getJvmName(), ' ', function.getJvmName(), '(');
        List<Environment.Type> paramTypes = function.getParameterTypes();
        List<String> params = ast.getParameters();
        for (int i = 0; i < function.getArity(); i++) {
            print(paramTypes.get(i).getJvmName(), ' ', params.get(i));
            if (i != function.getArity() - 1) {
                print(", ");
            }
        }
        print(") {");
        int topIndent = indent;
        indent++;
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement statement : statements) {
            newline(indent);
            print(statement);
        }
        indent = topIndent;
        if (statements.size() > 0) {
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression(), ";"); 
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), ' ', ast.getVariable().getJvmName());
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
        int past_indent = indent;

        print("if (", ast.getCondition(), ") {");
        indent += 1;
        for(Ast.Statement thenStmt : ast.getThenStatements()){
            newline(indent);
            print(thenStmt);
            
        }
        indent -= 1;
        newline(indent);
        print("}");

        if(!ast.getElseStatements().isEmpty()){
            print(" else {");
            indent += 1;
            for(Ast.Statement elseStmt : ast.getElseStatements()){
                newline(indent);
                print(elseStmt);
            }
            indent -= 1;
            newline(indent - 1);
            print("}");
        }
        indent = past_indent;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        int prev_indent = indent;
        print("switch (", ast.getCondition(), ") {");
        indent++;
        for(Ast.Statement.Case cs : ast.getCases()){
            newline(indent);
            print(cs);
        }
        indent = prev_indent;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        int prev_indent = indent;
        if(ast.getValue().isPresent()) {
            print("case ", ast.getValue().get(), ":");
            indent++;
            for (Ast.Statement csStmt : ast.getStatements()) {
                newline(indent);
                print(csStmt);
            }
            newline(indent);
            print("break;");
        }
        else{
            print("default:");
            indent++;
            for (Ast.Statement csStmt : ast.getStatements()) {
                newline(indent);
                print(csStmt);

            }
        }

        indent = prev_indent;
        return null;
    }


   @Override
   public Void visit(Ast.Statement.While ast) {
        int prev_indent = indent;
        print("while (", ast.getCondition(), ") {");
        indent++;
        for(Ast.Statement stmt : ast.getStatements()){
            newline(indent);
            print(stmt);

        }
        indent = prev_indent;
        newline(indent);
        print("}");
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
            print("Math.pow(", ast.getLeft(), ' ', ast.getRight(), ')');
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
        List<Ast.Expression> args = ast.getArguments();
        for (int i = 0; i < args.size(); i++) {
            print(args.get(i));
            if (i != args.size() - 1) {
                print(',');
            }
        }
        print(')');
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print('{');
        List<Ast.Expression> vals = ast.getValues();
        for (int i = 0; i < vals.size(); i++) {
            print(vals.get(i));
            if (i != vals.size() - 1) {
                print(", ");
            }
        }
        print('}');
        return null;
    }

}
