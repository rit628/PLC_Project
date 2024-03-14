package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(){




        }



        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() == null){
            return Environment.create(Environment.NIL.getValue());
        }
        else {
            return Environment.create(ast.getLiteral());
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject right = visit(ast.getRight());
        Environment.PlcObject left = visit(ast.getLeft());
        String operator = ast.getOperator();
        System.out.println(operator);



        //THIS IS FOR BOOLEAN VALUES
        // TODO: ACCOUNT FOR
        if(operator.equals("||") || operator.equals("&&")){

            Boolean left_val = requireType(Boolean.class, left);
            //TODO: CHANGE THIS TO ACCOUNT FOR UNDEFINED VALUES (ACCESS)
            Boolean right_val = requireType(Boolean.class, right);

            if(operator.equals("||")){
                System.out.println("Here");
                return Environment.create(left_val || right_val);
            }
            else{
                return Environment.create(left_val && right_val);
            }

        }



        // THIS IS FOR ADDITION
        if(operator.equals("+")) {

            // String Concatenation
            if(left.getValue().getClass() == String.class || right.getValue().getClass() == String.class){
                String left_string = left.getValue().toString();
                String right_string = right.getValue().toString();

                return Environment.create(left_string + right_string);
            }
            else if(left.getValue().getClass() == BigInteger.class){
                BigInteger left_int  = (BigInteger)left.getValue();
                BigInteger right_int = requireType(BigInteger.class, right);
                BigInteger result = left_int.add(right_int);

                return Environment.create(result);
            }
            else if(left.getValue().getClass() == BigDecimal.class){
                BigDecimal left_dec = (BigDecimal)left.getValue();
                BigDecimal right_dec = requireType(BigDecimal.class, right);
                BigDecimal result  = left_dec.add(right_dec);

                return Environment.create(result);
            }
        }

        throw new UnsupportedOperationException(); //TODO

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
