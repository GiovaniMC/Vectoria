package vectoria.modid.math.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String input;
    private int position;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (Character.isDigit(current) || current == '.') {
                tokens.add(readNumber());
                continue;
            }

            if (Character.isLetter(current)) {
                tokens.add(readIdentifier());
                continue;
            }

            if (current == '\\') {
                tokens.add(readCommand());
                continue;
            }

            switch (current) {
                case '+':
                    tokens.add(new Token(TokenType.PLUS, "+"));
                    position++;
                    break;

                case '-':
                    tokens.add(new Token(TokenType.MINUS, "-"));
                    position++;
                    break;

                case '*':
                    tokens.add(new Token(TokenType.MULTIPLY, "*"));
                    position++;
                    break;

                case '/':
                    tokens.add(new Token(TokenType.DIVIDE, "/"));
                    position++;
                    break;

                case '^':
                    tokens.add(new Token(TokenType.POWER, "^"));
                    position++;
                    break;

                case '%':
                    tokens.add(new Token(TokenType.MODULO, "%"));
                    position++;
                    break;

                case '!':
                    tokens.add(new Token(TokenType.FACTORIAL, "!"));
                    position++;
                    break;

                case '_':
                    tokens.add(new Token(TokenType.UNDERSCORE, "_"));
                    position++;
                    break;

                case '(':
                    tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                    position++;
                    break;

                case ')':
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                    position++;
                    break;

                case '{':
                    tokens.add(new Token(TokenType.LEFT_BRACE, "{"));
                    position++;
                    break;

                case '}':
                    tokens.add(new Token(TokenType.RIGHT_BRACE, "}"));
                    position++;
                    break;

                case '[':
                    tokens.add(new Token(TokenType.LEFT_BRACKET, "["));
                    position++;
                    break;

                case ']':
                    tokens.add(new Token(TokenType.RIGHT_BRACKET, "]"));
                    position++;
                    break;

                case ',':
                    tokens.add(new Token(TokenType.COMMA, ","));
                    position++;
                    break;

                case '=':
                    tokens.add(new Token(TokenType.EQUAL, "="));
                    position++;
                    break;

                case '<':
                    if (peek('=')) {
                        tokens.add(new Token(TokenType.LESS_EQUAL, "<="));
                        position += 2;
                    } else {
                        tokens.add(new Token(TokenType.LESS, "<"));
                        position++;
                    }
                    break;

                case '>':
                    if (peek('=')) {
                        tokens.add(new Token(TokenType.GREATER_EQUAL, ">="));
                        position += 2;
                    } else {
                        tokens.add(new Token(TokenType.GREATER, ">"));
                        position++;
                    }
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unexpected character: '" + current
                                    + "' at position " + position
                    );
            }
        }

        return tokens;
    }

    private Token readNumber() {
        int start = position;
        boolean decimalPoint = false;

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isDigit(current)) {
                position++;
            } else if (current == '.' && !decimalPoint) {
                decimalPoint = true;
                position++;
            } else {
                break;
            }
        }

        if (position < input.length()
                && (input.charAt(position) == 'e'
                || input.charAt(position) == 'E')) {

            position++;

            if (position < input.length()
                    && (input.charAt(position) == '+'
                    || input.charAt(position) == '-')) {
                position++;
            }

            int exponentStart = position;

            while (position < input.length()
                    && Character.isDigit(input.charAt(position))) {
                position++;
            }

            if (position == exponentStart) {
                throw new IllegalArgumentException(
                        "Invalid exponent at position " + exponentStart
                );
            }
        }

        String value = input.substring(start, position);

        if (value.equals(".")) {
            throw new IllegalArgumentException(
                    "Invalid number at position " + start
            );
        }

        return new Token(TokenType.NUMBER, value);
    }

    private Token readIdentifier() {
        char character = input.charAt(position);
        position++;

        return new Token(
                TokenType.IDENTIFIER,
                String.valueOf(character)
        );
    }

    private Token readCommand() {
        int start = position;

        position++;

        if (position >= input.length()
                || !Character.isLetter(input.charAt(position))) {
            throw new IllegalArgumentException(
                    "Invalid LaTeX command at position " + start
            );
        }

        int commandStart = position;

        while (position < input.length()
                && Character.isLetter(input.charAt(position))) {
            position++;
        }

        String command = input.substring(commandStart, position);

        if (command.equals("cdot")) {
            return new Token(TokenType.MULTIPLY, "\\cdot");
        }

        return new Token(TokenType.COMMAND, command);
    }

    private boolean peek(char expected) {
        return position + 1 < input.length()
                && input.charAt(position + 1) == expected;
    }
}