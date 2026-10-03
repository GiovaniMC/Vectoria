package vectoria.modid.math.parser;

import vectoria.modid.math.expression.BinaryExpression;
import vectoria.modid.math.expression.ConstantExpression;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.expression.FunctionExpression;
import vectoria.modid.math.expression.NumberExpression;
import vectoria.modid.math.expression.UnaryExpression;
import vectoria.modid.math.expression.VariableExpression;
import vectoria.modid.math.lexer.Token;
import vectoria.modid.math.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int position;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Expression parse() {
        Expression expression = parseAddition();

        if (position < tokens.size()) {
            throw error("Unexpected token: " + current());
        }

        return expression;
    }

    private Expression parseAddition() {
        Expression expression = parseMultiplication();

        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previous();
            Expression right = parseMultiplication();

            expression = new BinaryExpression(
                    expression,
                    operator.type() == TokenType.PLUS
                            ? BinaryExpression.Operator.ADD
                            : BinaryExpression.Operator.SUBTRACT,
                    right
            );
        }

        return expression;
    }

    private Expression parseMultiplication() {
        Expression expression = parsePower();

        while (true) {
            if (match(TokenType.MULTIPLY)) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.MULTIPLY,
                        parsePower()
                );
            } else if (match(TokenType.DIVIDE)) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.DIVIDE,
                        parsePower()
                );
            } else if (startsImplicitMultiplication()) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.MULTIPLY,
                        parsePower()
                );
            } else {
                break;
            }
        }

        return expression;
    }

    private Expression parsePower() {
        Expression expression = parseUnary();

        if (match(TokenType.POWER)) {
            Expression exponent = parsePower();

            expression = new BinaryExpression(
                    expression,
                    BinaryExpression.Operator.POWER,
                    exponent
            );
        }

        return expression;
    }

    private Expression parseUnary() {
        if (match(TokenType.MINUS)) {
            return new UnaryExpression(
                    UnaryExpression.Operator.NEGATE,
                    parseUnary()
            );
        }

        if (match(TokenType.PLUS)) {
            return parseUnary();
        }

        return parsePostfix();
    }

    private Expression parsePostfix() {
        Expression expression = parsePrimary();

        while (true) {
            if (match(TokenType.FACTORIAL)) {
                expression = new UnaryExpression(
                        UnaryExpression.Operator.FACTORIAL,
                        expression
                );
            } else if (match(TokenType.MODULO)) {
                expression = new UnaryExpression(
                        UnaryExpression.Operator.PERCENT,
                        expression
                );
            } else {
                break;
            }
        }

        return expression;
    }

    private Expression parsePrimary() {
        if (match(TokenType.NUMBER)) {
            return new NumberExpression(
                    Double.parseDouble(previous().value())
            );
        }

        if (match(TokenType.IDENTIFIER)) {
            return new VariableExpression(previous().value());
        }

        if (match(TokenType.COMMAND)) {
            return parseCommand(previous().value());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_PAREN, "Expected ')'.");

            return expression;
        }

        if (match(TokenType.LEFT_BRACE)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_BRACE, "Expected '}'.");

            return expression;
        }

        if (match(TokenType.LEFT_BRACKET)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_BRACKET, "Expected ']'.");

            return expression;
        }

        throw error("Expected expression.");
    }

    private Expression parseCommand(String command) {
        return switch (command) {
            case "pi", "e", "phi", "tau", "infty" ->
                    new ConstantExpression(command);

            case "frac" -> parseFraction();

            case "sqrt" -> parseSqrt();

            case "left" -> parseLeft();

            case "right" ->
                    throw error("\\right cannot appear without \\left.");

            default -> parseFunction(command);
        };
    }

    private Expression parseFraction() {
        Expression numerator = parseRequiredGroup(
                "Expected numerator after \\frac."
        );

        Expression denominator = parseRequiredGroup(
                "Expected denominator after \\frac."
        );

        return new BinaryExpression(
                numerator,
                BinaryExpression.Operator.DIVIDE,
                denominator
        );
    }

    private Expression parseSqrt() {
        Expression index = null;

        if (match(TokenType.LEFT_BRACKET)) {
            index = parseAddition();

            consume(
                    TokenType.RIGHT_BRACKET,
                    "Expected ']' after square root index."
            );
        }

        Expression argument = parseRequiredGroupOrPrimary(
                "Expected argument after \\sqrt."
        );

        if (index == null) {
            return new FunctionExpression(
                    "sqrt",
                    List.of(argument)
            );
        }

        return new FunctionExpression(
                "root",
                List.of(argument, index)
        );
    }

    private Expression parseLeft() {
        if (position >= tokens.size()) {
            throw error("Expected delimiter after \\left.");
        }

        TokenType opening = current().type();

        if (!isOpeningDelimiter(opening)) {
            throw error("Invalid delimiter after \\left.");
        }

        position++;

        Expression expression = parseAddition();

        if (!match(TokenType.COMMAND)
                || !previous().value().equals("right")) {
            throw error("Expected \\right.");
        }

        if (position >= tokens.size()) {
            throw error("Expected closing delimiter after \\right.");
        }

        TokenType closing = current().type();

        if (!isClosingDelimiter(closing)) {
            throw error("Invalid delimiter after \\right.");
        }

        position++;

        return expression;
    }

    private Expression parseFunction(String name) {
        List<Expression> arguments = new ArrayList<>();

        Expression argument;

        if (check(TokenType.LEFT_PAREN)
                || check(TokenType.LEFT_BRACE)) {
            argument = parseDelimitedArgument();
        } else {
            argument = parsePower();
        }

        arguments.add(argument);

        if (match(TokenType.COMMA)) {
            while (true) {
                arguments.add(parseAddition());

                if (!match(TokenType.COMMA)) {
                    break;
                }
            }
        }

        return new FunctionExpression(name, arguments);
    }

    private Expression parseDelimitedArgument() {
        if (match(TokenType.LEFT_PAREN)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_PAREN, "Expected ')'.");
            return expression;
        }

        if (match(TokenType.LEFT_BRACE)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_BRACE, "Expected '}'.");
            return expression;
        }

        throw error("Expected function argument.");
    }

    private Expression parseRequiredGroup(String message) {
        if (match(TokenType.LEFT_BRACE)) {
            Expression expression = parseAddition();
            consume(TokenType.RIGHT_BRACE, "Expected '}'.");
            return expression;
        }

        throw error(message);
    }

    private Expression parseRequiredGroupOrPrimary(String message) {
        if (position >= tokens.size()) {
            throw error(message);
        }

        if (check(TokenType.LEFT_BRACE)
                || check(TokenType.LEFT_PAREN)
                || check(TokenType.LEFT_BRACKET)) {
            return parsePrimary();
        }

        return parsePrimary();
    }

    private boolean startsImplicitMultiplication() {
        if (position >= tokens.size()) {
            return false;
        }

        return switch (current().type()) {
            case NUMBER,
                 IDENTIFIER,
                 COMMAND,
                 LEFT_PAREN,
                 LEFT_BRACE,
                 LEFT_BRACKET -> true;

            default -> false;
        };
    }

    private boolean isOpeningDelimiter(TokenType type) {
        return type == TokenType.LEFT_PAREN
                || type == TokenType.LEFT_BRACKET
                || type == TokenType.LEFT_BRACE;
    }

    private boolean isClosingDelimiter(TokenType type) {
        return type == TokenType.RIGHT_PAREN
                || type == TokenType.RIGHT_BRACKET
                || type == TokenType.RIGHT_BRACE;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            position++;
            return true;
        }

        return false;
    }

    private void consume(TokenType type, String message) {
        if (!match(type)) {
            throw error(message);
        }
    }

    private boolean check(TokenType type) {
        return position < tokens.size()
                && current().type() == type;
    }

    private Token current() {
        return tokens.get(position);
    }

    private Token previous() {
        return tokens.get(position - 1);
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message);
    }
}