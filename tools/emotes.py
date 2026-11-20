"""The emotes over heads (heart, star, exclaim, note, happy, and a plain bubble): each a whole white speech bubble
with its picture in it, smooth (drawn at four times the size and scaled down), 128 by 128 with the tail's tip at
the middle of the bottom. Written to textures/gui/emote/, each with a .mcmeta so the game smooths it when drawn.

    python3 tools/emotes.py
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "gui", "emote")
K = 4
N = 128 * K
CX, CY, R = 64 * K, 56 * K, 47 * K


def bubble_mask():
    m = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(m)
    d.ellipse((CX - R, CY - R, CX + R, CY + R), fill=255)
    # The tail: a rounded point under the middle.
    d.polygon([(CX - 15 * K, CY + R - 12 * K), (CX + 15 * K, CY + R - 12 * K), (CX, 124 * K)], fill=255)
    return m.filter(ImageFilter.GaussianBlur(1.2 * K)).point(lambda v: 255 if v > 128 else 0)


def heart(d):
    c = (240, 80, 106)
    # The classic heart curve, 31 across at this size.
    pts = []
    for i in range(240):
        t = i / 240 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((CX + x * 2.05 * K, CY + 3 * K - y * 2.05 * K))
    d.polygon(pts, fill=c)
    d.ellipse((CX - 22 * K, CY - 15 * K, CX - 11 * K, CY - 6 * K), fill=(255, 190, 202))


def star(d):
    c, edge = (255, 201, 60), (232, 150, 24)
    pts = []
    for i in range(10):
        a = -math.pi / 2 + i * math.pi / 5
        r = (34 if i % 2 == 0 else 15) * K
        pts.append((CX + r * math.cos(a), CY + 3 * K + r * math.sin(a)))
    d.polygon(pts, fill=edge)
    inner = [(CX + (px - CX) * 0.8, CY + 3 * K + (py - CY - 3 * K) * 0.8) for px, py in pts]
    d.polygon(inner, fill=c)


def exclaim(d):
    c = (229, 82, 92)
    w = 15 * K
    d.rounded_rectangle((CX - w // 2, CY - 32 * K, CX + w // 2, CY + 12 * K), radius=w // 2, fill=c)
    d.ellipse((CX - 9 * K, CY + 19 * K, CX + 9 * K, CY + 37 * K), fill=c)


def note(d):
    c = (74, 143, 224)
    d.ellipse((CX - 30 * K, CY + 6 * K, CX - 4 * K, CY + 28 * K), fill=c)
    d.ellipse((CX + 4 * K, CY + 0 * K, CX + 30 * K, CY + 22 * K), fill=c)
    d.rectangle((CX - 11 * K, CY - 28 * K, CX - 4 * K, CY + 18 * K), fill=c)
    d.rectangle((CX + 23 * K, CY - 34 * K, CX + 30 * K, CY + 12 * K), fill=c)
    d.polygon([(CX - 11 * K, CY - 28 * K), (CX + 30 * K, CY - 34 * K), (CX + 30 * K, CY - 22 * K), (CX - 11 * K, CY - 16 * K)], fill=c)


def happy(d):
    c, ink = (255, 201, 60), (38, 48, 74)
    r = 33 * K
    d.ellipse((CX - r, CY - r, CX + r, CY + r), fill=(232, 150, 24))
    r2 = r - 4 * K
    d.ellipse((CX - r2, CY - r2, CX + r2, CY + r2), fill=c)
    for ex in (-12, 12):
        d.ellipse((CX + ex * K - 5 * K, CY - 15 * K, CX + ex * K + 5 * K, CY - 1 * K), fill=ink)
    d.arc((CX - 19 * K, CY - 12 * K, CX + 19 * K, CY + 20 * K), 20, 160, fill=ink, width=6 * K)


def plain(d):
    for ex in (-22, 0, 22):
        d.ellipse((CX + ex * K - 7 * K, CY - 7 * K, CX + ex * K + 7 * K, CY + 7 * K), fill=(169, 180, 212))


DRAW = {"heart": heart, "star": star, "exclaim": exclaim, "note": note, "happy": happy, "dots": plain}


def make(name):
    mask = bubble_mask()
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    # A soft shadow just under it, then a pale blue rim, then the white.
    sh = mask.filter(ImageFilter.GaussianBlur(3 * K)).point(lambda v: v * 70 // 255)
    img.paste((20, 30, 60, 255), (0, 3 * K), sh)
    rim = Image.new("RGBA", (N, N), (196, 214, 234, 255))
    img.paste(rim, (0, 0), mask)
    inner = mask.filter(ImageFilter.MinFilter(4 * K + 1))
    img.paste(Image.new("RGBA", (N, N), (255, 255, 255, 255)), (0, 0), inner)
    pic = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    DRAW[name](ImageDraw.Draw(pic))
    img.alpha_composite(pic)
    return img.resize((128, 128), Image.LANCZOS)


def ballmark():
    """The Poké Ball mark at the end of each bar in a list (textures/gui/v/icon_ballmark.png): white, to be tinted."""
    m = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(m)
    c, r, t = 64 * K, 54 * K, 11 * K
    d.ellipse((c - r, c - r, c + r, c + r), fill=255)
    d.ellipse((c - r + t, c - r + t, c + r - t, c + r - t), fill=0)
    d.rectangle((c - r, c - t // 2, c + r, c + t // 2), fill=255)
    d.ellipse((c - 21 * K, c - 21 * K, c + 21 * K, c + 21 * K), fill=0)
    d.ellipse((c - 17 * K, c - 17 * K, c + 17 * K, c + 17 * K), fill=255)
    d.ellipse((c - 7 * K, c - 7 * K, c + 7 * K, c + 7 * K), fill=0)
    img = Image.new("RGBA", (N, N), (255, 255, 255, 0))
    img.putalpha(m)
    out = os.path.join(OUT, "..", "v", "icon_ballmark.png")
    img.resize((128, 128), Image.LANCZOS).save(out)
    with open(out + ".mcmeta", "w") as f:
        json.dump({"texture": {"blur": True, "clamp": True}}, f)


def sheen():
    """The light and shade laid over a page's own colour (textures/gui/v/bg_sheen.png): the UI Viewer's
    linear-gradient(160deg, 62% white, the colour at 55%, 38% black) over any colour."""
    w, h, W, H = 480, 270, 1280.0, 720.0
    a = math.radians(160)
    dx, dy = math.sin(a), -math.cos(a)
    length = abs(W * dx) + abs(H * dy)
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            X, Y = (x + 0.5) / w * W - W / 2, (y + 0.5) / h * H - H / 2
            t = (X * dx + Y * dy) / length + 0.5
            if t < 0.55:
                px[x, y] = (255, 255, 255, round(255 * 0.62 * (1 - t / 0.55)))
            else:
                px[x, y] = (0, 0, 0, round(255 * 0.38 * min(1, (t - 0.55) / 0.45)))
    out = os.path.join(OUT, "..", "v", "bg_sheen.png")
    img.save(out)
    with open(out + ".mcmeta", "w") as f:
        json.dump({"texture": {"blur": True, "clamp": True}}, f)


def main():
    os.makedirs(OUT, exist_ok=True)
    ballmark()
    sheen()
    for name in DRAW:
        make(name).save(os.path.join(OUT, name + ".png"))
        with open(os.path.join(OUT, name + ".png.mcmeta"), "w") as f:
            json.dump({"texture": {"blur": True, "clamp": True}}, f)
        print("wrote", name)


if __name__ == "__main__":
    main()
