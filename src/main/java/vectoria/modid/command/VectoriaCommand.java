package vectoria.modid.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import vectoria.modid.math.evaluation.Evaluator;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.lexer.Lexer;
import vectoria.modid.math.parser.Parser;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class VectoriaCommand {

    private static final String GREEN = "\u00a7a";
    private static final String YELLOW = "\u00a7e";
    private static final String RED = "\u00a7c";
    private static final String WHITE = "\u00a7f";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("vectoria")
                        .then(argument("expression", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String input = StringArgumentType.getString(context, "expression");
                                    return handleVectoria(context.getSource(), input);
                                })
                        )
        );

        dispatcher.register(
                literal("vectoriaev")
                        .then(argument("expression", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String input = StringArgumentType.getString(context, "expression");
                                    return handleVectoriaEv(context.getSource(), input);
                                })
                        )
        );
    }

    private static int handleVectoria(ServerCommandSource source, String input) {
        try {
            String text = input.trim();
            List<String> groups = parseBracedGroups(text);

            String message;
            if (groups != null && groups.size() == 2) {
                message = solveEquation(groups.get(0).trim(), groups.get(1).trim());
            } else if (groups != null && groups.size() == 1) {
                message = evaluateOrCompare(groups.get(0), Map.of());
            } else {
                message = evaluateOrCompare(text, Map.of());
            }

            final String out = message;
            source.sendFeedback(() -> Text.literal(GREEN + "Result: " + WHITE + out), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal(RED + "Error: " + describe(e)));
            return 0;
        }
    }

    private static int handleVectoriaEv(ServerCommandSource source, String input) {
        try {
            String text = input.trim();
            List<String> groups = parseBracedGroups(text);

            String message;
            if (groups != null && groups.size() == 2) {
                Map<String, Double> vars = parseAssignments(groups.get(1));
                message = evaluateOrCompare(groups.get(0), vars) + "  (" + describeAssignments(vars) + ")";
            } else if (groups != null && groups.size() == 1) {
                message = evaluateOrCompare(groups.get(0), Map.of());
            } else {
                message = evaluateOrCompare(text, Map.of());
            }

            final String out = message;
            source.sendFeedback(() -> Text.literal(YELLOW + "Evaluated Result: " + WHITE + out), false);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal(RED + "Error: " + describe(e)));
            return 0;
        }
    }

    private static String evaluateOrCompare(String text, Map<String, Double> vars) {
        int eq = topLevelEquals(text);
        if (eq < 0) return formatNumber(evaluateText(text, vars));
        if (topLevelEquals(text, eq + 1) >= 0) throw new IllegalArgumentException("Only one '=' is allowed");

        double left = evaluateText(text.substring(0, eq), vars);
        double right = evaluateText(text.substring(eq + 1), vars);
        boolean equal = Math.abs(left - right) <= 1e-9 * Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)));

        return formatNumber(left) + " = " + formatNumber(right) + "  ->  " + (equal ? "true" : "false");
    }

    private static int topLevelEquals(String s) {
        return topLevelEquals(s, 0);
    }

    private static int topLevelEquals(String s, int from) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '(' || c == '[') depth++;
            else if (c == '}' || c == ')' || c == ']') depth--;
            else if (c == '=' && depth == 0 && i >= from) return i;
        }
        return -1;
    }

    private static double evaluateText(String text, Map<String, Double> vars) {
        Lexer lexer = new Lexer(text, true);
        Parser parser = new Parser(lexer.tokenize());
        Expression expr = parser.parse();
        return new Evaluator().evaluate(expr, vars);
    }

    private static List<String> parseBracedGroups(String input) {
        List<String> groups = new ArrayList<>();
        int n = input.length();
        int i = 0;
        while (i < n) {
            while (i < n && Character.isWhitespace(input.charAt(i))) i++;
            if (i >= n) break;
            if (input.charAt(i) != '{') return null;

            int depth = 0;
            int j = i;
            for (; j < n; j++) {
                char c = input.charAt(j);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            if (j >= n) throw new IllegalArgumentException("Unbalanced braces '{' '}'");

            groups.add(input.substring(i + 1, j));
            i = j + 1;
        }
        return groups.isEmpty() ? null : groups;
    }

    private static Map<String, Double> parseAssignments(String spec) {
        List<String> parts = splitTopLevel(spec);
        Map<String, Double> vars = new LinkedHashMap<>();

        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;

            int eq = p.indexOf('=');
            String name;
            String valueText;
            if (eq < 0) {
                if (parts.size() > 1) {
                    throw new IllegalArgumentException("Use name=value when giving several variables (e.g. x=3, y=2)");
                }
                name = "x";
                valueText = p;
            } else {
                name = p.substring(0, eq).trim();
                valueText = p.substring(eq + 1).trim();
            }

            if (!name.matches("[A-Za-z]")) {
                throw new IllegalArgumentException("Variable names must be a single letter: " + name);
            }
            vars.put(name, evaluateText(valueText, Map.of()));
        }

        if (vars.isEmpty()) throw new IllegalArgumentException("Provide the variable value. Example: {x^2+5}{3}");
        return vars;
    }

    private static List<String> splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '(' || c == '[') depth++;
            else if (c == '}' || c == ')' || c == ']') depth--;

            if ((c == ',' || c == ';') && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts;
    }

    private static String describeAssignments(Map<String, Double> vars) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : vars.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append("=").append(formatNumber(e.getValue()));
        }
        return sb.toString();
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static String solveEquation(String varName, String exprStr) {
        if (!varName.matches("[A-Za-z]")) {
            throw new IllegalArgumentException("Variable names must be a single letter: " + varName);
        }

        int eq = topLevelEquals(exprStr);
        if (eq >= 0) {
            exprStr = "(" + exprStr.substring(0, eq) + ")-(" + exprStr.substring(eq + 1) + ")";
        }

        Lexer lexer = new Lexer(exprStr, true);
        Parser parser = new Parser(lexer.tokenize());
        Expression expr = parser.parse();
        Evaluator evaluator = new Evaluator();

        try {
            double f0 = evalSafely(evaluator, expr, varName, 0.0);
            double f1 = evalSafely(evaluator, expr, varName, 1.0);
            double fn1 = evalSafely(evaluator, expr, varName, -1.0);
            double f2 = evalSafely(evaluator, expr, varName, 2.0);

            if (!Double.isNaN(f0) && !Double.isNaN(f1) && !Double.isNaN(fn1) && !Double.isNaN(f2)) {
                double c = f0;
                double a = (f1 + fn1 - 2 * f0) / 2.0;
                double b = (f1 - fn1) / 2.0;

                double expectedF2 = a * 4 + b * 2 + c;
                if (Math.abs(expectedF2 - f2) < 1e-4 && (Math.abs(a) > 1e-5 || Math.abs(b) > 1e-5)) {
                    if (Math.abs(a) < 1e-5) {
                        double root = -c / b;
                        return "x1=" + formatNumber(root);
                    }

                    double delta = (b * b) - (4.0 * a * c);

                    if (delta < -1e-5) {
                        return "the solution is not real";
                    }

                    if (Math.abs(delta) < 1e-5) {
                        double root = -b / (2.0 * a);
                        return "x1=" + formatNumber(root);
                    }

                    double sqrtDelta = Math.sqrt(delta);
                    double r1 = (-b - sqrtDelta) / (2.0 * a);
                    double r2 = (-b + sqrtDelta) / (2.0 * a);

                    if (r1 > r2) { double temp = r1; r1 = r2; r2 = temp; }

                    if (Math.abs(r1 + r2) < 1e-4 && Math.abs(r1) > 1e-4) {
                        double val = Math.abs(r2);
                        if (Math.abs(val * val - 2.0) < 1e-2) return "\\pm \\sqrt{2}";
                        if (Math.abs(val * val - 3.0) < 1e-2) return "\\pm \\sqrt{3}";
                        if (Math.abs(val * val - 5.0) < 1e-2) return "\\pm \\sqrt{5}";
                        if (Math.abs(val - Math.rint(val)) < 1e-4) return "\\pm " + formatNumber(val);
                        return String.format("\\pm %s (x1=%s, x2=%s)", formatNumber(val), formatNumber(r1), formatNumber(r2));
                    }

                    return String.format("x1=%s, x2=%s", formatNumber(r1), formatNumber(r2));
                }
            }
        } catch (Exception ignored) {}

        List<Double> roots = new ArrayList<>();
        double start = -20.0;
        double end = 20.0;
        double step = 0.1;

        Double prevX = null;
        Double prevVal = null;

        for (double x = start; x <= end; x += step) {
            double currVal = evalSafely(evaluator, expr, varName, x);
            if (!Double.isNaN(currVal)) {
                if (prevVal != null && prevX != null) {
                    if (prevVal * currVal <= 0.0 || Math.abs(currVal) < 1e-4) {
                        double root = refineRoot(expr, varName, (prevX + x) / 2.0, evaluator);
                        if (!Double.isNaN(root)) {
                            addUniqueRoot(roots, root);
                        }
                    }
                } else if (Math.abs(currVal) < 1e-4) {
                    double root = refineRoot(expr, varName, x, evaluator);
                    if (!Double.isNaN(root)) {
                        addUniqueRoot(roots, root);
                    }
                }
                prevX = x;
                prevVal = currVal;
            } else {
                prevX = null;
                prevVal = null;
            }
        }

        if (roots.isEmpty()) {
            return "the solution is not real";
        }

        roots.sort(Double::compareTo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roots.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("x").append(i + 1).append("=").append(formatNumber(roots.get(i)));
        }
        return sb.toString();
    }

    private static double evalSafely(Evaluator evaluator, Expression expr, String varName, double val) {
        try {
            Map<String, Double> vars = Map.of(varName, val);
            return evaluator.evaluate(expr, vars);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static void addUniqueRoot(List<Double> roots, double root) {
        for (double r : roots) {
            if (Math.abs(r - root) < 1e-3) return;
        }
        roots.add(root);
    }

    private static double refineRoot(Expression expr, String varName, double initialGuess, Evaluator evaluator) {
        double x = initialGuess;
        double h = 1e-5;
        for (int i = 0; i < 40; i++) {
            double fx = evalSafely(evaluator, expr, varName, x);
            if (Double.isNaN(fx)) return Double.NaN;
            if (Math.abs(fx) < 1e-7) return x;

            double fxh1 = evalSafely(evaluator, expr, varName, x + h);
            double fxh2 = evalSafely(evaluator, expr, varName, x - h);
            if (Double.isNaN(fxh1) || Double.isNaN(fxh2)) return Double.NaN;

            double fprime = (fxh1 - fxh2) / (2 * h);
            if (Math.abs(fprime) < 1e-12) break;

            double nextX = x - fx / fprime;
            if (Math.abs(nextX - x) < 1e-7) return nextX;
            x = nextX;
        }
        double finalFx = evalSafely(evaluator, expr, varName, x);
        if (!Double.isNaN(finalFx) && Math.abs(finalFx) < 1e-4) return x;
        return Double.NaN;
    }

    private static String formatNumber(double val) {
        if (Double.isNaN(val)) return "NaN";
        if (Double.isInfinite(val)) return val > 0 ? "Infinity" : "-Infinity";

        double abs = Math.abs(val);
        if (abs >= 1e12 || (abs < 1e-6 && abs != 0.0)) {
            return String.format(Locale.ROOT, "%.6e", val);
        }

        String s = String.format(Locale.ROOT, "%.6f", val);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }
}