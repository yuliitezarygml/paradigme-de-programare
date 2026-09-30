// ══════════════════════════════════════════════════════════════════════════════════
// 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION)
// Внутреннее состояние памяти калькулятора полностью скрыто от внешнего мира.
// Прямой доступ к переменной memoryValue невозможен извне (модификатор private).
// Любые манипуляции с памятью производятся исключительно через публичные
// методы-шлюзы: store(), recall(), add(), subtract(), clear(), hasValue().
// ══════════════════════════════════════════════════════════════════════════════════
public class Memory {

    // Скрытые (инкапсулированные) поля
    private double memoryValue = 0.0;
    private boolean hasValue = false;

    // MC - очистить память
    public void clear() {
        memoryValue = 0.0;
        hasValue = false;
    }

    // MR - прочитать значение из памяти
    public double recall() {
        return memoryValue;
    }

    // MS - записать число в память
    public void store(double value) {
        memoryValue = value;
        hasValue = true;
    }

    // M+ - прибавить к памяти
    public void add(double value) {
        memoryValue += value;
        hasValue = true;
    }

    // M- - вычесть из памяти
    public void subtract(double value) {
        memoryValue -= value;
        hasValue = true;
    }

    // Проверка, есть ли число в памяти (для индикатора "M" на экране)
    public boolean hasValue() {
        return hasValue;
    }
}

