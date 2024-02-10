package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Starts with @ Symbol", "@juiceWrld", true),

                Arguments.of("Second Character @ Symbol", "j@wrld", false),
                Arguments.of("Ends with @ Symbol", "juiceWrld@", false),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Trailing Zeros", "100000", true),
                Arguments.of("Zero", "0", true),
                Arguments.of("Middle Zero", "102", true),

                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Multiple Zeros", "00", false),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Positive Decimal", "0.1", false),
                Arguments.of("Negative Decimal", "-0.1", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("One", "1.0", true),
                Arguments.of("Zero", "0.0", true),
                Arguments.of("Negative One", "-1.0", true),
                Arguments.of("One Hundred and Twenty Three", "123.0", true),
                Arguments.of("Negative One Hundred and Twenty Three", "-123.0", true),
                Arguments.of("Fifteen Hundreths", "0.15", true),
                Arguments.of("Negative Fifteen Hundreths", "-0.15", true),
                Arguments.of("Negative Zero", "-0.0", true),

                Arguments.of("No Decimal", "1", false),
                Arguments.of("No Integer", ".1", false),
                Arguments.of("No Integer with Trailing 0", ".0", false),
                Arguments.of("Integer with Trailing Decimal Point", "1.", false),
                Arguments.of("Zero with Trailing Decimal Point", "0.", false),
                Arguments.of("Leading Zeroes", "0123.0", false),
                Arguments.of("Negative Leading Zeroes", "-0123.0", false),
                Arguments.of("Negative Decimal No Integer", "-.15", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Backspace Escape", "\'\\b\'", true),
                Arguments.of("Escaped Slash", "\'\\\\\'", true),
                Arguments.of("Escaped Quote", "\'\\'\'", true),
                Arguments.of("Invisible Character", "\'\u0000\'", true),
                Arguments.of("Tab Character", "\'\t\'", true),
                Arguments.of("Vertical Tab Character", "\'\u000B\'", true),
                Arguments.of("Backspace Character", "\'\b\'", true),
                Arguments.of("Form Feed", "\'\f\'", true),

                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Triple Quote", "\'\'\'", false),
                Arguments.of("Unclosed", "\'", false),
                Arguments.of("Unclosed with letter", "\'a", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Unescaped Backslash", "\'\\\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Normal String", "\"Hello world!\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Ends with Escape", "\"test_string\\n\"", true),
                Arguments.of("Proper Quote Escape", "\"Hello \\\"Bob\\\" \"", true),
                Arguments.of("Multiple Escapes", "\"test\\nstring\\nnew\"", true),
                Arguments.of("Escape Sequences", "\"\\b\\r\\t\\n\\'\\\"\\\\\"", true),
                Arguments.of("Special Characters", "\"`~!@#$%^&*()-_=+[{]}|;:,<.>/? \"", true),
                Arguments.of("Escaped Nested Quote", "\"\\\"\"", true),
                Arguments.of("Escaped Nested String", "\"\\\"\\\"\"", true),
                Arguments.of("Active Backspace", "\"\b\"", true),
                Arguments.of("Active Tab", "\"\t\"", true),
                Arguments.of("Invisible Character", "\"\u0000\"", true),
                Arguments.of("Form Feed", "\"\f\"", true),
                Arguments.of("Vertical Tab", "\"\u000B\"", true),
                Arguments.of("Escaped Backslash", "\"\\\\\"", true),


                Arguments.of("Non Escaped Backslash", "\"\\\"", false),
                Arguments.of("Literal Empty String", "", false),
                Arguments.of("Invalid Escape Sequences", "\"\\q\\w\\e\\1\\2\\`\\&\"", false),
                Arguments.of("Unterminated String", "\"", false),
                Arguments.of("Nested String", "\"\"\"\"", false),
                Arguments.of("Nested Quote", "\"\"\"", false),
                Arguments.of("Escaped Invisible Character", "\"\\u0000\"", false),
                Arguments.of("String Literal Broken by Newline", "\"\n\"", false),
                Arguments.of("String Literal Broken by Carriage Return", "\"\r\"", false),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Newline in String", "\"juice\nworld\"", false),
                Arguments.of("Newline in String End", "\"juice world\n\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Comparison 2", "==", true),
                Arguments.of("And Operator", "&&", true),
                Arguments.of("Or Operator", "||", true),
                Arguments.of("Bitwise And Operator", "&", true),
                Arguments.of("Bitwise Or Operator", "|", true),
                Arguments.of("Semicolon", ";", true),
                Arguments.of("Minus Sign", "-", true),
                Arguments.of("Plus Sign", "+", true),
                Arguments.of("Form Feed", "\f", true),
                Arguments.of("Vertical Tab", "\u000B", true),

                Arguments.of("Triple Equals", "===", false),
                Arguments.of("Triple Not Equals", "!==", false),
                Arguments.of("Double Minus Sign", "--", false),
                Arguments.of("Not And", "!&&", false),
                Arguments.of("Not Or", "!||", false),
                Arguments.of("Identifier Character", "a", false),
                Arguments.of("At Symbol", "@", false),
                Arguments.of("Digit", "1", false),
                Arguments.of("Character Quote", "'", false),
                Arguments.of("String Quote", "\"", false),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Newline", "\n", false),
                Arguments.of("Carriage Return", "\r", false),
                Arguments.of("Backspace", "\b", false),
                Arguments.of("Empty String", "", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
               Arguments.of("String Example", "\"Hello, World!\" \"This is string 2\"", Arrays.asList(
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 0),
                        new Token(Token.Type.STRING, "\"This is string 2\"", 16)
                )),
                Arguments.of("Leading Hyphen", "-five", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.IDENTIFIER, "five", 1)
                )),
                Arguments.of("Leading Digits", "1fish2fish3fishbluefish", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.IDENTIFIER, "fish2fish3fishbluefish", 1)
                )),
                Arguments.of("Spaced Integers", "05 394 98.81", Arrays.asList(
                        new Token(Token.Type.INTEGER, "0", 0),
                        new Token(Token.Type.INTEGER, "5", 1),
                        new Token(Token.Type.INTEGER, "394", 3),
                    new Token(Token.Type.DECIMAL, "98.81", 7)
                )),
                Arguments.of("Trailing Newline", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                )),
                Arguments.of("Negative Zero with Method Call", "-0.toString()", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0),
                        new Token(Token.Type.INTEGER, "0", 1),
                        new Token(Token.Type.OPERATOR, ".", 2),
                        new Token(Token.Type.IDENTIFIER, "toString", 3),
                        new Token(Token.Type.OPERATOR, "(", 11),
                        new Token(Token.Type.OPERATOR, ")", 12)
                )),
                Arguments.of("Negative One with Method Call", "-1.toString()", Arrays.asList(
                        new Token(Token.Type.INTEGER, "-1", 0),
                        new Token(Token.Type.OPERATOR, ".", 2),
                        new Token(Token.Type.IDENTIFIER, "toString", 3),
                        new Token(Token.Type.OPERATOR, "(", 11),
                        new Token(Token.Type.OPERATOR, ")", 12)
                )),
                Arguments.of("Negative Zero Decimal with Method Call", "-0.0.toString()", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-0.0", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.IDENTIFIER, "toString", 5),
                        new Token(Token.Type.OPERATOR, "(", 13),
                        new Token(Token.Type.OPERATOR, ")", 14)
                )),
                Arguments.of("Negative One Tenth with Method Call", "-0.1.toString()", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-0.1", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.IDENTIFIER, "toString", 5),
                        new Token(Token.Type.OPERATOR, "(", 13),
                        new Token(Token.Type.OPERATOR, ")", 14)
                )),
                Arguments.of("Negative One Tenth + Trailing Zero with Method Call", "-0.10.toString()", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-0.10", 0),
                        new Token(Token.Type.OPERATOR, ".", 5),
                        new Token(Token.Type.IDENTIFIER, "toString", 6),
                        new Token(Token.Type.OPERATOR, "(", 14),
                        new Token(Token.Type.OPERATOR, ")", 15)
                )),
                Arguments.of("Negative One Decimal with Method Call", "-1.0.toString()", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "-1.0", 0),
                        new Token(Token.Type.OPERATOR, ".", 4),
                        new Token(Token.Type.IDENTIFIER, "toString", 5),
                        new Token(Token.Type.OPERATOR, "(", 13),
                        new Token(Token.Type.OPERATOR, ")", 14)
                )),
                Arguments.of("Multiple Dots", "1.2.3", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.2", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "3", 4)
                )),
                Arguments.of("All Whitespace", " \b\n\r\t", Arrays.asList()),
                Arguments.of("Complex Lex Combination", "LET @boogie2988= (\"Juice\\nWrld !.\".toInt() + 99.9 - 999).toString() == ' '+ -999.toString()", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "@boogie2988", 4),
                        new Token(Token.Type.OPERATOR, "=", 15),
                        new Token(Token.Type.OPERATOR, "(", 17),
                        new Token(Token.Type.STRING, "\"Juice\\nWrld !.\"", 18),
                        new Token(Token.Type.OPERATOR, ".", 34),
                        new Token(Token.Type.IDENTIFIER, "toInt", 35),
                        new Token(Token.Type.OPERATOR, "(", 40),
                        new Token(Token.Type.OPERATOR, ")", 41),
                        new Token(Token.Type.OPERATOR, "+", 43),
                        new Token(Token.Type.DECIMAL, "99.9", 45),
                        new Token(Token.Type.OPERATOR, "-", 50),
                        new Token(Token.Type.INTEGER, "999", 52),
                        new Token(Token.Type.OPERATOR, ")", 55),
                        new Token(Token.Type.OPERATOR, ".", 56),
                        new Token(Token.Type.IDENTIFIER, "toString", 57),
                        new Token(Token.Type.OPERATOR, "(", 65),
                        new Token(Token.Type.OPERATOR, ")", 66),
                        new Token(Token.Type.OPERATOR, "==", 68),
                        new Token(Token.Type.CHARACTER, "\' \'", 71),
                        new Token(Token.Type.OPERATOR, "+", 74),
                        new Token(Token.Type.INTEGER, "-999", 76),
                        new Token(Token.Type.OPERATOR, ".", 80),
                        new Token(Token.Type.IDENTIFIER, "toString", 81),
                        new Token(Token.Type.OPERATOR, "(", 89),
                        new Token(Token.Type.OPERATOR, ")", 90)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    @Test
    void testEscapeException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"invalid\\escape\"").lex());
        Assertions.assertEquals(9, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
