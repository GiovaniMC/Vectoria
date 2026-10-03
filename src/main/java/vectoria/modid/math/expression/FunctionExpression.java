package vectoria.modid.math.expression;

import java.util.List;

public record FunctionExpression(
        String name,
        List<Expression> arguments
) implements Expression {
}