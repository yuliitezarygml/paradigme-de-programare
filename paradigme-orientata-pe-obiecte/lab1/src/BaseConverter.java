// Класс для перевода чисел в разные системы счисления (HEX, BIN, OCT)
// Требование пункта f: Transformarea hexazecimala (sau din alt sistem)
public class BaseConverter {

    // Перевод в 16-ричную систему (HEX)
    public static String toHex(long number) {
        return Long.toHexString(number).toUpperCase();
    }

    // Перевод в 8-ричную систему (OCT)
    public static String toOct(long number) {
        return Long.toOctalString(number);
    }

    // Перевод в 2-ичную систему (BIN)
    public static String toBin(long number) {
        return Long.toBinaryString(number);
    }
}

