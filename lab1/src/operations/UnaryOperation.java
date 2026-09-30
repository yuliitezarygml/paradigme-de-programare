package operations;

// ══════════════════════════════════════════════════════════════════════════════════
// 📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
// 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: implements Operation)
// Базовый абстрактный класс для унарных функций (корень, sin, cos, log и т.д.)
// ══════════════════════════════════════════════════════════════════════════════════
public abstract class UnaryOperation implements Operation {

    // 📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM)
    // Метод, который каждый конкретный класс реализует по-своему
    public abstract double calculate(double a) throws Exception;
}

