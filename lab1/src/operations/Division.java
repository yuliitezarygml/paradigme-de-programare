package operations;

// Класс деления (a / b) с проверкой деления на 0
public class Division extends BinaryOperation {
    @Override
    public String getName() {
        return "/";
    }

    @Override
    public double calculate(double a, double b) throws Exception {
        if (b == 0) {
            throw new Exception("Împărțire la zero!");
        }
        return a / b;
    }
}

