import math
import random
import tkinter as tk
from tkinter import font as tkfont

BG_TOP = "#241326"
BG_BOTTOM = "#74304f"
CARD = "#fff8fb"
INK = "#49263a"
MUTED = "#9a7187"
ROSE = "#d6537b"
ROSE_DARK = "#b83f68"
PALE = "#fbe5ed"

LETTER = (
    "Мне хотелось подарить тебе не просто открытку, а маленький момент, "
    "к которому можно возвращаться в любой день.\n\n"
    "Спасибо за твою улыбку, за разговоры обо всём и ни о чём, за твою "
    "искренность и за то тепло, которое появляется рядом с тобой. "
    "С тобой даже самые обычные моменты становятся особенными.\n\n"
    "Я очень ценю тебя. И надеюсь, что впереди у нас будет ещё много "
    "поводов смеяться, мечтать и создавать наши маленькие счастливые воспоминания.\n\n"
    "Пусть сегодня ты хотя бы на секунду посмотришь на себя моими глазами "
    "и увидишь, насколько ты прекрасна.  ♥"
)

COMPLIMENTS = [
    "Твоя улыбка умеет делать мой день лучше. Даже если день был так себе.",
    "Мне нравится не только то, какая ты, но и то, каким я становлюсь рядом с тобой.",
    "Ты — та самая приятная мысль, которая внезапно появляется посреди обычного дня.",
    "С тобой хочется делиться даже самыми маленькими новостями.",
    "У тебя удивительный талант: быть особенной, даже ничего для этого не делая.",
    "Мне повезло, что однажды наши дороги пересеклись.",
    "Напоминание на сегодня: ты очень важна. И очень-очень дорога мне."
]


def mix_color(a, b, t):
    a = tuple(int(a[i:i + 2], 16) for i in (1, 3, 5))
    b = tuple(int(b[i:i + 2], 16) for i in (1, 3, 5))
    return "#" + "".join(f"{round(a[i] + (b[i] - a[i]) * t):02x}" for i in range(3))


class LoveLetterApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Маленькое письмо для тебя")
        self.root.geometry("980x760")
        self.root.minsize(820, 660)
        self.root.configure(bg=BG_TOP)

        self.bg = tk.Canvas(root, highlightthickness=0, bd=0)
        self.bg.place(x=0, y=0, relwidth=1, relheight=1)
        self.hearts = []
        self._draw_background()
        self.root.bind("<Configure>", self._resize_background)

        self.card = tk.Frame(root, bg=CARD, highlightthickness=1,
                             highlightbackground="#f6d5e1")
        self.card.place(relx=0.5, rely=0.5, anchor="center",
                        relwidth=0.82, relheight=0.78)

        self.name_var = tk.StringVar(value="Моя хорошая")
        self.from_var = tk.StringVar(value="Твой человек")
        self.compliment_index = 0
        self.screen = "home"
        self.build_home()
        self.animate_hearts()

    def _draw_background(self):
        self.bg.delete("gradient")
        width = max(self.root.winfo_width(), 1)
        height = max(self.root.winfo_height(), 1)
        bands = 90
        for i in range(bands):
            y1 = int(height * i / bands)
            y2 = int(height * (i + 1) / bands) + 1
            color = mix_color(BG_TOP, BG_BOTTOM, i / max(bands - 1, 1))
            self.bg.create_rectangle(0, y1, width, y2, fill=color,
                                     outline=color, tags="gradient")
        self.bg.tag_lower("gradient")

        if not self.hearts:
            palette = ["#d77b9a", "#e9a6bb", "#f5c4d3", "#ba6a8b"]
            for _ in range(26):
                x = random.randint(0, width)
                y = random.randint(0, height)
                size = random.randint(10, 22)
                speed = random.uniform(0.35, 1.15)
                item = self.bg.create_text(
                    x, y, text=random.choice(["♥", "♡"]),
                    fill=random.choice(palette),
                    font=("Segoe UI", size), tags="particle"
                )
                self.hearts.append({
                    "id": item, "speed": speed,
                    "drift": random.uniform(-0.35, 0.35),
                    "phase": random.random() * math.tau
                })

    def _resize_background(self, _event=None):
        self._draw_background()

    def animate_hearts(self):
        height = max(self.root.winfo_height(), 1)
        width = max(self.root.winfo_width(), 1)
        for heart in self.hearts:
            item = heart["id"]
            x, y = self.bg.coords(item)
            heart["phase"] += 0.035
            x += heart["drift"] + math.sin(heart["phase"]) * 0.22
            y -= heart["speed"]
            if y < -30:
                y = height + random.randint(10, 100)
                x = random.randint(0, width)
            if x < -20:
                x = width + 10
            elif x > width + 20:
                x = -10
            self.bg.coords(item, x, y)
        self.root.after(45, self.animate_hearts)

    def clear_card(self):
        for child in self.card.winfo_children():
            child.destroy()

    def make_label(self, parent, text, size, color=INK, weight="normal",
                   wrap=620, justify="center"):
        return tk.Label(
            parent, text=text, bg=CARD, fg=color,
            font=("Segoe UI", size, weight), wraplength=wrap,
            justify=justify, anchor="center"
        )

    def make_button(self, parent, text, command, primary=True):
        return tk.Button(
            parent, text=text, command=command,
            font=("Segoe UI", 12, "bold" if primary else "normal"),
            bg=ROSE if primary else PALE,
            fg="white" if primary else ROSE_DARK,
            activebackground=ROSE_DARK if primary else "#f4d4e0",
            activeforeground="white" if primary else ROSE_DARK,
            relief="flat", bd=0, cursor="hand2",
            padx=24, pady=13, highlightthickness=0
        )

    def build_home(self):
        self.screen = "home"
        self.clear_card()
        body = tk.Frame(self.card, bg=CARD)
        body.pack(fill="both", expand=True, padx=46, pady=32)

        self.make_label(body, "✦   ♥   ✦", 22, ROSE).pack(pady=(5, 12))
        self.make_label(body, "маленькое письмо", 29, INK, "bold").pack()
        self.make_label(
            body, "для человека, который делает мир чуточку теплее",
            12, MUTED
        ).pack(pady=(8, 26))

        fields = tk.Frame(body, bg=CARD)
        fields.pack(fill="x", padx=70)
        self.make_label(fields, "Это письмо для", 11, MUTED).pack(anchor="w")
        self._make_entry(fields, self.name_var).pack(fill="x", pady=(5, 16))
        self.make_label(fields, "Подпись", 11, MUTED).pack(anchor="w")
        self._make_entry(fields, self.from_var).pack(fill="x", pady=(5, 18))

        self.make_label(
            body, "Небольшой сюрприз. Никакой спешки — просто открой.",
            11, MUTED
        ).pack(pady=(4, 20))
        self.make_button(body, "Открыть письмо   ♥", self.open_letter).pack()
        self.make_label(body, "сделано с теплом и одной очень важной мыслью",
                        9, "#b896a8").pack(side="bottom", pady=(20, 0))

    def _make_entry(self, parent, variable):
        return tk.Entry(
            parent, textvariable=variable, font=("Segoe UI", 12),
            bg="#fff0f5", fg=INK, insertbackground=ROSE,
            relief="flat", bd=0, highlightthickness=1,
            highlightbackground="#f1d3df", highlightcolor=ROSE
        )

    def open_letter(self):
        recipient = self.name_var.get().strip() or "Моя хорошая"
        sender = self.from_var.get().strip() or "Твой человек"
        self.screen = "letter"
        self.clear_card()

        body = tk.Frame(self.card, bg=CARD)
        body.pack(fill="both", expand=True, padx=52, pady=27)

        self.make_label(body, "♥", 27, ROSE).pack(pady=(0, 2))
        self.make_label(body, f"Для тебя, {recipient}!", 24, INK, "bold").pack()
        self.make_label(body, "одно маленькое напоминание", 10, MUTED).pack(pady=(4, 18))

        letter_frame = tk.Frame(body, bg="#fff0f5",
                                highlightthickness=1,
                                highlightbackground="#f4d9e3")
        letter_frame.pack(fill="both", expand=True, padx=12, pady=(0, 14))
        letter = tk.Label(
            letter_frame, text=LETTER, bg="#fff0f5", fg="#59374a",
            font=("Segoe UI", 12), wraplength=610, justify="center",
            padx=26, pady=18
        )
        letter.pack(fill="both", expand=True)

        self.compliment = tk.Label(
            body, text="А вот ещё кое-что для твоей улыбки ♥",
            bg=CARD, fg=ROSE_DARK, font=("Segoe UI", 11, "italic"),
            wraplength=610, justify="center"
        )
        self.compliment.pack(pady=(0, 10))

        buttons = tk.Frame(body, bg=CARD)
        buttons.pack()
        self.make_button(buttons, "Ещё одна причина улыбнуться",
                         self.next_compliment).pack(side="left", padx=5)
        self.make_button(buttons, "Вернуться", self.build_home,
                         primary=False).pack(side="left", padx=5)
        self.make_label(body, f"С нежностью, {sender}  ♥", 11, MUTED).pack(pady=(12, 0))

    def next_compliment(self):
        if self.screen != "letter":
            return
        message = COMPLIMENTS[self.compliment_index % len(COMPLIMENTS)]
        self.compliment_index += 1
        self.compliment.configure(text=message)

        # A tiny burst of hearts makes every click feel like a new surprise.
        width = max(self.root.winfo_width(), 1)
        for _ in range(8):
            x = random.randint(width // 4, width * 3 // 4)
            y = random.randint(40, max(self.root.winfo_height() // 3, 50))
            item = self.bg.create_text(
                x, y, text="♥", fill=random.choice(["#ffd5e2", "#f4a8c1", "#fff0f5"]),
                font=("Segoe UI", random.randint(14, 23)), tags="burst"
            )
            self.root.after(900, lambda item=item: self.bg.delete(item))


if __name__ == "__main__":
    window = tk.Tk()
    app = LoveLetterApp(window)
    window.mainloop()
