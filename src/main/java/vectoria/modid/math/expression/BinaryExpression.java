package vectoria.modid.math.expression;

public record BinaryExpression(
        Expression left,
        Operator operator,
        Expression right
) implements Expression {

    public enum Operator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        POWER
    }
}