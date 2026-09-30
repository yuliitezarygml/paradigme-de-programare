package operations;

// Класс вычисления квадратного корня (√a)
public class SquareRoot extends UnaryOperation {
    @Override
    public String getName() {
        return "√";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a < 0) {
            throw new Exception("Radical din număr negativ!");
        }
        return Math.sqrt(a);
    }
}

