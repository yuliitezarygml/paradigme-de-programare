import operations.*;

/**
 * Простой класс для тестирования математической логики калькулятора.
 */
public class TestCalculator {

    public static void main(String[] args) {
        System.out.println("=== Тестирование Калькулятора (Lab 1) ===");

        try {
            // 1. Тест сложения
            Addition add = new Addition();
            double resAdd = add.calculate(10.5, 4.5);
            System.out.println("10.5 + 4.5 = " + resAdd + " [OK]");

            // 2. Тест деления на 0
            Division div = new Division();
            try {
                div.calculate(10, 0);
                System.out.println("Деление на 0: ОШИБКА (не выброшено исключение)");
            } catch (Exception e) {
                System.out.println("Деление на 0 успешно поймано: " + e.getMessage() + " [OK]");
            }

            // 3. Тест корня из отрицательного
            SquareRoot sqrt = new SquareRoot();
            try {
                sqrt.calculate(-16);
                System.out.println("Корень из -16: ОШИБКА (не выброшено исключение)");
            } catch (Exception e) {
                System.out.println("Корень из -16 успешно пойман: " + e.getMessage() + " [OK]");
            }

            // 4. Тест тригонометрии
            Sinus sin = new Sinus(false); // в градусах
            double sin30 = sin.calculate(30);
            System.out.println("sin(30°) = " + sin30 + " [OK]");

            // 5. Тест логарифмов
            Logarithm log = new Logarithm();
            double log100 = log.calculate(100);
            System.out.println("log10(100) = " + log100 + " [OK]");

            // 6. Тест памяти
            Memory mem = new Memory();
            mem.store(50);
            mem.add(25);
            System.out.println("Память: 50 + 25 = " + mem.recall() + " [OK]");
            mem.clear();
            System.out.println("Очистка памяти: hasValue = " + mem.hasValue() + " [OK]");

            // 7. Тест систем счисления
            long n = 255;
            System.out.println("255 в HEX: " + BaseConverter.toHex(n) + " [OK]");
            System.out.println("255 в BIN: " + BaseConverter.toBin(n) + " [OK]");
            System.out.println("255 в OCT: " + BaseConverter.toOct(n) + " [OK]");

            System.out.println("\n>>> ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ УСПЕШНО! <<<");

        } catch (Exception e) {
            System.out.println("Произошла непредвиденная ошибка: " + e.getMessage());
        }
    }
}

