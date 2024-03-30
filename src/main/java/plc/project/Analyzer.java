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
            visit(value);
            requireAssignable(type, value.getType());
        }
        ast.setVariable(scope.defineVariable(name, name, type, mutable, Environment.create(value)));
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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        Ast.Expression cond = ast.getCondition();
        visit(cond);

        if(cond.getType() == Environment.Type.BOOLEAN){
            System.out.println("Good");
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
        throw new UnsupportedOperationException();  // TODO
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
