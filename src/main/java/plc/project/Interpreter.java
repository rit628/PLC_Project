package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

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
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Optional<Ast.Expression> astVal = ast.getValue();
        if (astVal.isPresent()){
            scope.defineVariable(ast.getName(),
                    true, visit(astVal.get()));
        }
        else{
            scope.defineVariable(ast.getName(),
                    true, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (ast.getReceiver().getClass() != Ast.Expression.Access.class) {
            throw new RuntimeException("RHS must be an access expression.");
        }
        Ast.Expression.Access LHS = Ast.Expression.Access.class.cast(ast.getReceiver());
        visit(LHS);
        String varName = LHS.getName();
        Environment.Variable var = scope.lookupVariable(varName);
        if (!var.getMutable()) {
            throw new RuntimeException(var.getName() + " is not mutable.");
        }
        Ast.Expression RHS = ast.getValue();
        if (LHS.getOffset().isEmpty()) {
            var.setValue(visit(RHS));
        }
        else {
            Object newVal = visit(RHS).getValue();
            List<Object> listVals = requireType(List.class, var.getValue());
            int offset = requireType(BigInteger.class, visit(LHS.getOffset().get())).intValue();
            listVals.set(offset, newVal);
            var.setValue(Environment.create(listVals));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if ( requireType( Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;        
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
        while( requireType( Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
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

        String operator = ast.getOperator();

        //THIS IS FOR BOOLEAN VALUES
        if(operator.equals("||") || operator.equals("&&")) {
            Environment.PlcObject left = visit(ast.getLeft());
            Boolean left_val = requireType(Boolean.class, left);

            if (operator.equals("||")) {
                if (left_val.equals(true)) {
                    return Environment.create(true);
                } else {
                    Environment.PlcObject right = visit(ast.getRight());
                    Boolean right_val = requireType(Boolean.class, right);
                    if (right_val.equals(true)) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                }
            } else {
                if (left_val.equals(false)) {
                    return Environment.create(false);
                } else {
                    Environment.PlcObject right = visit(ast.getRight());
                    Boolean right_val = requireType(Boolean.class, right);
                    if (right_val.equals(true)) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                }

            }
        }

        if(operator.equals("<") || operator.equals(">")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            int result = 0;

            if (left.getValue().getClass() == BigInteger.class) {
                BigInteger right_val = requireType(BigInteger.class, right);
                result = ((BigInteger) left.getValue()).compareTo(right_val);
            } else if (left.getValue().getClass() == BigDecimal.class) {
                BigDecimal right_val = requireType(BigDecimal.class, right);
                result = ((BigDecimal) left.getValue()).compareTo(right_val);
            } else if (left.getValue().getClass() == String.class) {
                String right_val = requireType(String.class, right);
                result = ((String) left.getValue()).compareTo(right_val);
            } else if (left.getValue().getClass() == Character.class) {
                Character right_val = requireType(Character.class, right);
                result = ((Character) left.getValue()).compareTo(right_val);
            }

            // If lhs less than
            if(operator.equals("<")){
                if(result < 0){
                    return Environment.create(true);
                }
                else{
                    return Environment.create(false);
                }

            }
            else{
                if(result > 0){
                    return Environment.create(false);
                }
                else{
                    return Environment.create(true);
                }

            }
        }



        // THis is for ==/!= operations
        if(operator.equals("==") || operator.equals("!=")){
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            Boolean is_equal = left.getValue().equals(right.getValue());
            if(operator.equals("!=")) {
                return Environment.create(!is_equal);
            }
            else{
                return Environment.create(is_equal);
            }
        }

        // This is for */- operations
        if(operator.equals("*") || operator.equals("-")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            if (left.getValue().getClass() == BigInteger.class || left.getValue().getClass() == BigDecimal.class) {

                if (left.getValue().getClass() == BigInteger.class) {
                    BigInteger left_val = (BigInteger) left.getValue();
                    BigInteger right_val = requireType(BigInteger.class, right);
                    BigInteger Resultant;
                    if(operator.equals("*")) {
                        Resultant = left_val.multiply(right_val);
                    }
                    else{
                        Resultant = left_val.subtract(right_val);
                    }

                    return Environment.create(Resultant);

                } else {
                    BigDecimal left_val = (BigDecimal) left.getValue();
                    BigDecimal right_val = requireType(BigDecimal.class, right);
                    BigDecimal Resultant;
                    if(operator.equals("*")) {
                        Resultant = left_val.multiply(right_val);
                    }
                    else{
                        Resultant = left_val.subtract(right_val);
                    }

                    return Environment.create(Resultant);
                }
            }
            else{
                throw new RuntimeException("Invalid Multiplication");
            }
        }

        //This is for / operation

        if(operator.equals("/")){
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            if (left.getValue().getClass() == BigInteger.class || left.getValue().getClass() == BigDecimal.class) {

                if (left.getValue().getClass() == BigInteger.class) {
                    BigInteger left_val = (BigInteger) left.getValue();
                    BigInteger right_val = requireType(BigInteger.class, right);
                    if(right_val.equals(BigInteger.ZERO)){
                        throw new RuntimeException("Invalid Division");
                    }
                    else{
                        BigInteger Resultant = left_val.divide(right_val);
                        return Environment.create(Resultant);
                    }


                } else {
                    BigDecimal left_val = (BigDecimal) left.getValue();
                    BigDecimal right_val = requireType(BigDecimal.class, right);
                    if(right_val.equals(BigDecimal.ZERO)){
                        throw new RuntimeException("Invalid Division");
                    }
                    else{
                        BigDecimal Resultant = left_val.divide(right_val, RoundingMode.HALF_EVEN);
                        return Environment.create(Resultant);
                    }
                }
            }
            else{
                throw new RuntimeException("Invalid Division");
            }
        }

        if(operator.equals("^")){
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());
            if (left.getValue().getClass() == BigInteger.class) {
                BigInteger left_val = (BigInteger) left.getValue();
                BigInteger right_val = requireType(BigInteger.class, right);
                BigInteger  MaxInt;
                BigInteger Remainder;
                BigInteger Total;
                MaxInt = BigInteger.valueOf(Integer.MAX_VALUE);



                if(right_val.compareTo(MaxInt) > 0){
                    BigInteger int_divide = right_val.divide(MaxInt);
                    Remainder = right_val.subtract(int_divide.multiply(MaxInt));
                    Total = left_val.pow(Integer.MAX_VALUE);
                    for(int i = 0; i < int_divide.intValue() - 1; i++){
                        Total = Total.multiply(left_val.pow(Integer.MAX_VALUE));
                    }
                    Total = Total.multiply(left_val.pow(Remainder.intValue()));
                    return Environment.create(Total);
                }
                else {
                    BigInteger Resultant = left_val.pow(right_val.intValue());
                    return Environment.create(Resultant);

                }
        }
            else{
                throw new RuntimeException("Invalid Exponentiation");
            }
        }

        
        // THIS IS FOR ADDITION
        if(operator.equals("+")) {
            Environment.PlcObject right = visit(ast.getRight());
            Environment.PlcObject left = visit(ast.getLeft());

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
        throw new RuntimeException("Invalid Binary Expression.");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        String name = ast.getName();
        Environment.Variable var = scope.lookupVariable(name);
        if (ast.getOffset().isEmpty()) {
            return var.getValue();
        }
        BigInteger offset = requireType(BigInteger.class, visit(ast.getOffset().get()));
        List<Object> listVals = requireType(List.class, var.getValue());
        if (offset.intValue() < 0 || offset.intValue() >= listVals.size()) {
            throw new RuntimeException("Invalid index for list " + var.getName() + ".");
        }
        Object indexVal = listVals.get(offset.intValue());
        if (indexVal.getClass() == Environment.PlcObject.class) {
            return Environment.PlcObject.class.cast(indexVal);
        }
        return Environment.create(indexVal);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        String name = ast.getName();
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.PlcObject> argObjects = new ArrayList<Environment.PlcObject>();
        for (Ast.Expression arg : args) {
            argObjects.add(visit(arg));
        }
        Environment.Function fun = scope.lookupFunction(name, args.size());
        return fun.invoke(argObjects);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Ast.Expression> values = ast.getValues();
        List<Object> objects = new ArrayList<Object>();
        for (Ast.Expression expression : values) {
            objects.add(visit(expression).getValue());
        }
        return Environment.create(objects);
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
