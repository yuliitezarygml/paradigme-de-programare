package operations;

// Класс вычисления синуса sin(a)
public class Sinus extends UnaryOperation {
    private boolean isRadians;

    public Sinus(boolean isRadians) {
        this.isRadians = isRadians;
    }

    @Override
    public String getName() {
        return "sin";
    }

    @Override
    public double calculate(double a) {
        double angle = isRadians ? a : Math.toRadians(a);
        double res = Math.sin(angle);
        if (Math.abs(res) < 1e-15) res = 0; // Округляем погрешность типа 1.2e-16 до 0
        return res;
    }
}

