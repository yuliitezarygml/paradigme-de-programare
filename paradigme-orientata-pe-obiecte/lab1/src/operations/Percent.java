package operations;

// Класс вычисления процента (a / 100)
public class Percent extends UnaryOperation {
    @Override
    public String getName() {
        return "%";
    }

    @Override
    public double calculate(double a) {
        return a / 100.0;
    }
}

