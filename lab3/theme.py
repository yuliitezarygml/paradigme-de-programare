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
