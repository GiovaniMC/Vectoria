package vectoria.modid.math.lexer;

import vectoria.modid.math.evaluation.Evaluator;
import vectoria.modid.math.expression.Expression;
import vectoria.modid.math.lexer.Lexer;
import vectoria.modid.math.parser.Parser;

import java.util.Map;

public class LexerTest {

    public static void main(String[] args) {
        String input = "\"ab\"";

        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer.tokenize());

        Expression expression = parser.parse();

        Evaluator evaluator = new Evaluator();

        double result = evaluator.evaluate(
                expression,
                Map.of("a",3.0,"b",4.0)
        );


        System.out.println(expression);
        System.out.println(result);
    }
}