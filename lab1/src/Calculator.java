import operations.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Главный класс калькулятора с графическим интерфейсом на Swing.
 * 
 * ══════════════════════════════════════════════════════════════════════════════════
 * 📌 ПРИНЦИП ООП #2: НАСЛЕДОВАНИЕ (INHERITANCE: extends JFrame)
 * Класс Calculator повторно использует готовый функционал оконной системы Swing.
 * ══════════════════════════════════════════════════════════════════════════════════
 */
public class Calculator extends JFrame implements ActionListener {

    // ══════════════════════════════════════════════════════════════════════════════════
    // 📌 ПРИНЦИП ООП #4: ИНКАПСУЛЯЦИЯ (ENCAPSULATION: private поля)
    // Все внутренние переменные состояния калькулятора и графические элементы
    // скрыты модификатором private от прямого доступа извне.
    // ══════════════════════════════════════════════════════════════════════════════════
    // Поля экрана
    private JLabel historyLabel;       // Верхняя строчка истории (например "15 + ")
    private JTextField displayField;   // Главный экран с числом
    private JLabel memoryLabel;        // Индикатор "M", если в памяти есть число

    // Панели для кнопок и режимов
    private JPanel buttonsPanel;
    private JPanel programmerPanel;
    private JLabel hexLabel, decLabel, octLabel, binLabel;

    // Выпадающий список выбора режима и кнопка DEG/RAD
    private JComboBox<String> modeBox;
    private JButton btnRadDeg;
    private boolean isRadians = false; // false = градусы, true = радианы

    // Переменные для вычислений
    private double firstNumber = 0;              // Первое число

    // ══════════════════════════════════════════════════════════════════════════════════
    // 📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ (POLYMORPHISM: полиморфная ссылка)
    // Переменная базового типа BinaryOperation хранит любой дочерний объект
    // (Addition, Subtraction, Division, Power, Modulo).
    // ══════════════════════════════════════════════════════════════════════════════════
    private BinaryOperation currentOperation = null; // Текущая операция (+, -, *, / и т.д.)
    private boolean isNewInput = true;           // Начинать ли ввод нового числа
    private Memory memory = new Memory();        // Объект для работы с памятью (Инкапсуляция)

    // Конструктор: создает окно и запускает интерфейс
    public Calculator() {
        setTitle("Calculator - Laborator 1 (OOP)");
        setSize(360, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // По центру экрана

        initUI();
        buildStandardButtons();
        updateDisplay();
    }

    // Инициализация графических компонентов
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(245, 245, 245));

        // 1. Верхняя панель: выбор режима и DEG/RAD
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(245, 245, 245));

        String[] modes = {"Standard", "Științific", "Programator (Baze)"};
        modeBox = new JComboBox<>(modes);
        modeBox.setFont(new Font("Arial", Font.BOLD, 14));
        modeBox.addActionListener(e -> changeMode());
        topBar.add(modeBox, BorderLayout.WEST);

        btnRadDeg = new JButton("DEG");
        btnRadDeg.setFont(new Font("Arial", Font.PLAIN, 12));
        btnRadDeg.setVisible(false); // Видна только в научном режиме
        btnRadDeg.addActionListener(e -> {
            isRadians = !isRadians;
            btnRadDeg.setText(isRadians ? "RAD" : "DEG");
        });
        topBar.add(btnRadDeg, BorderLayout.EAST);

        // 2. Дисплей: история + текущее число + индикатор памяти в углу
        JPanel screenPanel = new JPanel(new BorderLayout());
        screenPanel.setBackground(new Color(245, 245, 245));

        JPanel displayHeader = new JPanel(new BorderLayout());
        displayHeader.setBackground(new Color(245, 245, 245));
        displayHeader.setBorder(new EmptyBorder(2, 4, 2, 4));

        memoryLabel = new JLabel(" ");
        memoryLabel.setFont(new Font("Arial", Font.BOLD, 13));
        memoryLabel.setForeground(new Color(99, 102, 241));
        displayHeader.add(memoryLabel, BorderLayout.WEST);

        historyLabel = new JLabel(" ");
        historyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        historyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        historyLabel.setForeground(Color.GRAY);
        historyLabel.setToolTipText("Подсказка: дважды кликните для показа справки по принципам ООП");
        historyLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showOopInfo();
                }
            }
        });
        displayHeader.add(historyLabel, BorderLayout.EAST);

        displayField = new JTextField("0");
        displayField.setEditable(false);
        displayField.setHorizontalAlignment(SwingConstants.RIGHT);
        displayField.setFont(new Font("Arial", Font.BOLD, 36));
        displayField.setBackground(new Color(245, 245, 245));
        displayField.setBorder(null);

        screenPanel.add(displayHeader, BorderLayout.NORTH);
        screenPanel.add(displayField, BorderLayout.CENTER);

        // 3. Панель памяти: MC, MR, M+, M-, MS
        JPanel memPanel = new JPanel(new GridLayout(1, 5, 4, 4));
        memPanel.setBackground(new Color(245, 245, 245));

        memPanel.add(createButton("MC", false));
        memPanel.add(createButton("MR", false));
        memPanel.add(createButton("M+", false));
        memPanel.add(createButton("M-", false));
        memPanel.add(createButton("MS", false));

        // 4. Панель для режима программиста (HEX, DEC, OCT, BIN)
        programmerPanel = new JPanel(new GridLayout(4, 1, 2, 2));
        programmerPanel.setBackground(Color.WHITE);
        programmerPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        programmerPanel.setVisible(false);

        hexLabel = new JLabel(" HEX: 0");
        decLabel = new JLabel(" DEC: 0");
        octLabel = new JLabel(" OCT: 0");
        binLabel = new JLabel(" BIN: 0");

        Font progFont = new Font("Monospaced", Font.BOLD, 13);
        hexLabel.setFont(progFont);
        decLabel.setFont(progFont);
        octLabel.setFont(progFont);
        binLabel.setFont(progFont);

        programmerPanel.add(hexLabel);
        programmerPanel.add(decLabel);
        programmerPanel.add(octLabel);
        programmerPanel.add(binLabel);

        // Собираем весь верхний блок вместе
        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        topArea.setBackground(new Color(245, 245, 245));
        topArea.add(topBar);
        topArea.add(Box.createVerticalStrut(5));
        topArea.add(screenPanel);
        topArea.add(programmerPanel);
        topArea.add(Box.createVerticalStrut(5));
        topArea.add(memPanel);
        topArea.add(Box.createVerticalStrut(5));

        mainPanel.add(topArea, BorderLayout.NORTH);

        // 5. Панель кнопок
        buttonsPanel = new JPanel();
        buttonsPanel.setBackground(new Color(245, 245, 245));
        mainPanel.add(buttonsPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Подключаем ввод с клавиатуры
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                handleKeyInput(e.getKeyChar());
            }
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleEquals();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    handleBackspace();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    handleClearAll();
                }
            }
        });
        setFocusable(true);
    }

    // Вспомогательный метод создания кнопок
    private JButton createButton(String text, boolean isNumber) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", isNumber ? Font.BOLD : Font.PLAIN, 16));
        btn.setFocusable(false); // Чтобы фокус оставался на окне для клавиатуры
        btn.setBackground(isNumber ? Color.WHITE : new Color(238, 238, 238));

        // Выделяем кнопку равно: крупный четкий знак "="
        if ("=".equals(text)) {
            btn.setForeground(new Color(99, 102, 241)); // Контрастный фиолетовый цвет знака "="
            btn.setFont(new Font("Arial", Font.BOLD, 24));
        }

        btn.addActionListener(this);
        return btn;
    }

    // Смена режима калькулятора (Standard / Științific / Programator)
    private void changeMode() {
        int mode = modeBox.getSelectedIndex();
        buttonsPanel.removeAll();

        if (mode == 0) { // Standard
            setSize(360, 560);
            btnRadDeg.setVisible(false);
            programmerPanel.setVisible(false);
            buildStandardButtons();
        } else if (mode == 1) { // Științific
            setSize(440, 640);
            btnRadDeg.setVisible(true);
            programmerPanel.setVisible(false);
            buildScientificButtons();
        } else if (mode == 2) { // Programator
            setSize(400, 640);
            btnRadDeg.setVisible(false);
            programmerPanel.setVisible(true);
            buildProgrammerButtons();
            updateBases();
        }

        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    // Кнопки стандартного режима (как в image4.png)
    private void buildStandardButtons() {
        buttonsPanel.setLayout(new GridLayout(6, 4, 4, 4));

        String[] btns = {
            "%", "CE", "C", "⌫",
            "1/x", "x²", "√x", "/",
            "7", "8", "9", "*",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "+/-", "0", ".", "="
        };

        for (String text : btns) {
            boolean isNum = "0123456789".contains(text);
            buttonsPanel.add(createButton(text, isNum));
        }
    }

    // Кнопки научного режима (как в image3.png)
    private void buildScientificButtons() {
        buttonsPanel.setLayout(new GridLayout(7, 5, 4, 4));

        String[] btns = {
            "π", "e", "CE", "C", "⌫",
            "x²", "1/x", "|x|", "exp", "mod",
            "√x", "sin", "cos", "tan", "/",
            "x^y", "7", "8", "9", "*",
            "10^x", "4", "5", "6", "-",
            "log", "1", "2", "3", "+",
            "ln", "n!", "+/-", "0", "="
        };

        for (String text : btns) {
            boolean isNum = "0123456789".contains(text);
            buttonsPanel.add(createButton(text, isNum));
        }
    }

    // Кнопки режима программиста (системы счисления)
    private void buildProgrammerButtons() {
        buttonsPanel.setLayout(new GridLayout(6, 4, 4, 4));

        String[] btns = {
            "A", "B", "C_btn", "⌫",
            "C", "D", "E", "F",
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "+/-", "0", "=", "+"
        };

        for (String text : btns) {
            String label = "C_btn".equals(text) ? "C" : text;
            JButton b = createButton(label, "0123456789".contains(text));
            if ("C_btn".equals(text)) {
                b.setActionCommand("C");
            }
            buttonsPanel.add(b);
        }
    }

    // Обработка нажатий на кнопки
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        // 0. Защита: если на экране отображается ошибка ("Eroare..."), блокируем действия кроме C и CE
        if (displayField.getText().startsWith("Eroare") && !"C".equals(cmd) && !"CE".equals(cmd)) {
            return;
        }

        // 1. Цифры и точка
        if ("0123456789".contains(cmd) || ("ABCDEF".contains(cmd) && modeBox.getSelectedIndex() == 2)) {
            handleDigit(cmd);
        } else if (".".equals(cmd)) {
            handleDot();
        } 
        // 2. Очистка и удаление
        else if ("C".equals(cmd)) {
            handleClearAll();
        } else if ("CE".equals(cmd)) {
            displayField.setText("0");
            isNewInput = true;
        } else if ("⌫".equals(cmd)) {
            handleBackspace();
        } else if ("+/-".equals(cmd)) {
            handleSign();
        }
        // 3. Константы
        else if ("π".equals(cmd)) {
            displayField.setText(format(Math.PI));
            isNewInput = true;
        } else if ("e".equals(cmd)) {
            displayField.setText(format(Math.E));
            isNewInput = true;
        }
        // 4. Память
        else if ("MC".equals(cmd)) {
            memory.clear();
        } else if ("MR".equals(cmd)) {
            if (memory.hasValue()) {
                displayField.setText(format(memory.recall()));
                isNewInput = true;
            }
        } else if ("MS".equals(cmd)) {
            memory.store(getCurrentNumber());
            isNewInput = true;
        } else if ("M+".equals(cmd)) {
            memory.add(getCurrentNumber());
            isNewInput = true;
        } else if ("M-".equals(cmd)) {
            memory.subtract(getCurrentNumber());
            isNewInput = true;
        }
        // 5. Бинарные операции (+, -, *, /, x^y, mod)
        else if ("+".equals(cmd)) {
            setBinaryOperation(new Addition(), "+");
        } else if ("-".equals(cmd)) {
            setBinaryOperation(new Subtraction(), "-");
        } else if ("*".equals(cmd)) {
            setBinaryOperation(new Multiplication(), "×");
        } else if ("/".equals(cmd)) {
            setBinaryOperation(new Division(), "÷");
        } else if ("x^y".equals(cmd)) {
            setBinaryOperation(new Power(), "^");
        } else if ("mod".equals(cmd)) {
            setBinaryOperation(new Modulo(), "mod");
        }
        // 6. Равно
        else if ("=".equals(cmd)) {
            handleEquals();
        }
        // 7. Унарные операции
        else {
            handleUnaryOperation(cmd);
        }

        updateDisplay();
    }

    // Ввод цифры
    private void handleDigit(String digit) {
        if (isNewInput || "0".equals(displayField.getText()) || displayField.getText().startsWith("Eroare")) {
            displayField.setText(digit);
            isNewInput = false;
        } else {
            displayField.setText(displayField.getText() + digit);
        }
    }

    // Ввод точки
    private void handleDot() {
        if (isNewInput || displayField.getText().startsWith("Eroare")) {
            displayField.setText("0.");
            isNewInput = false;
        } else if (!displayField.getText().contains(".")) {
            displayField.setText(displayField.getText() + ".");
        }
    }

    // Смена знака +/-
    private void handleSign() {
        double val = getCurrentNumber();
        if (val != 0) {
            val = -val;
            displayField.setText(format(val));
        }
    }

    // Удаление последней цифры (Backspace)
    private void handleBackspace() {
        String text = displayField.getText();
        if (text.length() > 1 && !text.startsWith("Eroare")) {
            displayField.setText(text.substring(0, text.length() - 1));
        } else {
            displayField.setText("0");
            isNewInput = true;
        }
    }

    // Полный сброс (C)
    private void handleClearAll() {
        firstNumber = 0;
        currentOperation = null;
        historyLabel.setText(" ");
        displayField.setText("0");
        isNewInput = true;
    }

    // Установка бинарной операции
    private void setBinaryOperation(BinaryOperation op, String symbol) {
        try {
            firstNumber = getCurrentNumber();
            currentOperation = op;
            historyLabel.setText(format(firstNumber) + " " + symbol);
            isNewInput = true;
        } catch (Exception ex) {
            displayField.setText("Eroare!");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════
    // 📌 ПРИНЦИП ООП #3: ДИНАМИЧЕСКИЙ ПОЛИМОРФИЗМ (DYNAMIC DISPATCH / LATE BINDING)
    // Вызов currentOperation.calculate(firstNumber, secondNumber):
    // В зависимости от того, какой класс был подставлен (Addition, Subtraction, Division и т.д.),
    // виртуальная машина Java сама выбирает нужную реализацию формулы в рантайме.
    // ══════════════════════════════════════════════════════════════════════════════════
    private void handleEquals() {
        // Вызов окна справки ООП при вводе 777 или 1337 и нажатии "="
        if (currentOperation == null && (getCurrentNumber() == 777 || getCurrentNumber() == 1337)) {
            showOopInfo();
            return;
        }

        if (currentOperation != null) {
            try {
                double secondNumber = getCurrentNumber();
                double result = currentOperation.calculate(firstNumber, secondNumber);

                historyLabel.setText(format(firstNumber) + " " + currentOperation.getName() + " " + format(secondNumber) + " =");
                displayField.setText(format(result));

                firstNumber = result;
                currentOperation = null;
                isNewInput = true;
            } catch (Exception ex) {
                displayField.setText("Eroare: " + ex.getMessage());
                currentOperation = null;
                isNewInput = true;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════════
    // 📌 ПРИНЦИП ООП #3: ПОЛИМОРФИЗМ ДЛЯ УНАРНЫХ ОПЕРАЦИЙ
    // Через абстрактный тип UnaryOperation op вызывается метод calculate(val)
    // ══════════════════════════════════════════════════════════════════════════════════
    private void handleUnaryOperation(String opName) {
        try {
            double val = getCurrentNumber();
            UnaryOperation op = null;

            switch (opName) {
                case "√x":
                    op = new SquareRoot();
                    break;
                case "x²":
                    op = new Square();
                    break;
                case "1/x":
                    op = new Reciprocal();
                    break;
                case "%":
                    op = new Percent();
                    break;
                case "n!":
                    op = new Factorial();
                    break;
                case "|x|":
                    op = new AbsOperation();
                    break;
                case "sin":
                    op = new Sinus(isRadians);
                    break;
                case "cos":
                    op = new Cosinus(isRadians);
                    break;
                case "tan":
                    op = new Tangens(isRadians);
                    break;
                case "log":
                    op = new Logarithm();
                    break;
                case "ln":
                    op = new NaturalLog();
                    break;
                case "exp":
                    op = new ExpOperation();
                    break;
                case "10^x":
                    op = new TenPower();
                    break;
            }

            if (op != null) {
                double res = op.calculate(val);
                historyLabel.setText(op.getName() + "(" + format(val) + ")");
                displayField.setText(format(res));
                isNewInput = true;
            }
        } catch (Exception ex) {
            displayField.setText("Eroare: " + ex.getMessage());
            isNewInput = true;
        }
    }

    // Получить число с экрана
    private double getCurrentNumber() {
        try {
            return Double.parseDouble(displayField.getText());
        } catch (Exception e) {
            return 0;
        }
    }

    // Форматирование чисел для красивого вывода (например 5 вместо 5.0)
    private String format(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "Eroare";
        }
        if (d == (long) d) {
            return String.format(Locale.US, "%d", (long) d);
        } else {
            return String.format(Locale.US, "%.8f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
        }
    }

    // Обновление дисплея, индикатора памяти и систем счисления
    private void updateDisplay() {
        updateMemoryIndicator();
        if (modeBox.getSelectedIndex() == 2) {
            updateBases();
        }
    }

    // Обновление индикатора работы с памятью в левом верхнем углу дисплея
    private void updateMemoryIndicator() {
        if (memory.hasValue()) {
            memoryLabel.setText("M: " + format(memory.recall()));
            memoryLabel.setToolTipText("Valoare în memorie: " + format(memory.recall()));
        } else {
            memoryLabel.setText(" ");
            memoryLabel.setToolTipText(null);
        }
    }

    // Обновление HEX, DEC, OCT, BIN в режиме программиста
    private void updateBases() {
        try {
            long val = (long) getCurrentNumber();
            hexLabel.setText(" HEX: " + BaseConverter.toHex(val));
            decLabel.setText(" DEC: " + val);
            octLabel.setText(" OCT: " + BaseConverter.toOct(val));
            binLabel.setText(" BIN: " + BaseConverter.toBin(val));
        } catch (Exception ignored) {
        }
    }

    // Ввод с клавиатуры
    private void handleKeyInput(char ch) {
        if (displayField.getText().startsWith("Eroare")) {
            return;
        }
        if (Character.isDigit(ch)) {
            handleDigit(String.valueOf(ch));
        } else if (ch == '.' || ch == ',') {
            handleDot();
        } else if (ch == '+') {
            setBinaryOperation(new Addition(), "+");
        } else if (ch == '-') {
            setBinaryOperation(new Subtraction(), "-");
        } else if (ch == '*') {
            setBinaryOperation(new Multiplication(), "×");
        } else if (ch == '/') {
            setBinaryOperation(new Division(), "÷");
        } else if (ch == '=') {
            handleEquals();
        }
        updateDisplay();
    }

    // ══════════════════════════════════════════════════════════════════════════════════
    // 📌 СПРАВКА ДЛЯ ПРЕПОДАВАТЕЛЯ: 4 ОСНОВНЫХ ПРИНЦИПА ООП
    // Открывается при двойном клике на строку истории или при вводе 777 / 1337 и '='
    // ══════════════════════════════════════════════════════════════════════════════════
    private void showOopInfo() {
        String message = "<html><body style='width: 330px; font-family: sans-serif; padding: 5px;'>"
                + "<h2 style='color: #4F46E5; margin-top: 0;'>📘 4 Принципа ООП в Lab 1</h2>"
                + "<p><b>Автор / Разработчик:</b> Iulian</p>"
                + "<hr style='border: 0; border-top: 1px solid #ccc;'/>"
                + "<p><b>1. 🔹 АБСТРАКЦИЯ (Abstraction):</b><br/>"
                + "Интерфейс <code>Operation</code> и абстрактные классы <code>BinaryOperation</code>, "
                + "<code>UnaryOperation</code> задают единый контракт <code>calculate()</code>, скрывая формулы.</p>"
                + "<p><b>2. 🔹 НАСЛЕДОВАНИЕ (Inheritance):</b><br/>"
                + "<code>Addition extends BinaryOperation</code>, <code>SquareRoot extends UnaryOperation</code>, "
                + "а окно <code>Calculator extends JFrame</code>.</p>"
                + "<p><b>3. 🔹 ПОЛИМОРФИЗМ (Polymorphism):</b><br/>"
                + "Вызов <code>currentOperation.calculate(a, b)</code> динамически вызывает нужный класс "
                + "без ветвлений в коде вычисления.</p>"
                + "<p><b>4. 🔹 ИНКАПСУЛЯЦИЯ (Encapsulation):</b><br/>"
                + "Скрытое поле <code>private memoryValue</code> в <code>Memory.java</code> доступно "
                + "только через методы <code>store()</code>, <code>recall()</code>, <code>clear()</code>.</p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(
                this,
                message,
                "Справка ООП - Лабораторная работа 1",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}

