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

# ---- the START menu's apps (white, on coloured rounds) ----
T = (0, 0, 0, 0)
im, d = canvas()  # POKéDEX: the device, its lens and a screen
d.rounded_rectangle(pts([(5, 4), (27, 28)]), radius=4 * K, fill=W)
d.rounded_rectangle(pts([(9, 13), (23, 24)]), radius=2 * K, fill=T)
d.ellipse(pts([(8, 6.5), (13, 11.5)]), fill=T)
d.ellipse(pts([(9.5, 8), (11.5, 10)]), fill=W)
d.ellipse(pts([(16, 8), (18, 10)]), fill=T)
d.ellipse(pts([(20, 8), (22, 10)]), fill=T)
save(im, 'icon', 'dex.png')

im, d = canvas()  # BAG: a backpack with a flap and two straps
d.rounded_rectangle(pts([(6, 7), (26, 29)]), radius=5 * K, fill=W)
d.arc(pts([(11, 1.5), (21, 11)]), 180, 360, fill=W, width=3 * K)
d.rounded_rectangle(pts([(6, 7), (26, 16)]), radius=5 * K, fill=T)
d.rounded_rectangle(pts([(7.5, 8.5), (24.5, 15)]), radius=4 * K, fill=W)
d.rectangle(pts([(6, 16), (26, 17.6)]), fill=T)
d.rounded_rectangle(pts([(10, 20), (22, 26)]), radius=2 * K, fill=T)
d.rounded_rectangle(pts([(11.5, 21.5), (20.5, 24.5)]), radius=1 * K, fill=W)
save(im, 'icon', 'backpack.png')

im, d = canvas()  # MAP: a folded map with a pin
d.polygon(pts([(3, 7), (11, 4), (21, 7), (29, 4), (29, 25), (21, 28), (11, 25), (3, 28)]), fill=W)
d.line(pts([(11, 4), (11, 25)]), fill=T, width=int(1.5 * K))
d.line(pts([(21, 7), (21, 28)]), fill=T, width=int(1.5 * K))
d.ellipse(pts([(13, 9), (19, 15)]), fill=T)
d.polygon(pts([(13.3, 13.5), (18.7, 13.5), (16, 20)]), fill=T)
save(im, 'icon', 'map.png')

im, d = canvas()  # CLOTHES: a cap
d.chord(pts([(6, 6), (26, 26)]), 180, 360, fill=W)
d.rounded_rectangle(pts([(4, 15), (30, 19)]), radius=2 * K, fill=W)
d.ellipse(pts([(14.5, 4.5), (17.5, 7.5)]), fill=W)
d.line(pts([(16, 8), (16, 15)]), fill=T, width=int(1.2 * K))
save(im, 'icon', 'cap.png')

im, d = canvas()  # CARD: an ID card
d.rounded_rectangle(pts([(3, 7), (29, 25)]), radius=3 * K, fill=W)
d.ellipse(pts([(6.5, 11), (13.5, 18)]), fill=T)
d.chord(pts([(5.5, 17), (14.5, 26)]), 180, 360, fill=T)
for y in (12, 16, 20):
    d.rounded_rectangle(pts([(16.5, y - 0.9), (26, y + 0.9)]), radius=1 * K, fill=T)
save(im, 'icon', 'card.png')

im, d = canvas()  # WAIT: a clock
d.ellipse(pts([(3, 3), (29, 29)]), fill=W)
d.ellipse(pts([(6, 6), (26, 26)]), fill=T)
d.line(pts([(16, 16), (16, 9)]), fill=W, width=int(2.4 * K))
d.line(pts([(16, 16), (21, 19)]), fill=W, width=int(2.4 * K))
d.ellipse(pts([(14.5, 14.5), (17.5, 17.5)]), fill=W)
save(im, 'icon', 'clock.png')

im, d = canvas()  # SAVE: a notebook with a ribbon
d.rounded_rectangle(pts([(6, 3), (26, 29)]), radius=3 * K, fill=W)
d.rectangle(pts([(9, 3), (10.5, 29)]), fill=T)
d.polygon(pts([(18, 3), (23, 3), (23, 13), (20.5, 10.5), (18, 13)]), fill=T)
save(im, 'icon', 'save.png')

im, d = canvas()  # OPTION: a cog
cog = []
for i in range(48):
    a = math.pi * 2 * i / 48
    r = 13.5 if (i // 3) % 2 == 0 else 10.5
    cog.append((16 + math.cos(a) * r, 16 + math.sin(a) * r))
d.polygon(pts(cog), fill=W)
d.ellipse(pts([(11.5, 11.5), (20.5, 20.5)]), fill=T)
save(im, 'icon', 'cog.png')

im, d = canvas()  # EXIT: a cross
d.line(pts([(8, 8), (24, 24)]), fill=W, width=int(4 * K))
d.line(pts([(24, 8), (8, 24)]), fill=W, width=int(4 * K))
save(im, 'icon', 'close.png')

im, d = canvas()  # the map's "you are here": an arrow
d.polygon(pts([(16, 2), (28, 28), (16, 21), (4, 28)]), fill=W)
save(im, 'icon', 'arrow.png')
print('icons written')
