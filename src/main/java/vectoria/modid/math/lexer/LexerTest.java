package vectoria.modid.math.lexer;

import vectoria.modid.math.evaluation.Evaluator;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.parser.Parser;

import java.util.Map;

public class LexerTest {

    public static void main(String[] args) {
        Map<String, Double> vars = Map.of(
                "a", 3.0,
                "b", 4.0,
                "x", 10.0,
                "y", 2.5
        );

        test("2 + 3 * 4", 14.0, vars);
        test("(2 + 3) * 4", 20.0, vars);
        test("10 - 2^3", 2.0, vars);
        test("10 / 2 * 5", 25.0, vars);

        test("-5 + 10", 5.0, vars);
        test("3!", 6.0, vars);
        test("50%", 0.5, vars);
        test("-(2 + 3)", -5.0, vars);

        test("2a", 6.0, vars);
        test("a b", 12.0, vars);
        test("2(3 + 4)", 14.0, vars);
        test("a(b)", 12.0, vars);
        test("2\\pi", 2 * Math.PI, vars);

        test("\\frac{10}{2}", 5.0, vars);
        test("\\frac{a + 1}{b}", 1.0, vars);
        test("\\sqrt{16}", 4.0, vars);
        test("\\sqrt[3]{8}", 2.0, vars);

        test("\\sin(\\pi / 2)", 1.0, vars);
        test("\\cos(0)", 1.0, vars);
        test("\\ln(\\e)", 1.0, vars);
        test("\\abs(-15.5)", 15.5, vars);

        test("1.5e2", 150.0, vars);
        test("2e-1", 0.2, vars);

        test("\\left( 2 + 3 \\right) * 4", 20.0, vars);
        test("\\left[ 10 - \\left( 2 * 3 \\right) \\right]", 4.0, vars);

        testError("2 +", vars);
        testError("\\frac{10}", vars);
        testError("z * 2", vars);
        testError("\\sqrt[2]", vars);
    }

    private static void test(String input, double expected, Map<String, Double> variables) {
        try {
            Lexer lexer = new Lexer(input);
            Parser parser = new Parser(lexer.tokenize());
            Expression expression = parser.parse();
            Evaluator evaluator = new Evaluator();

            double result = evaluator.evaluate(expression, variables);

            if (Math.abs(result - expected) < 1e-9) {
                System.out.println("[OK] " + input + " = " + result);
            } else {
                System.err.println("[FAIL] " + input + " | Expected: " + expected + ", Got: " + result);
            }
        } catch (Exception e) {
            System.err.println("[UNEXPECTED ERROR] " + input + " | " + e.getMessage());
        }
    }

    private static void testError(String input, Map<String, Double> variables) {
        try {
            Lexer lexer = new Lexer(input);
            Parser parser = new Parser(lexer.tokenize());
            Expression expression = parser.parse();
            Evaluator evaluator = new Evaluator();

            evaluator.evaluate(expression, variables);

            System.err.println("[FAIL - SHOULD THROW] " + input);
        } catch (Exception e) {
            System.out.println("[OK - CAUGHT] " + input + " -> " + e.getMessage());
        }
    }
}