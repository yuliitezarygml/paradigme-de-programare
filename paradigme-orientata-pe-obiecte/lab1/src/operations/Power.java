package operations;

// Класс возведения в степень (a ^ b)
public class Power extends BinaryOperation {
    @Override
    public String getName() {
        return "^";
    }

    @Override
    public double calculate(double a, double b) {
        return Math.pow(a, b);
    }
}

