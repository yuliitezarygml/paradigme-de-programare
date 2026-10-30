package operations;

// ══════════════════════════════════════════════════════════════════════════════════
// 📌 ПРИНЦИП ООП #1: АБСТРАКЦИЯ (ABSTRACTION)
// 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: implements Operation)
// Абстрактный класс для операций над двумя числами (сложение, вычитание и т.д.)
// ══════════════════════════════════════════════════════════════════════════════════
public abstract class BinaryOperation implements Operation {
    
    // 📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM)
    // Абстрактный метод, который каждый класс-потомок обязан реализовать по-своему
    public abstract double calculate(double a, double b) throws Exception;
}

