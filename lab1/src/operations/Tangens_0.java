package operations;

// Класс вычисления тангенса tan(a)
public class Tangens extends UnaryOperation {
    private boolean isRadians;

    public Tangens(boolean isRadians) {
        this.isRadians = isRadians;
    }

    @Override
    public String getName() {
        return "tan";
    }

    @Override
    public double calculate(double a) throws Exception {
        double angle = isRadians ? a : Math.toRadians(a);
        double cosVal = Math.cos(angle);
        if (Math.abs(cosVal) < 1e-15) {
            throw new Exception("Tangenta nedefinită la 90°!");
        }
        double res = Math.tan(angle);
        if (Math.abs(res) < 1e-15) res = 0;
        return res;
    }
}

