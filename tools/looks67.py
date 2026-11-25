"""
0.67's pictures (John's 16:28):

- The Power Plant's top trim (its yellow band and the white cap on it): seamless, "Instead of things looking like
  blocks": one clean painted colour laid four blocks across, a faint soft unevenness, no edges, no lines.
- Victory Road's buildings' tops (the band and the long bars along the top, and the caps at their ends): "the same
  colour as the sort of brown thin bits that go from bottom to top ... that all needs to have a shine to it and kind
  of, I guess, look like a very dark gold. Kind of similar to the statues on the end": a dark gold metal with a soft
  sheen running along it, laid four blocks across so the bars run on.

Writes into src/main/resources/assets/retromon/textures/block/area (lookarea.save).
"""
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402

N = 64  # four blocks across, sixteen to a block


def wave(seed, terms=5, lo=1, hi=4):
    """A smooth periodic field over the 64 by 64 picture (so it tiles without a seam), -1 to 1."""
    r = random.Random(seed)
    ts = [(r.randint(lo, hi), r.randint(-hi, hi), r.random() * 2 * math.pi, r.random() + 0.3) for _ in range(terms)]
    tot = sum(t[3] for t in ts)

    def f(x, y):
        return sum(a * math.sin(2 * math.pi * (fx * x + fy * y) / N + ph) for fx, fy, ph, a in ts) / tot
    return f


def clamp(v):
    return max(0, min(255, int(round(v))))


def seamless_paint(base, seed, amount=0.022):
    """A clean painted colour with the faintest soft unevenness, no edges anywhere (it tiles four blocks across)."""
    f = wave(seed)
    g = random.Random(seed + 1)
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            k = f(x, y) * amount + (g.random() - 0.5) * 0.008
            c = tuple(clamp(base[i] + (255 - base[i]) * k if k > 0 else base[i] * (1 + k)) for i in range(3))
            im.putpixel((x, y), c + (255,))
    return im


def dark_gold(seed=67):
    """Dark gold metal: the posts' warm brown taken darker and golder, a soft sheen in long bands along it (as a
    polished bar catches the light), a fine brushed grain, no edges (it runs on four blocks across)."""
    base, hi, lo = (168, 126, 58), (236, 204, 128), (104, 74, 32)
    f = wave(seed, terms=4, lo=1, hi=2)
    g = random.Random(seed)
    grain = [g.random() for _ in range(N)]
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            # The sheen: long soft bands at a slant (periodic over the picture), brightest in a narrow core.
            s = 0.5 + 0.5 * math.sin(2 * math.pi * (x + y) / N + 0.6)
            sheen = s ** 10 * 0.85 + 0.18 * (0.5 + 0.5 * math.sin(2 * math.pi * (x + y) / N + 2.2)) - 0.12
            k = 0.06 * f(x, y) + 0.05 * (grain[y] - 0.5) + 0.02 * (g.random() - 0.5)
            t = max(-1.0, min(1.0, sheen + k))
            if t >= 0:
                c = tuple(base[i] + (hi[i] - base[i]) * t for i in range(3))
            else:
                c = tuple(base[i] + (lo[i] - base[i]) * -t for i in range(3))
            im.putpixel((x, y), tuple(clamp(v) for v in c) + (255,))
    return im


def build():
    L.save("pp_yellow_s", seamless_paint((242, 190, 44), 671))
    L.save("pp_white_s", seamless_paint((241, 240, 236), 672, 0.014))
    L.save("vr_gold", dark_gold())


if __name__ == "__main__":
    build()
    print("0.67 pictures written")
