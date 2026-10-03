package vectoria.modid.math.evaluation;

public class MathDomainException extends ArithmeticException {

    public MathDomainException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}