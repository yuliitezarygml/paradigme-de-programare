"""Все основные цвета и шрифты. Меняйте их здесь для обоих окон."""

from tkinter import ttk

BACKGROUND = "#f1f5f9"
SURFACE = "#ffffff"
TEXT = "#172033"
MUTED = "#64748b"
BLUE = "#2563eb"
OWN_MESSAGE = "#eaf2ff"
FONT = "Arial"


def apply_theme(window):
    window.configure(background=BACKGROUND)
    style = ttk.Style(window)
    # Clam позволяет задать одинаковые цвета на macOS, Windows и Linux.
    style.theme_use("clam")
    style.configure(".", font=(FONT, 11), background=BACKGROUND, foreground=TEXT)
    style.configure("TFrame", background=BACKGROUND)
    style.configure("Card.TFrame", background=SURFACE)
    style.configure("TLabel", background=BACKGROUND, foreground=TEXT)
    style.configure("Title.TLabel", font=(FONT, 23, "bold"))
    style.configure("Muted.TLabel", foreground=MUTED, font=(FONT, 10))
    style.configure("Card.TLabel", background=SURFACE)
    style.configure("CardMuted.TLabel", background=SURFACE, foreground=MUTED, font=(FONT, 10))
    style.configure("Section.TLabel", background=SURFACE, font=(FONT, 13, "bold"))
    style.configure("Online.TLabel", foreground="#15803d", background="#dcfce7", padding=(10, 5))
    style.configure("Offline.TLabel", foreground=MUTED, background="#e2e8f0", padding=(10, 5))
    style.configure("TEntry", padding=7, fieldbackground=SURFACE, foreground=TEXT,
                    bordercolor="#d8e1ed", lightcolor=SURFACE, darkcolor=SURFACE)
    style.map("TEntry", fieldbackground=[("disabled", "#f1f5f9")],
              foreground=[("disabled", MUTED)])
    style.configure("TButton", background=SURFACE, foreground=TEXT, padding=(12, 8),
                    bordercolor="#d8e1ed", lightcolor=SURFACE, darkcolor=SURFACE, borderwidth=1)
    style.map("TButton", background=[("active", "#e9eef5"), ("disabled", "#f1f5f9")],
              foreground=[("disabled", "#94a3b8")])
    style.configure("Accent.TButton", background=BLUE, foreground="white", bordercolor=BLUE)
    style.map("Accent.TButton", background=[("disabled", "#dbeafe"), ("active", "#1d4ed8")],
              foreground=[("disabled", "#8099bc"), ("!disabled", "white")])
    style.configure("Vertical.TScrollbar", background="#cbd5e1", troughcolor=SURFACE,
                    bordercolor=SURFACE, arrowcolor=MUTED, width=10)

# Цвета клиента в стиле тёмного Telegram. Панель сервера остаётся светлой.
CHAT_BACKGROUND = "#0e1621"
CHAT_PANEL = "#17212d"
CHAT_INPUT = "#202f3f"
CHAT_TEXT = "#e6edf3"
CHAT_MUTED = "#8da4b5"


def apply_chat_theme(window):
    apply_theme(window)
    window.configure(background=CHAT_BACKGROUND)
    style = ttk.Style(window)
    style.configure(".", background=CHAT_PANEL, foreground=CHAT_TEXT)
    for name in ("TFrame", "Card.TFrame", "TLabel", "Card.TLabel"):
        style.configure(name, background=CHAT_PANEL, foreground=CHAT_TEXT)
    style.configure("Title.TLabel", background=CHAT_PANEL, foreground=CHAT_TEXT, font=(FONT, 21, "bold"))
    style.configure("Section.TLabel", background=CHAT_PANEL, foreground=CHAT_TEXT, font=(FONT, 16, "bold"))
    for name in ("Muted.TLabel", "CardMuted.TLabel"):
        style.configure(name, background=CHAT_PANEL, foreground=CHAT_MUTED)
    style.configure("Online.TLabel", background="#1c463e", foreground="#6de2b9")
    style.configure("Offline.TLabel", background="#253648", foreground=CHAT_MUTED)
    style.configure("TEntry", fieldbackground=CHAT_INPUT, foreground=CHAT_TEXT,
                    bordercolor="#2b3d50", lightcolor=CHAT_INPUT, darkcolor=CHAT_INPUT)
    style.map("TEntry", fieldbackground=[("disabled", "#18232f")],
              foreground=[("disabled", CHAT_MUTED)])
    style.configure("TButton", background="#233347", foreground=CHAT_TEXT,
                    bordercolor="#2b3f52", lightcolor="#233347", darkcolor="#233347")
    style.map("TButton", background=[("disabled", "#1b2938"), ("active", "#304761")],
              foreground=[("disabled", "#5e758c")])
    style.configure("Accent.TButton", background="#2b92c7", foreground="white", bordercolor="#2b92c7")
    style.map("Accent.TButton", background=[("disabled", "#21405a"), ("active", "#3ca4d7")],
              foreground=[("disabled", "#7592a9"), ("!disabled", "white")])
    style.configure("Vertical.TScrollbar", background="#2c4055", troughcolor=CHAT_BACKGROUND,
                    bordercolor=CHAT_BACKGROUND, arrowcolor=CHAT_MUTED)
