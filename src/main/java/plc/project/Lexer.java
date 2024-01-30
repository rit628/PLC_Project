package plc.project;

import java.util.List;
import java.util.ArrayList;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
import java.util.List;
import java.util.ArrayList;

public final class Lexer {

    private final CharStream chars;
    
    // Grammar in Scuffed Chomsky Normal Form
    private static String identifierStart = "@|[A-Za-z]";
    private static String identifierEnd = "[A-Za-z0-9_-]";

    private static String numericDigit = "[0-9]";
    private static String decimalPoint = "\\.";    

    private static String escapeStart = "\\";
    private static String escapeChars = "[bnrt'\\\"\\\\]";

    private static String characterQuote = "'";
    private static String characterLiterals = "[^'\\n\\r\\\\]"; // Or escapes

    private static String stringQuote = "\\\"";
    private static String stringLiterals = "[^\\\"\\n\\r\\\\]"; // Or escapes

    private static String whitespace = "[ \\u0008\\n\\r\\t]";

    public Lexer(String input) {
        chars = new CharStream(input);

    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<Token>();
        while (chars.has(0)) {
            while (match(whitespace));
            chars.skip();
            tokens.add(lexToken());
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek(identifierStart)) {
            return lexIdentifier();
        }
        else if (peek(numericDigit) || peek("-", numericDigit)) {
            return lexNumber();
        }
        else if (peek(characterQuote)) {
            return lexCharacter();
        }
        else if (peek(stringQuote)) {
            return lexString();
        }
        else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        match(identifierStart);
        while (match(identifierEnd));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        if (peek("0") && !peek("0", decimalPoint, numericDigit)) { // If leading zero is not part of a decimal expression
            match("0");
            return chars.emit(Token.Type.INTEGER);
        }
        else if (peek("-", "0") && !peek("-", "0", decimalPoint, numericDigit)) { // If -0 not followed part of decimal expression
            throw new ParseException("Incomplete Decimal Expression", chars.index);
        }

        Token.Type deducedType = Token.Type.INTEGER;
        match("[0-9]|-");
        while (match(numericDigit));   // Match all digits left of decimal place

        if (match(decimalPoint, numericDigit)) // Check if number is a decimal
            deducedType = Token.Type.DECIMAL;   

        while (match(numericDigit));   // Match all digits to the right of decimal place

        return chars.emit(deducedType);
        
    }

    public Token lexCharacter() {
        match("'");
        if(peek("\\\\")){
            lexEscape();
        }
        else{
            if(!peek("'")) {
                match(characterLiterals);
            }
            else{
                throw new ParseException("Empty Character not Permited", chars.index);
            }

        }

        if(!peek("'")){
            throw new ParseException("Unterminated String", chars.index);
        }
        else{
            match("'");
            Token.Type detType = Token.Type.CHARACTER;
            return chars.emit(detType);
        }
    }


    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        if(!peek("\\\\", escapeChars)){ // If the lexEscape fails, then throw as exception
            throw new ParseException("Invalid Escape Sequence", chars.index);
        }
        else{
            match("\\\\", escapeChars);
        }        
    }

    public Token lexOperator() {
        if (match("!", "="));
        else if (match("=", "="));
        else if (match("&", "&"));
        else if (match("\\|", "\\|"));
        else {match(".");}
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
