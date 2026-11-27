"""
0.70's pictures (John's 27 Nov 14:44):

- Pokemon Tower's black beams, bottom to top up its front, and the pillar on its top: "they should be like a metal and
  shiny". Polished metal, as Victory Road's dark gold bars (looks67.dark_gold): a soft sheen in long slanting bands, a
  fine brushed grain, no edges, laid four blocks across so the beams and the pillar run on. The beams a dark gunmetal
  with a hint of the tower's violet, the pillar bright silver.

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
import looks67  # noqa: E402

N = 64


def clamp(v):
    return max(0, min(255, int(round(v))))


def polished(base, hi, lo, seed):
    """A polished metal laid four blocks across: looks67.dark_gold's sheen and grain in other colours."""
    f = looks67.wave(seed, terms=4, lo=1, hi=2)
    g = random.Random(seed)
    grain = [g.random() for _ in range(N)]
    im = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            s = 0.5 + 0.5 * math.sin(2 * math.pi * (x + y) / N + 0.6)
            sheen = s ** 10 * 0.9 + 0.2 * (0.5 + 0.5 * math.sin(2 * math.pi * (x + y) / N + 2.2)) - 0.12
            k = 0.06 * f(x, y) + 0.05 * (grain[y] - 0.5) + 0.02 * (g.random() - 0.5)
            t = max(-1.0, min(1.0, sheen + k))
            c = [base[i] + (hi[i] - base[i]) * t if t >= 0 else base[i] + (lo[i] - base[i]) * -t for i in range(3)]
            im.putpixel((x, y), tuple(clamp(v) for v in c) + (255,))
    return im


def main():
    L.save("tower_beam_metal", polished((66, 64, 80), (196, 196, 214), (22, 20, 30), 701))
    L.save("tower_spire_metal", polished((176, 178, 190), (250, 251, 255), (112, 114, 128), 702))


if __name__ == "__main__":
    main()
