package operations;

// Класс умножения (a * b)
public class Multiplication extends BinaryOperation {
    @Override
    public String getName() {
        return "*";
    }

    @Override
    public double calculate(double a, double b) {
        return a * b;
    }
}

