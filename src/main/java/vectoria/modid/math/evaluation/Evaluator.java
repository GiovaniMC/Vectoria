package vectoria.modid.math.evaluation;

import vectoria.modid.math.expression.BinaryExpression;
import vectoria.modid.math.expression.ConstantExpression;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.expression.FunctionExpression;
import vectoria.modid.math.expression.NumberExpression;
import vectoria.modid.math.expression.UnaryExpression;
import vectoria.modid.math.expression.VariableExpression;

import java.util.Map;

public class Evaluator {

    public double evaluate(
            Expression expression,
            Map<String, Double> variables
    ) {
        if (expression instanceof NumberExpression number) {
            return number.value();
        }

        if (expression instanceof VariableExpression variable) {
            Double value = variables.get(variable.name());

            if (value == null) {
                throw new IllegalArgumentException(
                        "Variable '" + variable.name() + "' has no value."
                );
            }

            return value;
        }

        if (expression instanceof ConstantExpression constant) {
            return evaluateConstant(constant);
        }

        if (expression instanceof UnaryExpression unary) {
            return evaluateUnary(unary, variables);
        }

        if (expression instanceof BinaryExpression binary) {
            return evaluateBinary(binary, variables);
        }

        if (expression instanceof FunctionExpression function) {
            return evaluateFunction(function, variables);
        }

        throw new IllegalArgumentException(
                "Unknown expression: " + expression
        );
    }

    private double evaluateConstant(ConstantExpression constant) {
        return switch (constant.name()) {
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            case "phi" -> (1.0 + Math.sqrt(5.0)) / 2.0;
            case "tau" -> 2.0 * Math.PI;

            case "infty" ->
                    throw new IllegalArgumentException(
                            "\\infty cannot be evaluated as a finite number."
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unknown constant: \\" + constant.name()
                    );
        };
    }

    private double evaluateUnary(
            UnaryExpression expression,
            Map<String, Double> variables
    ) {
        double value = evaluate(expression.operand(), variables);

        return switch (expression.operator()) {
            case NEGATE -> -value;
            case PERCENT -> value / 100.0;
            case FACTORIAL -> factorial(value);
        };
    }

    private double evaluateBinary(
            BinaryExpression expression,
            Map<String, Double> variables
    ) {
        double left = evaluate(expression.left(), variables);
        double right = evaluate(expression.right(), variables);

        return switch (expression.operator()) {
            case ADD -> left + right;
            case SUBTRACT -> left - right;
            case MULTIPLY -> left * right;
            case DIVIDE -> left / right;
            case POWER -> Math.pow(left, right);
        };
    }

    private double evaluateFunction(
            FunctionExpression expression,
            Map<String, Double> variables
    ) {
        String name = expression.name();

        double[] arguments = expression.arguments()
                .stream()
                .mapToDouble(argument ->
                        evaluate(argument, variables))
                .toArray();

        return switch (name) {
            case "sin" -> unary(name, arguments, Math::sin);
            case "cos" -> unary(name, arguments, Math::cos);
            case "tan" -> unary(name, arguments, Math::tan);

            case "cot" -> 1.0 / unary(name, arguments, Math::tan);
            case "sec" -> 1.0 / unary(name, arguments, Math::cos);
            case "csc" -> 1.0 / unary(name, arguments, Math::sin);

            case "asin", "arcsin" ->
                    unary(name, arguments, Math::asin);

            case "acos", "arccos" ->
                    unary(name, arguments, Math::acos);

            case "atan", "arctan" ->
                    unary(name, arguments, Math::atan);

            case "sinh" ->
                    unary(name, arguments, Math::sinh);

            case "cosh" ->
                    unary(name, arguments, Math::cosh);

            case "tanh" ->
                    unary(name, arguments, Math::tanh);

            case "sqrt" ->
                    unary(name, arguments, Math::sqrt);

            case "abs" ->
                    unary(name, arguments, Math::abs);

            case "exp" ->
                    unary(name, arguments, Math::exp);

            case "ln" ->
                    unary(name, arguments, Math::log);

            case "log" ->
                    evaluateLog(arguments);

            case "root" ->
                    evaluateRoot(arguments);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown function: \\" + name
                    );
        };
    }

    private double evaluateLog(double[] arguments) {
        if (arguments.length == 1) {
            return Math.log10(arguments[0]);
        }

        if (arguments.length == 2) {
            return Math.log(arguments[0])
                    / Math.log(arguments[1]);
        }

        throw new IllegalArgumentException(
                "\\log expects one argument or a base and argument."
        );
    }

    private double evaluateRoot(double[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException(
                    "Root expects a value and an index."
            );
        }

        return Math.pow(
                arguments[0],
                1.0 / arguments[1]
        );
    }

    private double unary(
            String name,
            double[] arguments,
            java.util.function.DoubleUnaryOperator function
    ) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException(
                    "\\" + name + " expects one argument."
            );
        }

        return function.applyAsDouble(arguments[0]);
    }

    private double factorial(double value) {
        if (value < 0 || value != Math.floor(value)) {
            throw new IllegalArgumentException(
                    "Factorial requires a non-negative integer."
            );
        }

        double result = 1.0;

        for (int i = 2; i <= (int) value; i++) {
            result *= i;
        }

        return result;
    }
}