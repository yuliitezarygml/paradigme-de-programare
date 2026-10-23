"""Лента сообщений: Canvas рисует отдельный пузырёк для каждого сообщения."""

import tkinter as tk
from datetime import datetime, timedelta
from tkinter import font as tkfont
from tkinter import ttk


BACKGROUND = "#0e1621"
OTHER_BUBBLE = "#1b2b3a"
OWN_BUBBLE = "#245b70"
TEXT = "#e6edf3"
MUTED = "#8da4b5"
ACCENT = "#69b9e8"
AUTHOR_COLORS = ("#82c7ed", "#b7a4ed", "#e5b98c", "#8bd1b2", "#e69eb2", "#cbd994")


class MessageView(ttk.Frame):
    """Прокручиваемая лента; выбранное сообщение передаётся в callback по id."""

    def __init__(self, parent, on_select_callback):
        super().__init__(parent)
        self.on_select_callback = on_select_callback
        self.canvas = tk.Canvas(
            self, background=BACKGROUND, borderwidth=0, highlightthickness=0,
            yscrollincrement=20, takefocus=True,
        )
        self.scrollbar = ttk.Scrollbar(self, orient="vertical", command=self.canvas.yview)
        self.canvas.configure(yscrollcommand=self.scrollbar.set)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        self.scrollbar.grid(row=0, column=1, sticky="ns")
        self.columnconfigure(0, weight=1)
        self.rowconfigure(0, weight=1)

        self._body_font = tkfont.Font(self, family="Arial", size=11)
        self._author_font = tkfont.Font(self, family="Arial", size=10, weight="bold")
        self._small_font = tkfont.Font(self, family="Arial", size=9)
        self._file_font = tkfont.Font(self, family="Arial", size=10, weight="bold")
        self._messages = {}
        self._own_name = ""
        self._selected = None
        self._positions = {}
        self._bubble_items = {}
        self._content_height = 1
        self._empty_text = "Пока здесь тихо. Напишите первое сообщение."
        self._resize_job = None

        self.canvas.bind("<Configure>", self._on_resize)
        self.canvas.bind("<MouseWheel>", self._on_mousewheel)
        self.canvas.bind("<Button-4>", lambda event: self._scroll(-3))
        self.canvas.bind("<Button-5>", lambda event: self._scroll(3))
        self.canvas.tag_bind("message", "<Button-1>", self._on_message_click)

    def render(self, messages, own_name, scroll_to_end=True, history=False,
               empty_text="Пока здесь тихо. Напишите первое сообщение."):
        """Перерисовать ленту. При загрузке истории сохраняется видимое сообщение."""
        anchor = self._scroll_anchor()
        self._messages = dict(messages)
        self._own_name = own_name
        self._empty_text = empty_text
        if self._selected not in self._messages:
            self._selected = None
        if self._resize_job is not None:
            self.after_cancel(self._resize_job)
            self._resize_job = None
        # История добавляется сверху, поэтому текущая строка остаётся на месте.
        self._draw(scroll_to_end=scroll_to_end and not history, anchor=anchor)

    def near_bottom(self):
        """True, если пользователь находится в конце переписки или рядом с ним."""
        bottom = self.canvas.yview()[1]
        return (1 - bottom) * self._content_height <= 48

    def highlight(self, message_id):
        """Выделить пузырёк без вызова callback (None снимает выделение)."""
        old_id = self._selected
        self._selected = message_id if message_id in self._messages else None
        for item_id in (old_id, self._selected):
            bubble = self._bubble_items.get(item_id)
            if bubble is not None:
                selected = item_id == self._selected
                self.canvas.itemconfigure(
                    bubble, outline=ACCENT if selected else "", width=2 if selected else 0,
                )

    def _on_message_click(self, event):
        for tag in self.canvas.gettags("current"):
            if tag.startswith("message:"):
                message_id = int(tag.split(":", 1)[1])
                self.highlight(message_id)
                self.canvas.focus_set()
                self.on_select_callback(message_id)
                return "break"
        return None

    def _scroll(self, units):
        self.canvas.yview_scroll(units, "units")
        return "break"

    def _on_mousewheel(self, event):
        # Windows обычно передаёт 120 за шаг, macOS — несколько единиц.
        delta = event.delta
        units = -int(delta / 120) * 3 if abs(delta) >= 120 else -int(delta)
        return self._scroll(units)

    def _on_resize(self, event):
        if self._resize_job is None:
            self._resize_job = self.after_idle(self._redraw_after_resize)

    def _redraw_after_resize(self):
        self._resize_job = None
        at_bottom = self.near_bottom()
        self._draw(scroll_to_end=at_bottom, anchor=self._scroll_anchor())

    def _scroll_anchor(self):
        """Запоминаем сообщение у верхнего края и расстояние до этого края."""
        top = self.canvas.canvasy(0)
        for message_id, bounds in self._positions.items():
            if bounds[3] >= top:
                return message_id, bounds[1] - top, top
        return None, 0, top

    def _draw(self, scroll_to_end=False, anchor=None):
        width = self.canvas.winfo_width()
        height = max(1, self.canvas.winfo_height())
        if width <= 1:
            width = 640  # Настоящая ширина появится после первого <Configure>.
        self.canvas.delete("all")
        self._positions = {}
        self._bubble_items = {}
        y = 16
        previous_day = None
        for message_id in sorted(self._messages):
            message = self._messages[message_id]
            moment = self._local_time(message.get("timestamp", ""))
            day = moment.date() if moment else None
            if day != previous_day:
                y = self._draw_date(day, width, y)
                previous_day = day
            y = self._draw_message(message_id, message, moment, width, y) + 9

        if not self._messages:
            self.canvas.create_text(
                width / 2, height / 2, text=self._empty_text, anchor="center",
                fill=MUTED, font=self._body_font, justify="center",
                width=max(30, min(360, width - 60)), tags=("empty",),
            )
        self._content_height = max(height, y + 10)
        self.canvas.configure(scrollregion=(0, 0, width, self._content_height))
        if scroll_to_end:
            self.canvas.yview_moveto(1)
        elif anchor is not None:
            message_id, offset, old_top = anchor
            bounds = self._positions.get(message_id)
            top = bounds[1] - offset if bounds else old_top
            self.canvas.yview_moveto(max(0, top) / self._content_height)

    def _draw_date(self, day, width, y):
        if day is None:
            return y
        today = datetime.now().astimezone().date()
        if day == today:
            label = "Сегодня"
        elif day == today - timedelta(days=1):
            label = "Вчера"
        else:
            label = day.strftime("%d.%m.%Y")
        label_width = self._small_font.measure(label) + 24
        self._rounded_rectangle(
            (width - label_width) / 2, y, (width + label_width) / 2, y + 25,
            radius=12, fill="#192633", tags=("date",),
        )
        self.canvas.create_text(
            width / 2, y + 12, text=label, fill=MUTED,
            font=self._small_font, tags=("date",),
        )
        return y + 36

    def _draw_message(self, message_id, message, moment, width, y):
        own = message.get("sender", "") == self._own_name
        sender = str(message.get("sender", ""))
        body = str(message.get("text") or "")
        reply = message.get("reply")
        attachment = message.get("file")
        time_text = moment.strftime("%H:%M") if moment else ""
        file_text = ""
        if attachment:
            file_text = f"Файл: {attachment.get('name', 'Файл')} · {self._file_size(attachment.get('size', 0))}"

        margin = 20 if width >= 300 else 12
        max_width = max(28, min(520, int(width * 0.70), width - 2 * margin))
        natural_width = max(self._text_width(body, self._body_font),
                            self._text_width(file_text, self._file_font),
                            self._small_font.measure(time_text), 52)
        if not own:
            natural_width = max(natural_width, self._author_font.measure(sender))
        if reply:
            natural_width = max(natural_width,
                                self._author_font.measure(str(reply.get("sender", ""))) + 10,
                                self._small_font.measure(self._reply_preview(reply)) + 10)
        bubble_width = min(max_width, natural_width + 24)
        content_width = max(4, bubble_width - 24)
        x = width - margin - bubble_width if own else margin
        tags = ("message", f"message:{message_id}")
        cursor_y = y + 9
        color = AUTHOR_COLORS[sum(ord(letter) for letter in sender) % len(AUTHOR_COLORS)]

        # У своих сообщений имя не повторяется: направление уже видно по стороне.
        if not own:
            cursor_y = self._draw_text(
                x + 12, cursor_y, self._elide(sender, self._author_font, content_width),
                self._author_font, color, content_width, tags + ("sender",),
            ) + 4

        if reply:
            quote_y = cursor_y
            quote_width = max(4, content_width - 10)
            cursor_y = self._draw_text(
                x + 22, cursor_y,
                self._elide(str(reply.get("sender", "")), self._author_font, quote_width),
                self._author_font, ACCENT, quote_width, tags + ("reply",),
            ) + 1
            cursor_y = self._draw_text(
                x + 22, cursor_y,
                self._elide(self._reply_preview(reply), self._small_font, quote_width),
                self._small_font, MUTED, quote_width, tags + ("reply",),
            )
            self.canvas.create_line(
                x + 13, quote_y + 1, x + 13, cursor_y - 1,
                fill=ACCENT, width=2, tags=tags + ("reply",),
            )
            cursor_y += 7

        if body:
            cursor_y = self._draw_text(
                x + 12, cursor_y, body, self._body_font, TEXT,
                content_width, tags + ("body",),
            ) + 2
        if file_text:
            cursor_y = self._draw_text(
                x + 12, cursor_y, file_text, self._file_font, "#9edcf5",
                content_width, tags + ("attachment",),
            ) + 2
        if not body and not file_text:
            cursor_y = self._draw_text(
                x + 12, cursor_y, "Сообщение", self._body_font, MUTED,
                content_width, tags + ("body",),
            ) + 2

        time_item = self.canvas.create_text(
            x + bubble_width - 12, cursor_y + 1, text=time_text, anchor="ne",
            fill=MUTED, font=self._small_font, tags=tags + ("time",),
        )
        time_bounds = self.canvas.bbox(time_item)
        bottom = (time_bounds[3] if time_bounds else cursor_y + 12) + 7
        selected = message_id == self._selected
        bubble = self._rounded_rectangle(
            x, y, x + bubble_width, bottom, radius=12,
            fill=OWN_BUBBLE if own else OTHER_BUBBLE,
            outline=ACCENT if selected else "", width=2 if selected else 0,
            tags=tags + ("bubble", "own" if own else "other"),
        )
        self.canvas.tag_lower(bubble)
        self._positions[message_id] = (x, y, x + bubble_width, bottom)
        self._bubble_items[message_id] = bubble
        return bottom

    def _draw_text(self, x, y, text, font, color, width, tags):
        item = self.canvas.create_text(
            x, y, text=text, anchor="nw", fill=color, font=font,
            width=width, justify="left", tags=tags,
        )
        bounds = self.canvas.bbox(item)
        return bounds[3] if bounds else y

    def _rounded_rectangle(self, x1, y1, x2, y2, radius=12, **options):
        # Повторные точки оставляют прямые стороны и сглаживают только углы.
        radius = min(radius, (x2 - x1) / 2, (y2 - y1) / 2)
        points = (
            x1 + radius, y1, x2 - radius, y1, x2, y1, x2, y1 + radius,
            x2, y2 - radius, x2, y2, x2 - radius, y2, x1 + radius, y2,
            x1, y2, x1, y2 - radius, x1, y1 + radius, x1, y1,
        )
        return self.canvas.create_polygon(points, smooth=True, splinesteps=16, **options)

    @staticmethod
    def _text_width(text, font):
        return max((font.measure(line) for line in text.split("\n")), default=0)

    @staticmethod
    def _elide(text, font, width):
        text = " ".join(text.split())
        if font.measure(text) <= width:
            return text
        while text and font.measure(text + "…") > width:
            text = text[:-1]
        return text + "…"

    @staticmethod
    def _reply_preview(reply):
        return str(reply.get("text") or reply.get("file_name") or "Сообщение")

    @staticmethod
    def _file_size(size):
        try:
            value = max(0, int(size))
        except (TypeError, ValueError):
            value = 0
        for unit in ("Б", "КБ", "МБ", "ГБ"):
            if value < 1024 or unit == "ГБ":
                return f"{value:g} {unit}" if value < 10 else f"{value:.0f} {unit}"
            value /= 1024

    @staticmethod
    def _local_time(timestamp):
        try:
            return datetime.fromisoformat(str(timestamp).replace("Z", "+00:00")).astimezone()
        except (ValueError, TypeError, OverflowError):
            return None
