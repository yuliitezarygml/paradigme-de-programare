package operations;

// Класс вычисления натурального логарифма ln(a)
public class NaturalLog extends UnaryOperation {
    @Override
    public String getName() {
        return "ln";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a <= 0) {
            throw new Exception("ln cere x > 0!");
        }
        return Math.log(a);
    }
}

