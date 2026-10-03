package vectoria.modid.plot;

import vectoria.modid.math.evaluation.Evaluator;
import vectoria.modid.math.expression.BinaryExpression;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.expression.FunctionExpression;
import vectoria.modid.math.expression.UnaryExpression;
import vectoria.modid.math.expression.VariableExpression;
import vectoria.modid.math.lexer.Lexer;
import vectoria.modid.math.parser.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class PlotFunction {

    public enum Mode {
        PLANE, SPACE
    }

    public enum Kind {
        EXPLICIT, IMPLICIT, SURFACE
    }

    private static final Set<String> RESERVED_PLANE = Set.of("x", "y");
    private static final Set<String> RESERVED_SPACE = Set.of("x", "y", "z");

    private final Kind kind;
    private final Expression expression;
    private final List<String> parameters;
    private final Evaluator evaluator = new Evaluator();
    private final Map<String, Double> scope = new HashMap<>();

    private PlotFunction(Kind kind, Expression expression, List<String> parameters) {
        this.kind = kind;
        this.expression = expression;
        this.parameters = parameters;
    }

    public Kind kind() {
        return kind;
    }

    public List<String> parameters() {
        return parameters;
    }

    public void bind(Map<String, Double> values) {
        scope.clear();
        scope.putAll(values);
    }

    public double value(double x, double y) {
        scope.put("x", x);
        scope.put("y", y);
        try {
            double result = evaluator.evaluate(expression, scope);
            return Double.isFinite(result) ? result : Double.NaN;
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    public static PlotFunction compile(String text, Mode mode) {
        String source = text == null ? "" : text.trim();
        if (source.isEmpty()) throw new IllegalArgumentException("Empty function");

        String left = source;
        String right = null;
        int eq = topLevelEquals(source, 0);
        if (eq >= 0) {
            if (topLevelEquals(source, eq + 1) >= 0) throw new IllegalArgumentException("Only one '=' is allowed");
            left = source.substring(0, eq).trim();
            right = source.substring(eq + 1).trim();
            if (left.isEmpty() || right.isEmpty()) throw new IllegalArgumentException("Both sides of '=' are required");
        }

        return mode == Mode.PLANE ? compilePlane(left, right) : compileSpace(left, right);
    }

    private static int topLevelEquals(String text, int from) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '(' || c == '[') depth++;
            else if (c == '}' || c == ')' || c == ']') depth--;
            else if (c == '=' && depth == 0 && i >= from) return i;
        }
        return -1;
    }

    private static PlotFunction compilePlane(String left, String right) {
        if (right == null) {
            Expression expression = parse(left);
            Kind kind = freeVariables(expression).contains("y") ? Kind.IMPLICIT : Kind.EXPLICIT;
            return build(kind, expression, RESERVED_PLANE);
        }

        if (isName(left, "y") || isName(right, "y")) {
            String other = isName(left, "y") ? right : left;
            Expression expression = parse(other);
            if (!freeVariables(expression).contains("y")) {
                return build(Kind.EXPLICIT, expression, RESERVED_PLANE);
            }
        }

        return build(Kind.IMPLICIT, parse("(" + left + ")-(" + right + ")"), RESERVED_PLANE);
    }

    private static PlotFunction compileSpace(String left, String right) {
        Expression expression;
        if (right == null) {
            expression = parse(left);
        } else if (isName(left, "z")) {
            expression = parse(right);
        } else if (isName(right, "z")) {
            expression = parse(left);
        } else {
            throw new IllegalArgumentException("3D supports only z = f(x, y)");
        }

        if (freeVariables(expression).contains("z")) {
            throw new IllegalArgumentException("Use z only alone on one side: z = f(x, y)");
        }

        return build(Kind.SURFACE, expression, RESERVED_SPACE);
    }

    private static PlotFunction build(Kind kind, Expression expression, Set<String> reserved) {
        TreeSet<String> names = new TreeSet<>(freeVariables(expression));
        names.removeAll(reserved);
        names.remove("e");
        return new PlotFunction(kind, expression, Collections.unmodifiableList(new ArrayList<>(names)));
    }

    private static boolean isName(String text, String name) {
        return text.replace(" ", "").equals(name);
    }

    private static Expression parse(String source) {
        try {
            return new Parser(new Lexer(source, true).tokenize()).parse();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Incomplete expression");
        }
    }

    public static Set<String> freeVariables(Expression expression) {
        Set<String> result = new TreeSet<>();
        collect(expression, Set.of(), result);
        return result;
    }

    private static void collect(Expression expression, Set<String> bound, Set<String> out) {
        if (expression instanceof VariableExpression variable) {
            if (!bound.contains(variable.name())) out.add(variable.name());
        } else if (expression instanceof BinaryExpression binary) {
            collect(binary.left(), bound, out);
            collect(binary.right(), bound, out);
        } else if (expression instanceof UnaryExpression unary) {
            collect(unary.operand(), bound, out);
        } else if (expression instanceof FunctionExpression function) {
            collectFunction(function, bound, out);
        }
    }

    private static void collectFunction(FunctionExpression function, Set<String> bound, Set<String> out) {
        List<Expression> args = function.arguments();
        String name = function.name();

        boolean binds = ((name.equals("sum") || name.equals("int")) && args.size() == 4)
                || (name.equals("dv") && args.size() == 3);

        if (!binds) {
            for (Expression argument : args) collect(argument, bound, out);
            return;
        }

        Set<String> inner = new HashSet<>(bound);
        inner.add(((VariableExpression) args.get(1)).name());
        collect(args.get(0), inner, out);
        for (int i = 2; i < args.size(); i++) collect(args.get(i), bound, out);
    }
}