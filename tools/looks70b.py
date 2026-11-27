"""
0.70's pictures for John's 27 Nov 15:00 Johto pass (tools/arealooks70b.py lays them):

- Goldenrod's fences, their tops (the white carpet rail and the posts' caps): "seamless white or marble ... Probably not
  many seams need to be seen" (gold_marble: white marble with the faintest grey veins, no edges, four blocks across).
- Goldenrod's MAGNET TRAIN plaza and its way over to the city ("not too keen on the floor being like this continued
  brick ... where it would have been a glazed terracotta ... It doesn't look as seamless as it probably could"): a
  warm cream stone paving in big flags (gold_plaza), its gold square and its green round the fountains a seamless
  terrazzo in their colours (no tiles), laid four blocks across.
- The NATIONAL PARK's swan, its eye ("a gem, like a blue and a shine to it"): a sapphire with a white glint.
- The TIN TOWER's roofs ("Tiling I just think could probably be a little bit better"): one run of rounded tiles in
  courses over its red and its gold alike, laid four blocks across from where each face is, so the tiles run on from
  block to block and from the red to the gold.
- Olivine's harbour gate at the pier's end ("the textures at the back haven't been done"): the sea wall's white, in
  big dressed stones with the faintest joints.
- The WHIRL ISLANDS' whirlpools ("semi-retro realistic"): a funnel of water, dark at its eye, with fine streaks of foam
  drawn in round it in a spiral, thinning out to the sea at its rim (32 frames, the streaks running inwards).
- Olivine's one turquoise roof ("Maybe just one of the top ones ... a sort of turquoise blue"), and Cianwood's in sea
  colours and one yellow ("any sea colours. Maybe one can have like a yellow roof"): the green roofs' tiles in those
  colours (looks56.tint_of).

Writes into src/main/resources/assets/retromon/textures/block/area (lookarea.save).
"""
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402
import looks56  # noqa: E402
import looks67  # noqa: E402

N = 64


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return tuple(clamp(c[i] + (255 - c[i]) * k if k > 0 else c[i] * (1 + k)) for i in range(3)) + (255,)


def avg(name):
    im = Image.open(os.path.join(L.TEX, name + ".png")).convert("RGB")
    px = list(im.getdata())
    return tuple(sum(p[i] for p in px) / len(px) for i in range(3))


def marble(base=(244, 242, 237), vein=(198, 196, 192), seed=7001):
    """White marble: the faintest soft unevenness and a few thin grey veins wandering across, no edges anywhere."""
    w1, w2 = looks67.wave(seed, terms=5, lo=1, hi=3), looks67.wave(seed + 1, terms=4, lo=1, hi=2)
    g = random.Random(seed)
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            a = 2 * math.pi * (x + 2 * y) / N + 2.6 * w1(x, y)
            v = abs(math.sin(a))
            t = max(0.0, 1 - v / 0.09) * 0.55 + max(0.0, 1 - abs(math.sin(2 * math.pi * (2 * x - y) / N + 2.0 * w2(x, y))) / 0.05) * 0.25
            k = 0.018 * w2(x, y) + (g.random() - 0.5) * 0.01
            c = shade(base, k)
            c = tuple(clamp(c[i] + (vein[i] - c[i]) * min(1.0, t)) for i in range(3)) + (255,)
            im.putpixel((x, y), c)
    return im


def flags(base, seed=7002, w=32, h=32):
    """A stone paving in big flags (two blocks by two), each a touch lighter or darker, its joints fine and soft, the
    rows set half a flag over."""
    g = random.Random(seed)
    tone = {(i, j): (g.random() - 0.5) * 0.05 for i in range(N // w + 1) for j in range(N // h)}
    f = looks67.wave(seed, terms=4, lo=1, hi=3)
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        j = y // h
        off = (w // 2) * (j % 2)
        for x in range(N):
            xx = (x + off) % N
            i = xx // w
            k = tone[(i % (N // w), j)] + 0.025 * f(x, y) + (g.random() - 0.5) * 0.018
            if xx % w == 0 or y % h == 0: k -= 0.09
            elif xx % w == 1 or y % h == 1: k += 0.03
            im.putpixel((x, y), shade(base, k))
    return im


def terrazzo(base, seed, chips=0.09):
    """A seamless terrazzo in one colour: a soft ground, small chips of lighter and darker stone through it, no tiles."""
    f = looks67.wave(seed, terms=4, lo=1, hi=3)
    g = random.Random(seed)
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            k = 0.03 * f(x, y) + (g.random() - 0.5) * 0.015
            r = g.random()
            if r < chips * 0.5: k += 0.16
            elif r < chips: k -= 0.14
            im.putpixel((x, y), shade(base, k))
    return im


def gem():
    """A sapphire for the swan's eye (a button: its face the picture's middle, x 5 to 11, y 6 to 10; its sides the
    bottom rows): deep blue, lighter to the top left, a white glint, its facets' edges."""
    deep, mid, hi = (18, 46, 128), (40, 104, 214), (120, 182, 255)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = ((x - 5) / 6 + (y - 6) / 4) / 2
            c = tuple(clamp(hi[i] + (deep[i] - hi[i]) * max(0.0, min(1.0, t))) for i in range(3)) + (255,)
            if y >= 14: c = shade(mid, -0.25 if (x + y) % 2 else -0.15)
            im.putpixel((x, y), c)
    for x, y in ((6, 7), (7, 7), (6, 8)):
        im.putpixel((x, y), (250, 252, 255, 255))
    im.putpixel((9, 9), (210, 230, 255, 255))
    for x in range(5, 11):
        im.putpixel((x, 9), shade(im.getpixel((x, 9)), -0.18))
    return im


def tiles(base, seed, tw=16, th=8):
    """Rounded roof tiles in channels a block wide running down the roof, two courses to a block: each tile lit at its
    top and shaded to its rounded foot, the course above's lip over it with a fine shadow under, the joints between
    the channels soft; a metal's sheen across. Drawn for a roof running down to the south (the picture's foot); the
    same courses in every colour, so the red channels and the gold run on side by side."""
    g = random.Random(seed)
    sheen = looks67.wave(seed, terms=3, lo=1, hi=2)
    im = Image.new("RGBA", (N, N))

    def bulge(xx):
        u = (xx + 0.5 - tw / 2) / (tw / 2)
        return 2.2 * max(0.0, 1 - u * u)
    for y in range(N):
        for x in range(N):
            c_ = y // th
            yy = y % th
            xx = x % tw
            # The course above's tile hangs down over this one's top in its rounded lip.
            pb = bulge(xx)
            if yy < pb:
                t, lip = 1.0, True
            else:
                t, lip = (yy - pb) / max(1.0, th - pb), False
            k = 0.14 - 0.3 * t
            if lip: k = 0.06 if yy < pb - 1 else -0.04
            elif yy - pb < 1: k -= 0.22
            if not lip and xx == 0: k -= 0.1
            k += 0.06 * (0.5 + 0.5 * math.sin(2 * math.pi * (x + y) / N + 0.8)) ** 6 + 0.03 * sheen(x, y) + (g.random() - 0.5) * 0.02
            im.putpixel((x, y), shade(base, k))
    return im


def ashlar(base, seed=7003, h=16, w=32):
    """Dressed white stone: big blocks a block high and two long, the courses set half over, the joints the faintest
    line (a sea wall's white, not a brick)."""
    g = random.Random(seed)
    f = looks67.wave(seed, terms=4, lo=1, hi=3)
    tone = {(i, j): (g.random() - 0.5) * 0.03 for i in range(N // w) for j in range(N // h)}
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        j = y // h
        off = (w // 2) * (j % 2)
        for x in range(N):
            xx = (x + off) % N
            k = tone[(xx // w, j)] + 0.02 * f(x, y) + (g.random() - 0.5) * 0.012
            if xx % w == 0 or y % h == 0: k -= 0.06
            im.putpixel((x, y), shade(base, k))
    return im


def whirlpool(size=80, frames=32):
    """A whirlpool: the sea dipping to a dark eye, fine streaks of foam spiralling in round it, thick by the funnel and
    thinning out to the sea at its rim (see-through there, so it settles into the water round it). The streaks' spiral
    moves on a whole turn of its phase over the 32 frames, so they run in and round and it loops."""
    im = Image.new("RGBA", (size, size * frames))
    g = random.Random(7004)
    grain = [[g.random() for _ in range(size)] for _ in range(size)]
    deep, mid, sea = (10, 36, 70), (24, 88, 140), (46, 124, 176)
    for f in range(frames):
        ph = 2 * math.pi * f / frames
        for y in range(size):
            for x in range(size):
                dx, dy = (x + 0.5 - size / 2) / (size / 2), (y + 0.5 - size / 2) / (size / 2)
                rr = math.hypot(dx, dy)
                if rr > 1: continue
                a = math.atan2(dy, dx)
                lr = math.log(max(rr, 0.04))
                # Two sets of streaks (5 broad, 9 fine), each a log spiral, both moving inwards with the phase.
                s1 = 0.5 + 0.5 * math.cos(5 * a + 7.5 * lr + ph * 5 / 5)
                s2 = 0.5 + 0.5 * math.cos(9 * a + 12.0 * lr - ph * 2 + 1.3)
                band = math.exp(-((rr - 0.36) / 0.2) ** 2)   # where the foam's thickest: round the funnel
                foam = (s1 ** 12 * 0.85 + s2 ** 18 * 0.55) * band
                foam *= 0.7 + 0.6 * grain[y][x]
                # The water dipping to its eye: darker and darker in towards the middle, the streaks' troughs darker.
                d = max(0.0, 1 - rr / 0.8)
                water = tuple(sea[i] + (deep[i] - sea[i]) * d ** 1.3 for i in range(3))
                water = tuple(water[i] * (1 - 0.12 * (1 - s1) * d) for i in range(3))
                fa = min(1.0, foam * 1.5)
                c = tuple(water[i] + ((214, 232, 242)[i] - water[i]) * fa for i in range(3))
                # See-through out to its rim (the sea round it showing through), the eye solid dark.
                al = min(1.0, (1 - rr) * 1.8) ** 1.5 * (200 + 50 * fa)
                if rr < 0.07: c, al = deep, 235
                im.putpixel((x, y + f * size), tuple(clamp(v) for v in c) + (clamp(al),))
    return im


def main():
    L.save("gold_marble", marble())
    L.save("gold_plaza", flags((224, 212, 184)))
    L.save("gold_plaza_gold", terrazzo(avg("gold_pave"), 7011))
    L.save("gold_plaza_green", terrazzo(avg("gold_lawn"), 7012))
    L.save("gold_plaza_lime", terrazzo(tuple(min(255, v * 1.08) for v in avg("cel_lime")), 7013))
    L.save("swan_gem", gem())
    # Each in four turns, for the roof's four sides (each side's running down away from the tower): to the south as
    # drawn, to the north turned over, to the east and the west across (arealooks70b lays each on its side).
    for name, base in (("tin_roof", (154, 82, 54)), ("tin_ridge", (182, 140, 60))):
        south = tiles(base, 7021)
        east = south.transpose(Image.TRANSPOSE)
        for side, im in (("south", south), ("north", south.transpose(Image.FLIP_TOP_BOTTOM)), ("east", east),
                         ("west", east.transpose(Image.FLIP_LEFT_RIGHT))):
            L.save("%s_%s" % (name, side), im)
    L.save("ol_gate_stone", ashlar((236, 234, 228)))
    L.save("whirlpool", whirlpool())
    with open(os.path.join(L.TEX, "whirlpool.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)
    for k, c in (("teal", "#2B9DB2"), ("sea", "#3A78BA"), ("aqua", "#4CB5A6"), ("sun", "#E0B33E")):
        for part in ("tiles", "edge", "under"):
            looks56.tint_of("roof_green_" + part, "roof_%s_%s" % (k, part), L.hexc(c))


if __name__ == "__main__":
    main()
