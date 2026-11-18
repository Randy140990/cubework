"""Small pictures for Retromon's screens: emote bubbles' icons (heart, note, !, star, smile) and the battle
commands' icons (fight, bag, ball, run), drawn at 4x and shrunk for smooth edges.
Writes src/main/resources/assets/retromon/textures/gui/{emote,icon}/*.png."""
import math, os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'retromon', 'textures', 'gui')
S = 32
K = 8  # supersampling


def canvas():
    im = Image.new('RGBA', (S * K, S * K), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def save(im, *path):
    out = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.resize((S, S), Image.LANCZOS).save(out)


def pts(seq):
    return [(x * K, y * K) for x, y in seq]


def heart(d, fill, outline=None, w=0):
    poly = []
    for i in range(200):
        t = math.pi * 2 * i / 200
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        poly.append((16 + x * 0.78, 16.5 + y * 0.78))
    d.polygon(pts(poly), fill=fill, outline=outline, width=w * K)


def star(cx, cy, r1, r2, n=5, rot=-math.pi / 2):
    p = []
    for i in range(n * 2):
        r = r1 if i % 2 == 0 else r2
        a = rot + math.pi * i / n
        p.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    return p


# ---- emotes (coloured, drawn inside a white bubble by the game) ----
im, d = canvas()
heart(d, (232, 72, 96, 255))
d.ellipse(pts([(9, 9), (14, 14)]), fill=(255, 190, 200, 255))
save(im, 'emote', 'heart.png')

im, d = canvas()
d.polygon(pts(star(16, 16.5, 14, 6)), fill=(255, 205, 60, 255))
d.polygon(pts(star(16, 16.5, 7, 3)), fill=(255, 240, 170, 255))
save(im, 'emote', 'star.png')

im, d = canvas()
d.rounded_rectangle(pts([(12.5, 3), (19.5, 21)]), radius=3 * K, fill=(232, 72, 72, 255))
d.ellipse(pts([(12.5, 23), (19.5, 30)]), fill=(232, 72, 72, 255))
save(im, 'emote', 'exclaim.png')

im, d = canvas()
blue = (70, 140, 230, 255)
d.ellipse(pts([(4, 20), (13, 28)]), fill=blue)
d.ellipse(pts([(18, 17), (27, 25)]), fill=blue)
d.rectangle(pts([(10.5, 6), (13, 24)]), fill=blue)
d.rectangle(pts([(24.5, 3), (27, 21)]), fill=blue)
d.polygon(pts([(10.5, 6), (27, 3), (27, 8), (10.5, 11)]), fill=blue)
save(im, 'emote', 'note.png')

im, d = canvas()
d.ellipse(pts([(3, 3), (29, 29)]), fill=(255, 205, 60, 255))
d.ellipse(pts([(10, 10), (13.5, 14.5)]), fill=(60, 40, 30, 255))
d.ellipse(pts([(18.5, 10), (22, 14.5)]), fill=(60, 40, 30, 255))
d.chord(pts([(8.5, 13), (23.5, 25)]), 0, 180, fill=(60, 40, 30, 255))
d.chord(pts([(11, 19.5), (21, 24.5)]), 0, 180, fill=(232, 90, 90, 255))
save(im, 'emote', 'happy.png')

# ---- battle command icons (white, tinted by the game) ----
W = (255, 255, 255, 255)
im, d = canvas()  # FIGHT: a burst
d.polygon(pts(star(16, 16, 15, 7, 8, -math.pi / 2)), fill=W)
save(im, 'icon', 'fight.png')

im, d = canvas()  # BAG: a satchel with a flap and a strap
d.rounded_rectangle(pts([(4, 11), (28, 29)]), radius=4 * K, fill=W)
d.arc(pts([(9, 3), (23, 17)]), 180, 360, fill=W, width=3 * K)
d.rectangle(pts([(4, 15), (28, 16.5)]), fill=(0, 0, 0, 0))
d.rounded_rectangle(pts([(13, 13), (19, 19)]), radius=1 * K, fill=W)
save(im, 'icon', 'bag.png')

im, d = canvas()  # POKéMON: a ball
d.ellipse(pts([(3, 3), (29, 29)]), fill=W)
d.rectangle(pts([(3, 14.6), (29, 17.4)]), fill=(0, 0, 0, 0))
d.ellipse(pts([(11, 11), (21, 21)]), fill=(0, 0, 0, 0))
d.ellipse(pts([(13.5, 13.5), (18.5, 18.5)]), fill=W)
save(im, 'icon', 'ball.png')

im, d = canvas()  # RUN: a double arrow
for ox in (0, 10):
    d.polygon(pts([(4 + ox, 5), (14 + ox, 16), (4 + ox, 27), (9 + ox, 27), (19 + ox, 16), (9 + ox, 5)]), fill=W)
save(im, 'icon', 'run.png')
print('icons written')
