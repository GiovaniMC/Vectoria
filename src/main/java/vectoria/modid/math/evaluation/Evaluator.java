package vectoria.modid.math.evaluation;

import vectoria.modid.math.expression.BinaryExpression;
import vectoria.modid.math.expression.ConstantExpression;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.expression.FunctionExpression;
import vectoria.modid.math.expression.NumberExpression;
import vectoria.modid.math.expression.UnaryExpression;
import vectoria.modid.math.expression.VariableExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Evaluator {

    private static final Set<String> UNARY_FUNCTIONS = Set.of(
            "sin", "cos", "tan", "cot", "sec", "csc",
            "asin", "acos", "atan", "arcsin", "arccos", "arctan",
            "sinh", "cosh", "tanh",
            "ln", "exp", "sqrt", "abs"
    );

    public double evaluate(Expression expr, Map<String, Double> vars) {
        if (expr instanceof NumberExpression number) {
            return number.value();
        }
        if (expr instanceof VariableExpression variable) {
            Double value = vars.get(variable.name());
            if (value != null) return value;
            Double named = namedConstant(variable.name());
            if (named != null) return named;
            throw new RuntimeException("Undefined variable: " + variable.name());
        }
        if (expr instanceof ConstantExpression constantExpr) {
            Double named = namedConstant(constantExpr.name());
            if (named == null) throw new RuntimeException("Unknown constant: " + constantExpr.name());
            return named;
        }
        if (expr instanceof BinaryExpression binary) {
            double left = evaluate(binary.left(), vars);
            double right = evaluate(binary.right(), vars);
            return switch (binary.operator()) {
                case ADD -> left + right;
                case SUBTRACT -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE -> {
                    if (right == 0.0) throw new MathDomainException("Division by zero");
                    yield left / right;
                }
                case POWER -> Math.pow(left, right);
            };
        }
        if (expr instanceof UnaryExpression unary) {
            double value = evaluate(unary.operand(), vars);
            return switch (unary.operator()) {
                case NEGATE -> -value;
                case PERCENT -> value / 100.0;
                case FACTORIAL -> factorial(value);
            };
        }
        if (expr instanceof FunctionExpression function) {
            return evaluateFunction(function, vars);
        }

        throw new RuntimeException("Unknown expression type: " + (expr != null ? expr.getClass().getName() : "null"));
    }

    private static Double namedConstant(String name) {
        return switch (name) {
            case "pi", "PI", "\u03C0" -> Math.PI;
            case "e" -> Math.E;
            case "tau" -> 2 * Math.PI;
            case "phi" -> (1 + Math.sqrt(5)) / 2;
            case "infty" -> Double.POSITIVE_INFINITY;
            default -> null;
        };
    }

    private static double factorial(double value) {
        if (value < 0 || value != Math.rint(value)) {
            throw new MathDomainException("factorial domain error: argument must be a non-negative integer");
        }
        if (value > 170) {
            throw new MathDomainException("factorial overflow: argument must be at most 170");
        }
        double result = 1;
        for (int i = 2; i <= (int) value; i++) result *= i;
        return result;
    }

    private double evaluateFunction(FunctionExpression function, Map<String, Double> vars) {
        String name = function.name();
        List<Expression> args = function.arguments();

        if (name.equals("sum")) return sum(args, vars);
        if (name.equals("int")) return integrate(args, vars);
        if (name.equals("dv")) return differentiate(args, vars);

        double[] values = new double[args.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = evaluate(args.get(i), vars);
        }
        return apply(name, values);
    }

    private double sum(List<Expression> args, Map<String, Double> vars) {
        String varName = variableName(args.get(1));
        long lower = Math.round(evaluate(args.get(2), vars));
        long upper = Math.round(evaluate(args.get(3), vars));
        if (upper - lower > 10_000_000L) throw new MathDomainException("sum range too large");

        Map<String, Double> scope = new HashMap<>(vars);
        double total = 0;
        for (long i = lower; i <= upper; i++) {
            scope.put(varName, (double) i);
            total += evaluate(args.get(0), scope);
        }
        return total;
    }

    private double integrate(List<Expression> args, Map<String, Double> vars) {
        String varName = variableName(args.get(1));
        double lower = evaluate(args.get(2), vars);
        double upper = evaluate(args.get(3), vars);
        int n = 1000;
        double h = (upper - lower) / n;

        Map<String, Double> scope = new HashMap<>(vars);
        scope.put(varName, lower);
        double sum = evaluate(args.get(0), scope);

        scope.put(varName, upper);
        sum += evaluate(args.get(0), scope);

        for (int i = 1; i < n; i++) {
            scope.put(varName, lower + i * h);
            sum += (i % 2 == 0 ? 2 : 4) * evaluate(args.get(0), scope);
        }
        return (h / 3.0) * sum;
    }

    private double differentiate(List<Expression> args, Map<String, Double> vars) {
        String varName = variableName(args.get(1));
        double x;
        if (args.size() > 2) {
            x = evaluate(args.get(2), vars);
        } else {
            Double at = vars.get(varName);
            if (at == null) throw new RuntimeException("Undefined variable: " + varName);
            x = at;
        }

        double h = 1e-5 * Math.max(1.0, Math.abs(x));
        Map<String, Double> forward = new HashMap<>(vars);
        Map<String, Double> backward = new HashMap<>(vars);
        forward.put(varName, x + h);
        backward.put(varName, x - h);
        return (evaluate(args.get(0), forward) - evaluate(args.get(0), backward)) / (2 * h);
    }

    private static String variableName(Expression expr) {
        if (expr instanceof VariableExpression variable) return variable.name();
        throw new IllegalArgumentException("Expected a variable name");
    }

    private static void requireArgs(String name, double[] values, int count) {
        if (values.length != count) {
            throw new MathDomainException(name + " expects " + count + " argument(s)");
        }
    }

    private double apply(String name, double[] values) {
        if (UNARY_FUNCTIONS.contains(name)) {
            requireArgs(name, values, 1);
            return applyUnary(name, values[0]);
        }

        if (name.equals("log")) {
            if (values.length == 1) {
                if (values[0] <= 0.0) {
                    throw new MathDomainException("log domain error: argument must be greater than 0");
                }
                return Math.log10(values[0]);
            }
            requireArgs(name, values, 2);
            if (values[0] <= 0.0 || values[1] <= 0.0 || values[1] == 1.0) {
                throw new MathDomainException("log domain error: argument must be > 0 and base must be > 0 and not 1");
            }
            return Math.log(values[0]) / Math.log(values[1]);
        }

        if (name.equals("root")) {
            requireArgs(name, values, 2);
            double value = values[0];
            double index = values[1];
            if (index == 0.0) throw new MathDomainException("root index cannot be 0");
            if (value < 0.0) {
                boolean oddInteger = index == Math.rint(index) && Math.abs(index % 2) == 1;
                if (!oddInteger) {
                    throw new MathDomainException("root domain error: negative argument needs an odd integer index");
                }
                return -Math.pow(-value, 1.0 / index);
            }
            return Math.pow(value, 1.0 / index);
        }

        throw new RuntimeException("Unknown function: " + name);
    }

    private double applyUnary(String name, double x) {
        return switch (name) {
            case "sin" -> Math.sin(x);
            case "cos" -> Math.cos(x);
            case "tan" -> Math.tan(x);
            case "cot" -> {
                double t = Math.tan(x);
                if (t == 0.0) throw new MathDomainException("cot domain error: tan(x) = 0");
                yield 1.0 / t;
            }
            case "sec" -> {
                double c = Math.cos(x);
                if (c == 0.0) throw new MathDomainException("sec domain error: cos(x) = 0");
                yield 1.0 / c;
            }
            case "csc" -> {
                double s = Math.sin(x);
                if (s == 0.0) throw new MathDomainException("csc domain error: sin(x) = 0");
                yield 1.0 / s;
            }
            case "asin", "arcsin" -> {
                if (x < -1.0 || x > 1.0) {
                    throw new MathDomainException("asin domain error: argument must be between -1 and 1");
                }
                yield Math.asin(x);
            }
            case "acos", "arccos" -> {
                if (x < -1.0 || x > 1.0) {
                    throw new MathDomainException("acos domain error: argument must be between -1 and 1");
                }
                yield Math.acos(x);
            }
            case "atan", "arctan" -> Math.atan(x);
            case "sinh" -> Math.sinh(x);
            case "cosh" -> Math.cosh(x);
            case "tanh" -> Math.tanh(x);
            case "exp" -> Math.exp(x);
            case "ln" -> {
                if (x <= 0.0) {
                    throw new MathDomainException("ln domain error: argument must be greater than 0");
                }
                yield Math.log(x);
            }
            case "sqrt" -> {
                if (x < 0.0) {
                    throw new MathDomainException("sqrt domain error: argument must be greater than or equal to 0");
                }
                yield Math.sqrt(x);
            }
            case "abs" -> Math.abs(x);
            default -> throw new RuntimeException("Unknown function: " + name);
        };
    }
}