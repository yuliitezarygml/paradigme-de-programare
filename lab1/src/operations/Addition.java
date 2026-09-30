package operations;

// ══════════════════════════════════════════════════════════════════════════════════
// 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: extends BinaryOperation)
// ══════════════════════════════════════════════════════════════════════════════════
public class Addition extends BinaryOperation {
    @Override
    public String getName() {
        return "+";
    }

    // 📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM: @Override calculate)
    @Override
    public double calculate(double a, double b) {
        return a + b;
    }
}

