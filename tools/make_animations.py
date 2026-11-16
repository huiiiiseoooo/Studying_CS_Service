"""스택·큐 학습 애니메이션(GIF)과 대체 이미지(PNG)를 만든다.

실행:  python3 tools/make_animations.py      (Pillow 필요: pip install pillow)
결과:  media/animations/*.gif, media/fallback/*.png

색은 프론트엔드 디자인 토큰(frontend/src/styles/tokens.css)과 맞춘다.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT_GIF = ROOT / "media" / "animations"
OUT_PNG = ROOT / "media" / "fallback"

W, H = 800, 450
S = 2  # 2배로 그린 뒤 줄여서 가장자리를 부드럽게 한다.

BG = "#FFFFFF"
INK = "#1B1D2A"
MUTED = "#6C7083"
LINE = "#D9DCE8"
CELL = "#F3F4FA"
ACCENT = "#3D4EDB"
ACCENT_SOFT = "#E4E7FB"
MOVING = "#F2A33A"
MOVING_INK = "#3A2606"

STEP_MS = 40  # 움직이는 구간의 프레임 간격


def _font(candidates, size):
    for path, index in candidates:
        try:
            return ImageFont.truetype(path, size * S, index=index)
        except OSError:
            continue
    return ImageFont.load_default(size * S)


MONO = [("/System/Library/Fonts/Menlo.ttc", 1), ("/System/Library/Fonts/Menlo.ttc", 0),
        ("C:/Windows/Fonts/consolab.ttf", 0), ("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf", 0)]
KOREAN = [("/System/Library/Fonts/AppleSDGothicNeo.ttc", 0), ("C:/Windows/Fonts/malgun.ttf", 0),
          ("/usr/share/fonts/truetype/nanum/NanumGothic.ttf", 0)]

F_OP = _font(MONO, 30)
F_VALUE = _font(MONO, 24)
F_LABEL = _font(MONO, 16)
F_TEXT = _font(KOREAN, 19)


def ease(t):
    return t * t * (3 - 2 * t)


def lerp(a, b, t):
    return a + (b - a) * t


class Canvas:
    def __init__(self):
        self.img = Image.new("RGB", (W * S, H * S), BG)
        self.d = ImageDraw.Draw(self.img)

    def rect(self, x, y, w, h, fill=None, outline=None, width=2, radius=10):
        self.d.rounded_rectangle([x * S, y * S, (x + w) * S, (y + h) * S], radius=radius * S,
                                 fill=fill, outline=outline, width=width * S)

    def line(self, points, fill=INK, width=3):
        self.d.line([(x * S, y * S) for x, y in points], fill=fill, width=width * S, joint="curve")

    def text(self, x, y, s, font, fill=INK, anchor="la"):
        self.d.text((x * S, y * S), s, font=font, fill=fill, anchor=anchor)

    def triangle(self, cx, cy, size, direction, fill):
        s = size
        pts = {
            "right": [(cx - s, cy - s), (cx + s, cy), (cx - s, cy + s)],
            "down": [(cx - s, cy - s), (cx + s, cy - s), (cx, cy + s)],
            "up": [(cx - s, cy + s), (cx + s, cy + s), (cx, cy - s)],
        }[direction]
        self.d.polygon([(x * S, y * S) for x, y in pts], fill=fill)

    def block(self, cx, cy, w, h, value, moving=False):
        fill, ink = (MOVING, MOVING_INK) if moving else (ACCENT, "#FFFFFF")
        self.rect(cx - w / 2, cy - h / 2, w, h, fill=fill, radius=9)
        self.text(cx, cy, str(value), F_VALUE, fill=ink, anchor="mm")

    def header(self, op, note):
        self.text(40, 34, op, F_OP, fill=ACCENT)
        self.text(40, 78, note, F_TEXT, fill=MUTED)

    def footer(self, s):
        if s:
            self.text(W / 2, H - 36, s, F_TEXT, fill=INK, anchor="mm")

    def done(self):
        return self.img.resize((W, H), Image.LANCZOS)


class Timeline:
    """프레임과 프레임별 표시 시간을 모은다."""

    def __init__(self):
        self.frames = []
        self.durations = []
        self.poster = None

    def hold(self, draw, ms, poster=False):
        frame = draw()
        self.frames.append(frame)
        self.durations.append(ms)
        if poster:
            self.poster = frame

    def move(self, draw_at, steps=14):
        for i in range(1, steps + 1):
            self.frames.append(draw_at(ease(i / steps)))
            self.durations.append(STEP_MS)

    def save(self, name):
        OUT_GIF.mkdir(parents=True, exist_ok=True)
        OUT_PNG.mkdir(parents=True, exist_ok=True)
        # 모든 프레임이 같은 색상표를 쓰도록 맞춘다(프레임 사이 색 흔들림 방지, 용량 감소).
        sample = Image.new("RGB", (W, H * 2))
        sample.paste(self.frames[len(self.frames) // 2], (0, 0))
        sample.paste(self.poster or self.frames[-1], (0, H))
        palette = sample.quantize(colors=64, method=Image.MEDIANCUT)
        paletted = [f.quantize(palette=palette, dither=Image.NONE) for f in self.frames]
        paletted[0].save(OUT_GIF / f"{name}.gif", save_all=True, append_images=paletted[1:],
                         duration=self.durations, loop=0, optimize=False, disposal=1)
        (self.poster or self.frames[-1]).save(OUT_PNG / f"{name}.png", optimize=True)
        size_kb = (OUT_GIF / f"{name}.gif").stat().st_size / 1024
        seconds = sum(self.durations) / 1000
        print(f"{name}: {len(self.frames)} frames, {seconds:.1f}s, {size_kb:.0f} KB")


# ---------------------------------------------------------------- 스택

ST_CX = 400          # 스택 통의 가로 중심
ST_BLOCK_W, ST_BLOCK_H = 150, 50
ST_GAP = 8
ST_BOTTOM = 372      # 통 바닥 y
ST_CAPACITY = 4
ST_ENTRY_Y = 118     # 통 위, 블록이 대기하는 높이


def stack_slot_y(i):
    return ST_BOTTOM - ST_GAP - ST_BLOCK_H / 2 - i * (ST_BLOCK_H + ST_GAP)


def draw_stack(op, note, values, top_y=None, moving=None, footer=""):
    """values: 통 안의 값(아래→위), top_y: top 화살표 y(None이면 숨김), moving: (값, cx, cy)"""
    c = Canvas()
    c.header(op, note)
    half = ST_BLOCK_W / 2 + 12
    top = stack_slot_y(ST_CAPACITY - 1) - ST_BLOCK_H / 2 - 10
    c.rect(ST_CX - half, top, half * 2, ST_BOTTOM - top, fill=CELL, radius=4)
    c.line([(ST_CX - half, top), (ST_CX - half, ST_BOTTOM), (ST_CX + half, ST_BOTTOM), (ST_CX + half, top)])
    for i, v in enumerate(values):
        c.block(ST_CX, stack_slot_y(i), ST_BLOCK_W, ST_BLOCK_H, v)
    if top_y is not None:
        x = ST_CX - half - 22
        c.triangle(x, top_y, 8, "right", ACCENT)
        c.line([(x - 44, top_y), (x - 6, top_y)], fill=ACCENT, width=3)
        c.text(x - 54, top_y, "top", F_LABEL, fill=ACCENT, anchor="rm")
    if moving:
        v, cx, cy = moving
        c.block(cx, cy, ST_BLOCK_W, ST_BLOCK_H, v, moving=True)
    c.footer(footer)
    return c.done()


def make_stack_push():
    t = Timeline()
    values = []
    t.hold(lambda: draw_stack("stack", "빈 스택에서 시작합니다", [], footer="넣는 곳과 꺼내는 곳이 모두 맨 위입니다"), 1200)
    for v in (10, 20, 30):
        i = len(values)
        old_top = stack_slot_y(i - 1) if i else None
        op, note = f"push({v})", f"{v}을 맨 위에 올립니다"
        t.hold(lambda: draw_stack(op, note, values, old_top, (v, ST_CX, ST_ENTRY_Y)), 600)
        t.move(lambda k: draw_stack(op, note, values, old_top, (v, ST_CX, lerp(ST_ENTRY_Y, stack_slot_y(i), k))))
        values = values + [v]
        if old_top is not None:
            t.move(lambda k: draw_stack(op, note, values, lerp(old_top, stack_slot_y(i), k)), steps=6)
        t.hold(lambda: draw_stack(op, f"top 이 {v}을 가리킵니다", values, stack_slot_y(i)), 1000, poster=(v == 30))
    t.hold(lambda: draw_stack("push 완료", "가장 나중에 넣은 30 이 맨 위에 있습니다", values, stack_slot_y(2),
                              footer="넣은 순서  10 → 20 → 30"), 2400)
    t.save("stack-push")


def make_stack_pop():
    t = Timeline()
    values = [10, 20, 30]
    popped = []
    out_x, out_y = 640, ST_ENTRY_Y

    def foot():
        return "꺼낸 순서  " + " → ".join(map(str, popped)) if popped else ""

    t.hold(lambda: draw_stack("stack", "10, 20, 30 순서로 쌓인 스택입니다", values, stack_slot_y(2)), 1400)
    while values:
        v = values[-1]
        i = len(values) - 1
        rest = values[:-1]
        op, note = "pop()", f"맨 위의 {v}을 꺼냅니다"
        y0 = stack_slot_y(i)
        t.hold(lambda: draw_stack(op, note, rest, y0, (v, ST_CX, y0), foot()), 500)
        t.move(lambda k: draw_stack(op, note, rest, y0, (v, ST_CX, lerp(y0, ST_ENTRY_Y, k)), foot()), steps=12)
        t.move(lambda k: draw_stack(op, note, rest, y0, (v, lerp(ST_CX, out_x, k), out_y), foot()), steps=10)
        popped.append(v)
        new_top = stack_slot_y(i - 1) if rest else None
        if new_top is not None:
            t.move(lambda k: draw_stack(f"pop() → {v}", note, rest, lerp(y0, new_top, k), (v, out_x, out_y), foot()),
                   steps=6)
        after = f"top 이 {rest[-1]}으로 내려갑니다" if rest else "스택이 비었습니다"
        t.hold(lambda: draw_stack(f"pop() → {v}", after, rest, new_top, (v, out_x, out_y), foot()), 1000,
               poster=(v == 30))
        values = rest
    t.hold(lambda: draw_stack("pop 완료", "넣은 순서의 반대로 나왔습니다 (LIFO)", [], None, None, foot()), 2400)
    t.save("stack-pop")


# ---------------------------------------------------------------- 큐

Q_CELLS = 5
Q_CELL_W, Q_CELL_H = 96, 72
Q_X0 = (W - Q_CELLS * Q_CELL_W) / 2
Q_Y = 196
Q_BLOCK_W, Q_BLOCK_H = 78, 54
Q_ENTER_X = W - 62   # 오른쪽 바깥(들어오는 곳)
Q_EXIT_X = 62        # 왼쪽 바깥(나가는 곳)


def q_cell_cx(i):
    return Q_X0 + Q_CELL_W * (i + 0.5)


Q_CY = Q_Y + Q_CELL_H / 2


def draw_queue(op, note, cells, front, rear, moving=None, footer=""):
    """cells: {칸 번호: 값}, front/rear: 표시 위치(칸 번호, 소수 가능), moving: (값, cx)"""
    c = Canvas()
    c.header(op, note)
    for i in range(Q_CELLS):
        c.rect(Q_X0 + i * Q_CELL_W, Q_Y, Q_CELL_W, Q_CELL_H, fill=CELL, outline=LINE, width=2, radius=0)
    c.line([(Q_X0, Q_Y), (Q_X0 + Q_CELLS * Q_CELL_W, Q_Y)])
    c.line([(Q_X0, Q_Y + Q_CELL_H), (Q_X0 + Q_CELLS * Q_CELL_W, Q_Y + Q_CELL_H)])
    for i, v in cells.items():
        c.block(q_cell_cx(i), Q_CY, Q_BLOCK_W, Q_BLOCK_H, v)
    # front: 위쪽에서 아래를 가리킨다 / rear: 아래쪽에서 위를 가리킨다
    fx = q_cell_cx(front)
    c.text(fx, Q_Y - 40, "front", F_LABEL, fill=ACCENT, anchor="mm")
    c.triangle(fx, Q_Y - 16, 8, "down", ACCENT)
    rx = q_cell_cx(rear)
    c.triangle(rx, Q_Y + Q_CELL_H + 16, 8, "up", MUTED)
    c.text(rx, Q_Y + Q_CELL_H + 40, "rear", F_LABEL, fill=MUTED, anchor="mm")
    if moving:
        v, cx = moving
        c.block(cx, Q_CY, Q_BLOCK_W, Q_BLOCK_H, v, moving=True)
    c.footer(footer)
    return c.done()


def make_queue_enqueue():
    t = Timeline()
    cells = {}
    t.hold(lambda: draw_queue("queue", "빈 큐에서 시작합니다", cells, 0, 0,
                              footer="뒤(rear)로 들어가고 앞(front)으로 나옵니다"), 1200)
    for v in (10, 20, 30):
        i = len(cells)
        op, note = f"enqueue({v})", f"{v}을 맨 뒤에 넣습니다"
        t.hold(lambda: draw_queue(op, note, cells, 0, i, (v, Q_ENTER_X)), 600)
        t.move(lambda k: draw_queue(op, note, cells, 0, i, (v, lerp(Q_ENTER_X, q_cell_cx(i), k))), steps=16)
        cells = {**cells, i: v}
        t.move(lambda k: draw_queue(op, note, cells, 0, lerp(i, i + 1, k)), steps=6)
        t.hold(lambda: draw_queue(op, "rear 가 다음 빈 칸으로 이동합니다", cells, 0, i + 1), 1000, poster=(v == 30))
    t.hold(lambda: draw_queue("enqueue 완료", "먼저 넣은 10 이 맨 앞에 있습니다", cells, 0, 3,
                              footer="넣은 순서  10 → 20 → 30"), 2400)
    t.save("queue-enqueue")


def make_queue_dequeue():
    t = Timeline()
    cells = {0: 10, 1: 20, 2: 30}
    out = []

    def foot():
        return "꺼낸 순서  " + " → ".join(map(str, out)) if out else ""

    t.hold(lambda: draw_queue("queue", "10, 20, 30 순서로 들어온 큐입니다", cells, 0, 3), 1400)
    for i in range(3):
        v = cells[i]
        rest = {k: x for k, x in cells.items() if k != i}
        op, note = "dequeue()", f"맨 앞의 {v}을 꺼냅니다"
        t.hold(lambda: draw_queue(op, note, rest, i, 3, (v, q_cell_cx(i)), foot()), 500)
        t.move(lambda k: draw_queue(op, note, rest, i, 3, (v, lerp(q_cell_cx(i), Q_EXIT_X, k)), foot()), steps=16)
        out.append(v)
        t.move(lambda k: draw_queue(f"dequeue() → {v}", note, rest, lerp(i, i + 1, k), 3, (v, Q_EXIT_X), foot()),
               steps=6)
        after = "front 가 다음 칸으로 이동합니다" if rest else "큐가 비었습니다 (front = rear)"
        t.hold(lambda: draw_queue(f"dequeue() → {v}", after, rest, i + 1, 3, (v, Q_EXIT_X), foot()), 1000,
               poster=(i == 0))
        cells = rest
    t.hold(lambda: draw_queue("dequeue 완료", "넣은 순서 그대로 나왔습니다 (FIFO)", {}, 3, 3, None, foot()), 2400)
    t.save("queue-dequeue")


if __name__ == "__main__":
    make_stack_push()
    make_stack_pop()
    make_queue_enqueue()
    make_queue_dequeue()
