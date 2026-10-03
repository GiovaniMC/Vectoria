package vectoria.modid.math;

public class LexerTest {
    public static void main(String[] args) {
        Lexer lexer = new Lexer("x^2 + 3.14*sin(theta)");

        for (Token token : lexer.tokenize()) {
            System.out.println(token);
        }
    }
}

//Resultado esperado
//Token[type=IDENTIFIER, value=x]
//Token[type=POWER, value=^]
//Token[type=NUMBER, value=2]
//Token[type=PLUS, value=+]
//Token[type=NUMBER, value=3.14]
//Token[type=MULTIPLY, value=*]
//Token[type=IDENTIFIER, value=sin]
//Token[type=LEFT_PAREN, value=(]
//Token[type=IDENTIFIER, value=theta]
//Token[type=RIGHT_PAREN, value=)]