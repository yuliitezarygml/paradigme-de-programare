package operations;

// Класс вычисления косинуса cos(a)
public class Cosinus extends UnaryOperation {
    private boolean isRadians;

    public Cosinus(boolean isRadians) {
        this.isRadians = isRadians;
    }

    @Override
    public String getName() {
        return "cos";
    }

    @Override
    public double calculate(double a) {
        double angle = isRadians ? a : Math.toRadians(a);
        double res = Math.cos(angle);
        if (Math.abs(res) < 1e-15) res = 0; // Округляем погрешность
        return res;
    }
}

