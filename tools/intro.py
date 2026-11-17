#!/usr/bin/env python3
"""Makes the intro screen's picture: John's pixelised Charizard, taken down to chunky pixels on a night-blue sky."""
import os, random
from PIL import Image, ImageFilter

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "kantojohto", "textures", "gui", "intro.png")
W, H = 480, 270
src = Image.open(os.path.join(HERE, "art", "charizard-pixelized.jpg")).convert("RGB")
src = src.resize((round(src.width * 472 / src.height), 472), Image.LANCZOS)
# Key out the white background: only the near-white connected to the picture's edges (white inside the
# body, like highlights, stays).
px = src.load()
w, h = src.size
def white(x, y):
    r, g, b = px[x, y]
    return r > 215 and g > 215 and b > 200
mask = Image.new("L", src.size, 255)
mp = mask.load()
stack = [(x, 0) for x in range(w)] + [(x, h - 1) for x in range(w)] + [(0, y) for y in range(h)] + [(w - 1, y) for y in range(h)]
seen = bytearray(w * h)
while stack:
    x, y = stack.pop()
    if x < 0 or y < 0 or x >= w or y >= h or seen[y * w + x]: continue
    seen[y * w + x] = 1
    if not white(x, y): continue
    mp[x, y] = 0
    for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
        if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx]: stack.append((nx, ny))
mask = mask.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))
src.putalpha(mask)
# Down to about 150 pixels tall, with a small palette, then back up 2x for chunky pixels.
th = 118
small = src.resize((round(src.width * th / src.height), th), Image.LANCZOS)
alpha = small.split()[3].point(lambda a: 255 if a > 120 else 0)
pal = small.convert("RGB").quantize(colors=20, method=Image.Quantize.MEDIANCUT).convert("RGB")
pal.putalpha(alpha)
big = pal.resize((pal.width * 2, pal.height * 2), Image.NEAREST)
# Night-blue sky, a few pixel stars, a darker ground band.
bg = Image.new("RGB", (W, H))
bp = bg.load()
rnd = random.Random(1996)
for y in range(H):
    t = y / H
    c = (int(16 + 30 * t), int(24 + 52 * t), int(58 + 70 * t))
    for x in range(W):
        bp[x, y] = c
for _ in range(90):
    x, y = rnd.randrange(W), rnd.randrange(int(H * 0.7))
    v = rnd.choice([160, 200, 240])
    bp[x, y] = (v, v, min(255, v + 15))
for y in range(int(H * 0.86), H):
    for x in range(W):
        r, g, b = bp[x, y]
        bp[x, y] = (r // 2, g // 2 + 8, b // 2)
bg = bg.convert("RGBA")
bg.alpha_composite(big, (W - big.width - 18, H - big.height - 10))
bg.convert("RGB").save(OUT)
print(OUT, bg.size, "charizard", big.size)
