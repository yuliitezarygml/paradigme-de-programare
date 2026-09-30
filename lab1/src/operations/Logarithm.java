package operations;

// Класс вычисления десятичного логарифма log10(a)
public class Logarithm extends UnaryOperation {
    @Override
    public String getName() {
        return "log";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a <= 0) {
            throw new Exception("log cere x > 0!");
        }
        return Math.log10(a);
    }
}

