package operations;

// Класс вычитания (a - b)
public class Subtraction extends BinaryOperation {
    @Override
    public String getName() {
        return "-";
    }

    @Override
    public double calculate(double a, double b) {
        return a - b;
    }
}

