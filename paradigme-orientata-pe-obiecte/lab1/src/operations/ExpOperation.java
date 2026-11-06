package operations;

// Класс вычисления экспоненты e^a
public class ExpOperation extends UnaryOperation {
    @Override
    public String getName() {
        return "e^x";
    }

    @Override
    public double calculate(double a) {
        return Math.exp(a);
    }
}

