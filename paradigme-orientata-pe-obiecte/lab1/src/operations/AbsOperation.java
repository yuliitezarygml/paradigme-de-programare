package operations;

// Класс вычисления модуля |a|
public class AbsOperation extends UnaryOperation {
    @Override
    public String getName() {
        return "|x|";
    }

    @Override
    public double calculate(double a) {
        return Math.abs(a);
    }
}

