package vectoria.modid.math.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Lexer {
    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "cot", "sec", "csc",
            "asin", "acos", "atan", "arcsin", "arccos", "arctan",
            "sinh", "cosh", "tanh",
            "ln", "log", "exp", "abs", "sqrt"
    );

    private static final Set<String> OPERATORS = Set.of("int", "sum", "dv");

    private final String input;
    private final boolean singleLetterVariables;
    private int position = 0;

    public Lexer(String input) {
        this(input, false);
    }

    public Lexer(String input, boolean singleLetterVariables) {
        this.input = input;
        this.singleLetterVariables = singleLetterVariables;
    }

    public List<Token> tokenize() {
        position = 0;
        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
            } else if (Character.isDigit(current) || current == '.') {
                tokens.add(new Token(TokenType.NUMBER, readNumber()));
            } else if (Character.isLetter(current)) {
                readWord(tokens);
            } else if (current == '\\') {
                Token command = readCommand();
                if (command != null) tokens.add(command);
            } else if (current == '{' && skipBracedOperator(tokens)) {
                continue;
            } else {
                tokens.add(readSymbol(current));
                position++;
            }
        }

        return tokens;
    }

    private boolean skipBracedOperator(List<Token> tokens) {
        int i = position + 1;
        while (i < input.length() && Character.isWhitespace(input.charAt(i))) i++;
        if (i >= input.length() || input.charAt(i) != '\\') return false;
        i++;

        int start = i;
        while (i < input.length() && Character.isLetter(input.charAt(i))) i++;
        String name = input.substring(start, i);

        while (i < input.length() && Character.isWhitespace(input.charAt(i))) i++;
        if (i >= input.length() || input.charAt(i) != '}') return false;
        if (!OPERATORS.contains(name)) return false;

        tokens.add(new Token(TokenType.COMMAND, name));
        position = i + 1;
        return true;
    }

    private Token readSymbol(char c) {
        return switch (c) {
            case '+' -> new Token(TokenType.PLUS, "+");
            case '-' -> new Token(TokenType.MINUS, "-");
            case '*', '\u00B7', '\u00D7' -> new Token(TokenType.MULTIPLY, "*");
            case '/', '\u00F7' -> new Token(TokenType.DIVIDE, "/");
            case '^' -> new Token(TokenType.POWER, "^");
            case '_' -> new Token(TokenType.UNDERSCORE, "_");
            case '(' -> new Token(TokenType.LPAREN, "(");
            case ')' -> new Token(TokenType.RPAREN, ")");
            case '[' -> new Token(TokenType.LBRACKET, "[");
            case ']' -> new Token(TokenType.RBRACKET, "]");
            case '{' -> new Token(TokenType.LBRACE, "{");
            case '}' -> new Token(TokenType.RBRACE, "}");
            case '!' -> new Token(TokenType.FACTORIAL, "!");
            case '%' -> new Token(TokenType.MODULO, "%");
            case ',' -> new Token(TokenType.COMMA, ",");
            case '=' -> new Token(TokenType.EQUAL, "=");
            case ';' -> new Token(TokenType.SEMICOLON, ";");
            default -> throw error("Unexpected character: " + c);
        };
    }

    private void readWord(List<Token> tokens) {
        String run = readIdentifier();

        if (!singleLetterVariables) {
            tokens.add(wordToken(run));
            return;
        }

        int i = 0;
        while (i < run.length()) {
            String known = longestKnownWord(run, i);
            if (known != null) {
                tokens.add(wordToken(known));
                i += known.length();
            } else {
                tokens.add(new Token(TokenType.IDENTIFIER, String.valueOf(run.charAt(i))));
                i++;
            }
        }
    }

    private String longestKnownWord(String run, int from) {
        String best = run.startsWith("pi", from) ? "pi" : null;
        for (String function : FUNCTIONS) {
            if (run.startsWith(function, from) && (best == null || function.length() > best.length())) {
                best = function;
            }
        }
        return best;
    }

    private Token wordToken(String word) {
        if (word.equals("pi") || word.equals("\u03C0")) return new Token(TokenType.COMMAND, "pi");
        if (FUNCTIONS.contains(word)) return new Token(TokenType.COMMAND, word);
        return new Token(TokenType.IDENTIFIER, word);
    }

    private Token readCommand() {
        position++;
        if (position >= input.length()) throw error("Trailing '\\' at end of expression");

        char c = input.charAt(position);
        if (!Character.isLetter(c)) {
            position++;
            if (c == ',' || c == ';' || c == ':' || c == '!' || c == ' ') return null;
            throw error("Invalid LaTeX command: \\" + c);
        }

        String name = readIdentifier();
        return switch (name) {
            case "cdot", "times", "ast" -> new Token(TokenType.MULTIPLY, "*");
            case "div" -> new Token(TokenType.DIVIDE, "/");
            default -> new Token(TokenType.COMMAND, name);
        };
    }

    private String readNumber() {
        StringBuilder sb = new StringBuilder();
        boolean hasDecimal = false;
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isDigit(c)) {
                sb.append(c);
                position++;
            } else if (c == '.' && !hasDecimal) {
                hasDecimal = true;
                sb.append(c);
                position++;
            } else {
                break;
            }
        }
        if (sb.toString().equals(".")) throw error("Invalid number: '.'");
        return sb.toString();
    }

    private String readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (position < input.length() && Character.isLetter(input.charAt(position))) {
            sb.append(input.charAt(position));
            position++;
        }
        return sb.toString();
    }

    private RuntimeException error(String message) {
        return new IllegalArgumentException(message + " (position " + position + ")");
    }
}