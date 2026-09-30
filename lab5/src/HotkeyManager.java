import java.awt.*;
import java.awt.event.KeyEvent;

// Менеджер горячих клавиш (Hotkeys) для быстрого вызова резидентного приложения
// Лабораторная работа 5 - пункт e
public class HotkeyManager {

    private final Runnable toggleAction;
    private boolean enabled = true;

    public HotkeyManager(Runnable toggleAction) {
        this.toggleAction = toggleAction;
        initHotkey();
    }

    private void initHotkey() {
        // Устанавливаем глобальный диспетчер клавиш для приложения
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (!enabled || e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }

                // Горячая клавиша: Ctrl + Shift + W (или Cmd + Shift + W на Mac)
                boolean isCtrlOrCmd = (e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK)) != 0;
                boolean isShift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;

                if (isCtrlOrCmd && isShift && e.getKeyCode() == KeyEvent.VK_W) {
                    System.out.println("Сработала горячая клавиша: [Ctrl/Cmd + Shift + W]");
                    if (toggleAction != null) {
                        toggleAction.run();
                    }
                    return true; // событие обработано
                }

                return false;
            }
        });
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
