package operations;

// Класс вычисления 10^a
public class TenPower extends UnaryOperation {
    @Override
    public String getName() {
        return "10^x";
    }

    @Override
    public double calculate(double a) {
        return Math.pow(10, a);
    }
}

