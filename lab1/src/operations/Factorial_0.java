package operations;

// Класс вычисления факториала (n!)
public class Factorial extends UnaryOperation {
    @Override
    public String getName() {
        return "n!";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a < 0 || a != Math.floor(a)) {
            throw new Exception("Factorialul cere întreg pozitiv!");
        }
        if (a > 170) {
            throw new Exception("Număr prea mare pentru factorial!");
        }
        int n = (int) a;
        double result = 1;
        for (int i = 1; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}

