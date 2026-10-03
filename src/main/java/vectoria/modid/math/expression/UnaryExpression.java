package vectoria.modid.math.expression;

public record UnaryExpression(Operator operator, Expression operand) implements Expression {

    public enum Operator {
        NEGATE, FACTORIAL, PERCENT
    }
}