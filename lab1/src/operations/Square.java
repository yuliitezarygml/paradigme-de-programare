package operations;

// Класс возведения в квадрат (a²)
public class Square extends UnaryOperation {
    @Override
    public String getName() {
        return "x²";
    }

    @Override
    public double calculate(double a) {
        return a * a;
    }
}

