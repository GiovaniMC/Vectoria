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
            throw error("Unexpected token: " + current().value());
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
        Expression expression = parseUnary();

        while (true) {
            if (match(TokenType.MULTIPLY)) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.MULTIPLY,
                        parseUnary()
                );
            } else if (match(TokenType.DIVIDE)) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.DIVIDE,
                        parseUnary()
                );
            } else if (startsImplicitMultiplication()) {
                expression = new BinaryExpression(
                        expression,
                        BinaryExpression.Operator.MULTIPLY,
                        parseUnary()
                );
            } else {
                break;
            }
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

        return parsePower();
    }

    private Expression parsePower() {
        Expression expression = parsePostfix();

        if (match(TokenType.POWER)) {
            Expression exponent = parseUnary();

            expression = new BinaryExpression(
                    expression,
                    BinaryExpression.Operator.POWER,
                    exponent
            );
        }

        return expression;
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
            return new NumberExpression(Double.parseDouble(previous().value()));
        }

        if (match(TokenType.IDENTIFIER)) {
            return new VariableExpression(previous().value());
        }

        if (match(TokenType.COMMAND)) {
            return parseCommand(previous().value());
        }

        if (match(TokenType.LPAREN)) {
            Expression expression = parseAddition();
            consume(TokenType.RPAREN, "Expected ')'.");
            return expression;
        }

        if (match(TokenType.LBRACE)) {
            Expression expression = parseAddition();
            consume(TokenType.RBRACE, "Expected '}'.");
            return expression;
        }

        if (match(TokenType.LBRACKET)) {
            Expression expression = parseAddition();
            consume(TokenType.RBRACKET, "Expected ']'.");
            return expression;
        }

        throw error("Expected expression.");
    }

    private Expression parseCommand(String command) {
        return switch (command) {
            case "pi", "e", "phi", "tau", "infty" -> new ConstantExpression(command);
            case "frac" -> parseFraction();
            case "sqrt" -> parseSqrt();
            case "left" -> parseLeft();
            case "right" -> throw error("\\right cannot appear without \\left.");
            case "sum" -> parseSum();
            case "int" -> parseInt();
            case "dv" -> parseDv();
            default -> parseFunction(command);
        };
    }

    private Expression parseFraction() {
        Expression numerator = parseRequiredGroup("Expected numerator after \\frac.");
        Expression denominator = parseRequiredGroup("Expected denominator after \\frac.");

        return new BinaryExpression(numerator, BinaryExpression.Operator.DIVIDE, denominator);
    }

    private Expression parseSqrt() {
        Expression index = null;

        if (match(TokenType.LBRACKET)) {
            index = parseAddition();
            consume(TokenType.RBRACKET, "Expected ']' after square root index.");
        }

        Expression argument = parseRequiredGroupOrPrimary("Expected argument after \\sqrt.");

        if (index == null) {
            return new FunctionExpression("sqrt", List.of(argument));
        }

        return new FunctionExpression("root", List.of(argument, index));
    }

    private Expression parseSum() {
        consume(TokenType.UNDERSCORE, "Expected '_' after \\sum.");
        consume(TokenType.LBRACE, "Expected '{' after '_'.");
        String varName = current().value();
        consume(TokenType.IDENTIFIER, "Expected variable name.");
        consume(TokenType.EQUAL, "Expected '='.");
        Expression lower = parseAddition();
        consume(TokenType.RBRACE, "Expected '}'.");

        consume(TokenType.POWER, "Expected '^' after \\sum lower bound.");
        Expression upper = parseBound("Expected upper bound after '^'.");
        Expression body = parseRequiredGroup("Expected '{' or '(' for \\sum body.");

        return new FunctionExpression("sum", List.of(body, new VariableExpression(varName), lower, upper));
    }

    private Expression parseInt() {
        consume(TokenType.UNDERSCORE, "Expected '_' after \\int.");
        Expression lower = parseBound("Expected lower bound after '_'.");

        consume(TokenType.POWER, "Expected '^' after \\int lower bound.");
        Expression upper = parseBound("Expected upper bound after '^'.");

        Expression body = parseRequiredGroup("Expected '{' or '(' for \\int body.");
        Expression varNode = parseRequiredGroup("Expected '{' or '(' for \\int variable.");

        if (!(varNode instanceof VariableExpression)) {
            throw error("Integration variable must be an identifier.");
        }

        return new FunctionExpression("int", List.of(body, varNode, lower, upper));
    }

    private Expression parseDv() {
        Expression body = parseRequiredGroup("Expected '{' or '(' for \\dv expression.");
        Expression varNode = parseRequiredGroup("Expected '{' or '(' for \\dv variable.");

        if (!(varNode instanceof VariableExpression)) {
            throw error("Differentiation variable must be an identifier.");
        }

        if (check(TokenType.LBRACE) || check(TokenType.LPAREN)) {
            Expression point = parseRequiredGroup("Expected '{' or '(' for \\dv point.");
            return new FunctionExpression("dv", List.of(body, varNode, point));
        }

        return new FunctionExpression("dv", List.of(body, varNode));
    }

    private Expression parseLeft() {
        if (position >= tokens.size()) throw error("Expected delimiter after \\left.");

        TokenType opening = current().type();
        if (!isOpeningDelimiter(opening)) throw error("Invalid delimiter after \\left.");
        position++;

        Expression expression = parseAddition();

        if (!match(TokenType.COMMAND) || !previous().value().equals("right")) {
            throw error("Expected \\right.");
        }
        if (position >= tokens.size()) throw error("Expected closing delimiter after \\right.");

        TokenType closing = current().type();
        if (!isClosingDelimiter(closing)) throw error("Invalid delimiter after \\right.");
        position++;

        return expression;
    }

    private Expression parseFunction(String name) {
        List<Expression> arguments = new ArrayList<>();

        if (match(TokenType.LPAREN) || match(TokenType.LBRACE)) {
            TokenType closing = previous().type() == TokenType.LPAREN ? TokenType.RPAREN : TokenType.RBRACE;

            if (!check(closing)) {
                arguments.add(parseAddition());
                while (match(TokenType.COMMA)) {
                    arguments.add(parseAddition());
                }
            }
            consume(closing, "Expected closing delimiter for function arguments.");
        } else {
            arguments.add(parsePower());
        }

        return new FunctionExpression(name, arguments);
    }

    private Expression parseRequiredGroup(String message) {
        if (match(TokenType.LBRACE)) {
            Expression expression = parseAddition();
            consume(TokenType.RBRACE, "Expected '}'.");
            return expression;
        }

        if (match(TokenType.LPAREN)) {
            Expression expression = parseAddition();
            consume(TokenType.RPAREN, "Expected ')'.");
            return expression;
        }

        throw error(message);
    }

    private Expression parseBound(String message) {
        if (check(TokenType.LBRACE) || check(TokenType.LPAREN)) {
            return parseRequiredGroup(message);
        }

        if (check(TokenType.NUMBER) || check(TokenType.IDENTIFIER) || check(TokenType.COMMAND)) {
            return parsePrimary();
        }

        throw error(message);
    }

    private Expression parseRequiredGroupOrPrimary(String message) {
        if (position >= tokens.size()) throw error(message);
        return parsePrimary();
    }

    private boolean startsImplicitMultiplication() {
        if (position >= tokens.size()) return false;
        if (current().type() == TokenType.COMMAND && current().value().equals("right")) return false;

        return switch (current().type()) {
            case NUMBER, IDENTIFIER, COMMAND, LPAREN, LBRACE, LBRACKET -> true;
            default -> false;
        };
    }

    private boolean isOpeningDelimiter(TokenType type) {
        return type == TokenType.LPAREN || type == TokenType.LBRACKET || type == TokenType.LBRACE;
    }

    private boolean isClosingDelimiter(TokenType type) {
        return type == TokenType.RPAREN || type == TokenType.RBRACKET || type == TokenType.RBRACE;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            position++;
            return true;
        }
        return false;
    }

    private void consume(TokenType type, String message) {
        if (!match(type)) throw error(message);
    }

    private boolean check(TokenType type) {
        return position < tokens.size() && current().type() == type;
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