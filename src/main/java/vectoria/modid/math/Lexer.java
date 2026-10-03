package vectoria.modid.math;

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

            if (Character.isLetter(current) || current == '_') {
                tokens.add(readIdentifier());
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

                case '(':
                    tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                    position++;
                    break;

                case ')':
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
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
                            "Unexpected character: '" + current + "' at position " + position
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
                && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {

            position++;

            if (position < input.length()
                    && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
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
        int start = position;

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isLetterOrDigit(current) || current == '_') {
                position++;
            } else {
                break;
            }
        }

        return new Token(
                TokenType.IDENTIFIER,
                input.substring(start, position)
        );
    }

    private boolean peek(char expected) {
        return position + 1 < input.length()
                && input.charAt(position + 1) == expected;
    }
}