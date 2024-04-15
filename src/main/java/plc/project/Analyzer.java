package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import plc.project.Environment.PlcObject;

import java.util.Optional;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function func : ast.getFunctions()) {
            visit(func);
        }
        if (scope.lookupFunction("main", 0).getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException("Function main must return an integer.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        String name = ast.getName();
        boolean mutable = ast.getMutable();
        Environment.Type type = Environment.getType(ast.getTypeName());
        Ast.Expression value = null;
        if (ast.getValue().isPresent()) {
            value = ast.getValue().get();
            if (value instanceof Ast.Expression.PlcList) {
                ((Ast.Expression.PlcList)value).setType(type);
            }
            visit(value);
            requireAssignable(type, value.getType());
        }
        ast.setVariable(scope.defineVariable(name, name, type, mutable, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String name = ast.getName();
        List<String> paramTypesNames = ast.getParameterTypeNames();
        List<Environment.Type> paramTypes = new ArrayList<Environment.Type>();
        Optional<String> returnTypeName = ast.getReturnTypeName();
        Environment.Type returnType = Environment.Type.NIL;
        java.util.function.Function<List<PlcObject>, PlcObject> lambda = (args) -> Environment.NIL;
        for (String paramTypename : paramTypesNames) {
            paramTypes.add(Environment.getType(paramTypename));
        }
        if (returnTypeName.isPresent()) {
            returnType = Environment.getType(returnTypeName.get());
        }
        ast.setFunction(scope.defineFunction(name, name, paramTypes, returnType, lambda));
        function = ast;
        Scope oldScope = scope;
        scope = new Scope(scope);
        List<String> parameters = ast.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            scope.defineVariable(parameters.get(i), parameters.get(i), paramTypes.get(i), true, Environment.NIL);
        }
        List<Ast.Statement> statements = ast.getStatements();
        for (Ast.Statement statement : statements) {
            visit(statement);
        }
        scope = oldScope;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (ast.getExpression().getClass() != Ast.Expression.Function.class) {
            throw new RuntimeException("Statement expression must be a function invokation.");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        String name = ast.getName();
        Environment.Type type = null;
        Ast.Expression value = null;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }
        if (ast.getValue().isPresent()) {
            value = ast.getValue().get();
            visit(value);
            if (type != null) {
                requireAssignable(type, value.getType());
            }
            else {
                type = value.getType();
            }
        }
        if (type == null && value == null) {
            throw new RuntimeException("Declaration must contain a type annotation or initialization.");
        }
        ast.setVariable(scope.defineVariable(name, name, type, true, Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        Ast.Expression receiver = ast.getReceiver();
        Ast.Expression value = ast.getValue();
        if (receiver.getClass() != Ast.Expression.Access.class) {
            throw new RuntimeException("Receiver must be access expression.");
        }
        visit(receiver);
        visit(value);
        requireAssignable(receiver.getType(), value.getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Expression cond = ast.getCondition();
        visit(cond);
        if(cond.getType() != Environment.Type.BOOLEAN){
            throw new RuntimeException("Condition not a boolean");
        }

        if(ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then Not present");
        }
        else{
            for(Ast.Statement stmt : ast.getThenStatements()){
                visit(stmt);
            }
            for(Ast.Statement stmt : ast.getElseStatements()){
                visit(stmt);
            }
        }
        visit(cond);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        Environment.Type cond_type = ast.getCondition().getType();
        List<Ast.Statement.Case> switch_cases = ast.getCases();
        Boolean default_reached = false;


        // Perform the validation checks:
        for(Ast.Statement.Case cs : switch_cases){
            if(cs.getValue().isPresent()) {
                Ast.Expression case_value = cs.getValue().get();
                visit(case_value);
                Environment.Type case_cond = case_value.getType();
                if(cond_type != case_cond){
                    throw new RuntimeException("Case type does not match switch condition type");
                }
                else{
                    visit(cs);
                }
            }
            // Handle the default Condition:
            else{
                default_reached = true;
                visit(cs);
            }
        }

        if(!default_reached){
            throw new RuntimeException("Non Valued Default found");
        }
        return null;

    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        for (Ast.Statement.Statement stmt : ast.getStatements()){
            visit(stmt);
        }
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        Ast.Expression cond = ast.getCondition();
        List<Ast.Statement> statements = ast.getStatements();
        visit(cond);

        if(cond.getType() == Environment.Type.BOOLEAN){
            scope = new Scope(scope);
            for(Ast.Statement st : statements){
                visit(st);
            }
            scope = scope.getParent();
        }
        else{
            throw new RuntimeException("Bad Condition");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Type returnType = scope.lookupFunction(function.getName(), function.getParameters().size()).getReturnType();
        requireAssignable(returnType, ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal == null) {
            ast.setType(Environment.Type.NIL);
        }
        else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (literal instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else if (literal instanceof BigInteger) {
            try {
                ((BigInteger)literal).intValueExact();
            } catch (ArithmeticException e) {
                throw new RuntimeException("Integer value out of range.");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if (literal instanceof BigDecimal) {
            double value = ((BigDecimal)literal).doubleValue();
            if (
                value == Double.POSITIVE_INFINITY ||
                value == Double.NEGATIVE_INFINITY || 
                (value == 0.0 && !literal.equals(BigDecimal.ZERO))
            ) {
                throw new RuntimeException("Decimal value out of range.");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (ast.getExpression().getClass() != Ast.Expression.Binary.class) {
            throw new RuntimeException("Group expression must be a binary expression.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        String operator = ast.getOperator();
        if(operator.equals("&&") || operator.equals("||")){
            if(ast.getLeft().getType() != Environment.Type.BOOLEAN || ast.getRight().getType() != Environment.Type.BOOLEAN){
                throw new RuntimeException("Operator Failed");
            }
            else{
                ast.setType(Environment.Type.BOOLEAN);
            }
        }
        else if(operator.equals("<") || operator.equals(">") || operator.equals("==") || operator.equals("!=")){
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            if (ast.getLeft().getType() == ast.getRight().getType()) {
                ast.setType(Environment.Type.BOOLEAN);
            }
            else{
                throw new RuntimeException("Operators of not same type");
            }
        }
        else if(operator.equals("+")){
            if(ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING){
                ast.setType(Environment.Type.STRING);
            }
            else if(ast.getLeft().getType() == ast.getRight().getType()){
                if(ast.getLeft().getType() == Environment.Type.INTEGER){
                    ast.setType(Environment.Type.INTEGER);
                }
                else if(ast.getLeft().getType() == Environment.Type.DECIMAL){
                    ast.setType(Environment.Type.DECIMAL);
                }
                else{
                    throw new RuntimeException("Unacceptable addition");
                }
            }
            else{
                throw new RuntimeException("Uncompatible data types for addition");
            }
        }
        else if(operator.equals("-") || operator.equals("*") || operator.equals("/")){
            if(ast.getLeft().getType() == Environment.Type.INTEGER || ast.getLeft().getType() == Environment.Type.DECIMAL){
                if(ast.getRight().getType() == ast.getLeft().getType()){
                    ast.setType(ast.getLeft().getType());
                }
                else{
                    throw new RuntimeException("RHS type is not compatible with LHS type");
                }
            }
            else{
                throw new RuntimeException("LHS is not of acceptable type");
            }

        }
        else if(operator.equals("^")){
            if(ast.getLeft().getType() == Environment.Type.INTEGER && ast.getRight().getType() == Environment.Type.INTEGER){
                ast.setType(Environment.Type.INTEGER);
            }
            else{
                throw new RuntimeException("Unacceptable exponentiation type");
            }

        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        String name = ast.getName();
        Environment.Variable variable = scope.lookupVariable(name);
        Optional<Ast.Expression> offset = ast.getOffset();
        if (offset.isPresent()) {
            visit(offset.get());
            if (offset.get().getType() != Environment.Type.INTEGER) {
                throw new RuntimeException("Offset must be an integer.");
            }
        }
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        List<Ast.Expression> args = ast.getArguments();
        Environment.Function function = scope.lookupFunction(ast.getName(), args.size());
        List<Environment.Type> paramsTypes = function.getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            visit(args.get(i));
            requireAssignable(paramsTypes.get(i), args.get(i).getType());
        }
        ast.setFunction(function);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        Environment.Type type = ast.getType();
        for (Ast.Expression expression : ast.getValues()) {
            visit(expression);
            requireAssignable(type, expression.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (
            target == Environment.Type.ANY ||
            target == Environment.Type.COMPARABLE && (type.getScope().getParent() == Environment.Type.COMPARABLE.getScope()) ||
            target == type
        ) {
            return;
        }
        throw new RuntimeException("Types " + target.toString() + " and " + type.toString() + " are not assignable.");
    }

}