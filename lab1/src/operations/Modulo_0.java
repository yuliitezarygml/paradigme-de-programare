package operations;

// Класс остатка от деления (a mod b)
public class Modulo extends BinaryOperation {
    @Override
    public String getName() {
        return "mod";
    }

    @Override
    public double calculate(double a, double b) throws Exception {
        if (b == 0) {
            throw new Exception("Modulo cu zero!");
        }
        return a % b;
    }
}

