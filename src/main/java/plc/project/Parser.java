package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    // Expression reserved words
    private static String reservedTrue = "TRUE";
    private static String reservedFalse = "FALSE";
    private static String reservedNil = "NIL";

    // Global reserved words
    private static String reservedList = "LIST";
    private static String reservedVar = "VAR";
    private static String reservedVal = "VAL";
    
    // Function reserved words
    private static String reservedFun = "FUN";    
    private static String reservedDo = "DO";    
    private static String reservedEnd = "END";

    // Statement reserved words
    private static String reservedLet = "LET";
    private static String reservedReturn = "RETURN";

    // Switch reserved words
    private static String reservedSwitch = "SWITCH";
    private static String reservedCase = "CASE";
    private static String reservedDefault = "DEFAULT";

    // Conditional reserved words
    private static String reservedIf = "IF";
    private static String reservedElse = "ELSE";

    // Loop reserved words
    private static String reservedWhile = "WHILE";

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    private String cleanEscapes(String literal) {
        literal = literal.replace("\\b", "\b");
        literal = literal.replace("\\n", "\n");
        literal = literal.replace("\\r", "\r");
        literal = literal.replace("\\t", "\t");
        literal = literal.replace("\\'", "\'");
        literal = literal.replace("\\\"", "\"");
        literal = literal.replace("\\\\", "\\");
        return literal;
    }

    private int getFailIndex() {
        if (tokens.has(0)) {
            Token target = tokens.get(0);
            return target.getIndex();
        }
        else if (tokens.index > 0) {
            Token target = tokens.get(-1);
            return target.getIndex() + target.getLiteral().length();
        }
        return 0;
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<Ast.Global>();
        List<Ast.Function> functions = new ArrayList<Ast.Function>();
        // Matches global*
        while (peek(reservedList) || peek(reservedVar) || peek(reservedVal)) {
            globals.add(parseGlobal());
        }
        // Matches function*
        while (peek(reservedFun)) {
            functions.add(parseFunction());
        }
        if (tokens.has(0)) {
            throw new ParseException("Function Definition Followed By Global Declaration.", getFailIndex());
        }
        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global global;
        if (peek(reservedList)) {
            global = parseList();
        }
        else if (peek(reservedVar)) {
            global = parseMutable();
        }
        else if (peek(reservedVal)) {
            global = parseImmutable();
        }
        else {
            throw new ParseException("Invalid Global.", getFailIndex());
        }
        if (!match(";")) {
            throw new ParseException("Missing Semicolon.", getFailIndex());
        }
        return global;
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match(reservedList);
        if (!peek(Token.Type.IDENTIFIER, "=")) {
            match(Token.Type.IDENTIFIER); // Updates fail index if valid identifier is given
            throw new ParseException("Invalid List Declaration.", getFailIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER, "=");
        if (!match("[")) {
            throw new ParseException("Missing Opening Bracket.", getFailIndex());
        }
        List<Ast.Expression> values = new ArrayList<Ast.Expression>();
        do {
            values.add(parseExpression());
        } while (match(","));
        if (!match("]")) {
            throw new ParseException("Unclosed List Declaration.", getFailIndex());
        }
        return new Ast.Global(name, true, Optional.of(new Ast.Expression.PlcList(values)));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match(reservedVar);
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid Identifier.", getFailIndex());
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        return new Ast.Global(name, true, value);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match(reservedVal);
        if (!peek(Token.Type.IDENTIFIER, "=")) {
            match(Token.Type.IDENTIFIER); // Updates fail index if valid identifier is given
            throw new ParseException("Invalid Immutable Declaration.", getFailIndex());
        }
        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER, "=");
        Optional<Ast.Expression> value = Optional.of(parseExpression());
        return new Ast.Global(name, false, value);
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match(reservedFun);
        if (!peek(Token.Type.IDENTIFIER, "(")) {
            match(Token.Type.IDENTIFIER); // Updates fail index if valid identifier is given
            throw new ParseException("Invalid Function Definition.", getFailIndex());
        }

        String name = tokens.get(0).getLiteral();
        match(Token.Type.IDENTIFIER, "(");
        List<String> params = new ArrayList<String>();

        // Parses parameter list
        if (!peek(")")) {  // Checks if function has no args ex: f()
            do {
                if (match(Token.Type.IDENTIFIER)) {
                    params.add(tokens.get(-1).getLiteral());
                }
                else {
                    throw new ParseException("Invalid Function Parameter.", getFailIndex());
                }
            } while (match(","));
        }
        if (!match(")")) {  // Checks for missing closing parenthesis
            throw new ParseException("Unclosed Function Parameter List.", getFailIndex());
        }

        // Parses function implementation
        if (!match(reservedDo)) {
            throw new ParseException("Missing " + reservedDo + " Keyword.", getFailIndex());
        }
        List<Ast.Statement> statements = parseBlock();
        if (!match(reservedEnd)) {
            throw new ParseException("Missing " + reservedEnd + " Keyword.", getFailIndex());
        }
        return new Ast.Function(name, params, statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while (!(peek(reservedEnd) || peek(reservedCase) || peek(reservedDefault) || peek(reservedElse))) {
            statements.add(parseStatement());
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
      
        if (peek(reservedLet)){
            return parseDeclarationStatement();
        }
        else if (peek(reservedSwitch)){
            return parseSwitchStatement();   
        }
        else if (peek(reservedIf)) {
            return parseIfStatement();
        }
        else if (peek(reservedWhile)) {
            return parseWhileStatement();
        }
        else if (peek(reservedReturn)) {
            return parseReturnStatement();
        }
        else {
            Ast.Expression LHS = parseExpression();
            if (match("=")) {
                Ast.Expression RHS = parseExpression();
                if (!match(";")) {
                    throw new ParseException("Expected Semicolon.", getFailIndex());
                }
                return new Ast.Statement.Assignment(LHS, RHS);
            }
            if (!match(";")) {
                throw new ParseException("Expected Semicolon.", getFailIndex());
            }
            return new Ast.Statement.Expression(LHS);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match(reservedLet);
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid Identifier.", getFailIndex());
        }
        String var_name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> RHS_val = Optional.empty();
        
        if (match("=")){
            RHS_val = Optional.of(parseExpression());
        }
        
        if(match(";")) {
            Ast.Statement.Declaration DecStat = new Ast.Statement.Declaration(var_name, RHS_val);
            return DecStat;
        }
        else {
            throw new ParseException("Expected Semicolon.", getFailIndex()); 
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match(reservedIf);
        Ast.Expression condition = parseExpression();
        if (!match(reservedDo)) {
            throw new ParseException("Missing " + reservedDo + " Keyword", getFailIndex());
        }
        List<Ast.Statement> thenStatements = parseBlock();
        List<Ast.Statement> elseStatements = new ArrayList<Ast.Statement>();
        if (match(reservedElse)) {
            elseStatements = parseBlock();
        }
        if (!match(reservedEnd)) {
            throw new ParseException("Missing " + reservedEnd + " Keyword.", getFailIndex());
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match(reservedSwitch);
        Ast.Expression condition = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<Ast.Statement.Case>();
        while (peek(reservedCase)) {
            cases.add(parseCaseStatement());
        }
        if (!peek(reservedDefault)) {
            throw new ParseException("Missing " + reservedDefault + " Keyword", getFailIndex());
        }
        cases.add(parseCaseStatement());
        if (!match(reservedEnd)) {
            throw new ParseException("Missing " + reservedEnd + " Keyword", getFailIndex());
        }
        return new Ast.Statement.Switch(condition, cases);
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        Optional<Ast.Expression> value = Optional.empty(); 
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>(); 
        if (match(reservedCase)){
            value = Optional.of(parseExpression()); 
            if (match(":")){
                statements = parseBlock();
            }
            else {
                throw new ParseException("Missing Colon.", getFailIndex());
            }
        }
        else if (match(reservedDefault)){
            statements = parseBlock();
        }    

        Ast.Statement.Case CaseStatement = new Ast.Statement.Case(value, statements);
        return CaseStatement;
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match(reservedWhile);
        Ast.Expression condition = parseExpression();
        if (!match(reservedDo)) {            throw new ParseException("Missing " + reservedDo + " Keyword", getFailIndex());
        }
        List<Ast.Statement> statements = parseBlock();
        if (!match(reservedEnd)) {
            throw new ParseException("Missing " + reservedEnd + " Keyword.", getFailIndex());
        }
        return new Ast.Statement.While(condition, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match(reservedReturn);
        Ast.Expression value = parseExpression();
        if (!match(";")) {
            throw new ParseException("Missing Semicolon.", getFailIndex());
        }
        return new Ast.Statement.Return(value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression(); 
    }
    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression sub_exp = parseComparisonExpression(); 

        while (peek("&&")||peek("||")){
            
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR); 
            Ast.Expression right = parseComparisonExpression(); 
            
            Ast.Expression new_Bin = new Ast.Expression.Binary(operator, sub_exp, right);
            sub_exp = new_Bin; 
    
        }      
        return sub_exp; 
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression sub_exp = parseAdditiveExpression(); 

        while (peek("<")||peek(">")||peek("==")||peek("!=")){
            
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR); 
            Ast.Expression right = parseAdditiveExpression(); 
            
            Ast.Expression new_Bin = new Ast.Expression.Binary(operator, sub_exp, right);
            sub_exp = new_Bin; 
    
        }      
        return sub_exp; 
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression sub_exp = parseMultiplicativeExpression(); 

        while (peek("+")||peek("-")){
            
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR); 
            Ast.Expression right = parseMultiplicativeExpression(); 
            
            Ast.Expression new_Bin = new Ast.Expression.Binary(operator, sub_exp, right);
            sub_exp = new_Bin; 
    
        }      
        return sub_exp; 
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression sub_exp = parsePrimaryExpression(); 

        while (peek("*")||peek("/")||peek("^")){
            
            String operator = tokens.get(0).getLiteral();
            match(Token.Type.OPERATOR); 
            Ast.Expression right = parsePrimaryExpression(); 
            
            Ast.Expression new_Bin = new Ast.Expression.Binary(operator, sub_exp, right);
            sub_exp = new_Bin; 
    
        }      
        return sub_exp; 

    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (match(reservedNil)) {
            return new Ast.Expression.Literal(null);
        }
        else if (match(reservedTrue)) {
            return new Ast.Expression.Literal(Boolean.TRUE);
        }
        else if (match(reservedFalse)) {
            return new Ast.Expression.Literal(Boolean.FALSE);
        }
        else if (peek(Token.Type.INTEGER)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.INTEGER);

            return new Ast.Expression.Literal(new BigInteger(literal));
        }
        else if (peek(Token.Type.DECIMAL)) {
            String literal = tokens.get(0).getLiteral();
            match(Token.Type.DECIMAL);

            return new Ast.Expression.Literal(new BigDecimal(literal));
        }
        else if (peek(Token.Type.CHARACTER)) {
            String literal = tokens.get(0).getLiteral();
            // Replaces opening and closing '
            literal = literal.replaceAll("^\'", "").replaceAll("\'$", ""); 
            literal = cleanEscapes(literal);
            Character literalChar = literal.charAt(0);
            match(Token.Type.CHARACTER);

            return new Ast.Expression.Literal(literalChar);
        }
        else if (peek(Token.Type.STRING)) {
            String literal = tokens.get(0).getLiteral();
            // Replaces opening and closing "
            String literalString = literal.replaceAll("^\"", "").replaceAll("\"$", "");
            literalString = cleanEscapes(literalString);
            match(Token.Type.STRING);

            return new Ast.Expression.Literal(literalString);
        }
        else if (match("(")) {  // Parse group expression
            Ast.Expression innerExpr = parseExpression();

            if (!match(")")) {
                throw new ParseException("Unclosed Group Expression.", getFailIndex());
            }

            return new Ast.Expression.Group(innerExpr);
        }
        else if (peek(Token.Type.IDENTIFIER, "(")) {  // Parse function expression
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER, "(");
            List<Ast.Expression> args = new ArrayList<Ast.Expression>();
            if (!peek(")")) {  // Checks if function has no args ex: f()
                do {
                    args.add(parseExpression());
                } while (match(","));
            }
            if (!match(")")) {  // Checks for missing closing parenthesis
                throw new ParseException("Unclosed Function Expression.", getFailIndex());
            }
            return new Ast.Expression.Function(name, args);
        }
        else if (peek(Token.Type.IDENTIFIER)) {  // Parse access expression
            Optional<Ast.Expression> offset = Optional.empty();
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            // Parses list access
            if (match("[")) {
                offset = Optional.of(parseExpression());
                if (!match("]")) {
                    throw new ParseException("Unclosed List Access", getFailIndex());
                }
            }

            return new Ast.Expression.Access(offset, name);
        }
        throw new ParseException("Invalid Expression.", getFailIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
