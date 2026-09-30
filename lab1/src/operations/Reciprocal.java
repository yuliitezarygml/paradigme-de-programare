package operations;

// Класс обратного значения (1/a)
public class Reciprocal extends UnaryOperation {
    @Override
    public String getName() {
        return "1/x";
    }

    @Override
    public double calculate(double a) throws Exception {
        if (a == 0) {
            throw new Exception("Împărțire la zero!");
        }
        return 1.0 / a;
    }
}

