"""The battle screen's pictures, in Let's Go's style: the round command buttons (FIGHT, POKéMON, BAG, RUN), the
name plates for your Pokémon and theirs, the party's Poké Balls (full, down, empty) and the white pointer.
Drawn large and smooth (four times over, then scaled down) into textures/gui/battle/."""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/retromon/textures/gui/battle")
os.makedirs(OUT, exist_ok=True)
SS = 4


def rgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def button(name, base, deep, size=256):
    """A round button: a white rim, the colour deepening to the bottom, a big faint Poké Ball drawn into it, a
    gloss across the top."""
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    c = S / 2
    rim = S * 0.045
    # A soft shadow under it.
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse([S * 0.04, S * 0.07, S * 0.96, S * 0.99], fill=(0, 0, 0, 110))
    sh = sh.filter(ImageFilter.GaussianBlur(S * 0.02))
    im.alpha_composite(sh)
    d = ImageDraw.Draw(im)
    d.ellipse([S * 0.03, S * 0.03, S * 0.97, S * 0.97], fill=(255, 255, 255, 255))
    # The colour, top to bottom.
    fill = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fill)
    for y in range(S):
        t = y / S
        fd.line([(0, y), (S, y)], fill=mix(base, deep, t ** 1.2) + (255,))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse([S * 0.03 + rim, S * 0.03 + rim, S * 0.97 - rim, S * 0.97 - rim], fill=255)
    im.paste(fill, (0, 0), mask)
    # The Poké Ball pattern, faint, a little up and to the right.
    pat = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pd = ImageDraw.Draw(pat)
    bx, by, br = c * 1.12, c * 0.92, S * 0.42
    w = int(S * 0.035)
    pd.ellipse([bx - br, by - br, bx + br, by + br], outline=(255, 255, 255, 60), width=w)
    pd.line([(bx - br, by), (bx - br * 0.32, by)], fill=(255, 255, 255, 60), width=w)
    pd.line([(bx + br * 0.32, by), (bx + br, by)], fill=(255, 255, 255, 60), width=w)
    pd.ellipse([bx - br * 0.32, by - br * 0.32, bx + br * 0.32, by + br * 0.32], outline=(255, 255, 255, 60), width=w)
    pd.ellipse([bx - br * 0.14, by - br * 0.14, bx + br * 0.14, by + br * 0.14], fill=(255, 255, 255, 46))
    # Little sparkles.
    for (sx, sy, sr) in ((0.26, 0.3, 0.025), (0.32, 0.72, 0.018), (0.74, 0.24, 0.02)):
        pd.ellipse([S * sx - S * sr, S * sy - S * sr, S * sx + S * sr, S * sy + S * sr], fill=(255, 255, 255, 80))
    pm = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    pm.paste(pat, (0, 0), mask)
    im.alpha_composite(pm)
    # A gloss across the top.
    gl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(gl).ellipse([S * 0.16, S * 0.07, S * 0.84, S * 0.5], fill=(255, 255, 255, 70))
    gl = gl.filter(ImageFilter.GaussianBlur(S * 0.03))
    gm = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gm.paste(gl, (0, 0), mask)
    im.alpha_composite(gm)
    im.resize((size, size), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))


def plate(name, w, h):
    """A name plate: dark see-through teal, a bright mint edge, rounded, a sheen along the top."""
    W, H = w * SS, h * SS
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    r = H * 0.32
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([W * 0.01, H * 0.1, W * 0.995, H * 0.99], r, fill=(0, 0, 0, 90))
    sh = sh.filter(ImageFilter.GaussianBlur(H * 0.03))
    im.alpha_composite(sh)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, W * 0.985, H * 0.94], r, fill=rgb("8ff5c0") + (255,))
    edge = H * 0.05
    inner = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    idr = ImageDraw.Draw(inner)
    top, bottom = rgb("1d5a52"), rgb("0b2a2c")
    for y in range(H):
        idr.line([(0, y), (W, y)], fill=mix(top, bottom, y / H) + (232,))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([edge, edge, W * 0.985 - edge, H * 0.94 - edge], r - edge, fill=255)
    hole = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hole.paste(inner, (0, 0), mask)
    # The edge stays only round the outside: clear the inside first, then lay the teal in.
    clear = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    im.paste(clear, (0, 0), mask)
    im.alpha_composite(hole)
    sheen = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sheen).rounded_rectangle([edge * 2, edge * 1.6, W * 0.985 - edge * 2, H * 0.3], r * 0.6, fill=(255, 255, 255, 34))
    sm = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sm.paste(sheen, (0, 0), mask)
    im.alpha_composite(sm)
    im.resize((w, h), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))


def ball(name, top, bottom, band, size=48):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    o = S * 0.06
    d.ellipse([o, o, S - o, S - o], fill=band + (255,))
    i = o + S * 0.07
    d.pieslice([i, i, S - i, S - i], 180, 360, fill=top + (255,))
    d.pieslice([i, i, S - i, S - i], 0, 180, fill=bottom + (255,))
    d.rectangle([o, S / 2 - S * 0.045, S - o, S / 2 + S * 0.045], fill=band + (255,))
    cr = S * 0.15
    d.ellipse([S / 2 - cr, S / 2 - cr, S / 2 + cr, S / 2 + cr], fill=band + (255,))
    cr2 = S * 0.085
    d.ellipse([S / 2 - cr2, S / 2 - cr2, S / 2 + cr2, S / 2 + cr2], fill=bottom + (255,))
    # A glint.
    d.ellipse([S * 0.26, S * 0.2, S * 0.4, S * 0.32], fill=(255, 255, 255, 150))
    im.resize((size, size), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))


def empty(name, size=48):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    o = S * 0.1
    d.ellipse([o, o, S - o, S - o], fill=(20, 30, 40, 120), outline=(255, 255, 255, 110), width=int(S * 0.06))
    im.resize((size, size), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))


def pointer(name, size=64):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = [(S * 0.18, S * 0.12), (S * 0.9, S * 0.5), (S * 0.18, S * 0.88)]
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh).polygon([(x + S * 0.04, y + S * 0.05) for x, y in pts], fill=(0, 0, 0, 120))
    sh = sh.filter(ImageFilter.GaussianBlur(S * 0.03))
    im.alpha_composite(sh)
    d.polygon(pts, fill=(255, 255, 255, 255), outline=(40, 50, 70, 255))
    im.resize((size, size), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))


if __name__ == "__main__":
    button("fight", rgb("ff5a8a"), rgb("d81f55"))
    button("pokemon", rgb("4fe08e"), rgb("13a35a"))
    button("bag", rgb("ffd54a"), rgb("eda210"))
    button("run", rgb("5aaeff"), rgb("2468d8"))
    plate("plate_mine", 600, 160)
    plate("plate_foe", 600, 128)
    ball("ball_full", rgb("f04848"), rgb("f8f8f8"), rgb("26262e"))
    ball("ball_down", rgb("7c7c86"), rgb("b4b4bc"), rgb("3a3a44"))
    empty("ball_empty")
    pointer("pointer")
    print("wrote", os.path.normpath(OUT))
