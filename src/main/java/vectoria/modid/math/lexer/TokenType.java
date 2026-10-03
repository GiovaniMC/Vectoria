package vectoria.modid.math.lexer;

public enum TokenType {
    NUMBER,
    IDENTIFIER,
    COMMAND,

    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    POWER,
    MODULO,
    FACTORIAL,

    UNDERSCORE,

    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    EQUAL,
    LESS,
    GREATER,
    LESS_EQUAL,
    GREATER_EQUAL,

    COMMA
}