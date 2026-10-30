package operations;

// Класс вычисления логарифма по основанию 2 (log2(a))
public class Log2 extends UnaryOperation {
    @Override
    public String getName() {
        return "log2";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a <= 0) {
            throw new Exception("log2 cere x > 0!");
        }
        return Math.log(a) / Math.log(2.0);
    }
}

