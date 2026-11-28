#!/usr/bin/env python3
"""
(0.78) John's 28 Nov 14:54 round, the textures for tools/arealooks78.py:

  wf_foam      the foam at a waterfall's foot, in place of the cobwebs there ("make it seem a bit bubbly"): white and
               pale blue bubbles of a few sizes, heaped low, see-through between them (drawn as the cobweb's cross)
  chain_white  a chain in white ("the chains that are going across the lights ... I'm more than happy that they are
               probably a white"; and the chains at Olivine's sea gate): the steel chain's links, painted white

    python3 tools/looks78.py
"""
import os, random, sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402


def foam():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(78)
    # Bubbles, more and bigger low down, fewer and smaller up the picture.
    for _ in range(26):
        y = int(15 - abs(rnd.gauss(0, 4.5)))
        x = rnd.randrange(16)
        r = 2 if y > 10 and rnd.random() < 0.55 else 1
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy > r * r + 0.5: continue
                X, Y = x + dx, y + dy
                if not (0 <= X < 16 and 0 <= Y < 16): continue
                edge = dx * dx + dy * dy >= r * r - 0.5
                hi = dx == -r // 2 and dy == -r // 2 - (1 if r > 1 else 0)
                c = (255, 255, 255, 255) if hi else (200, 226, 244, 255) if edge else (238, 247, 252, 255)
                px[X, Y] = c
    return im


def chain_white():
    src = Image.open(os.path.join(L.TEX, "chain_steel.png")).convert("RGBA")
    out = Image.new("RGBA", src.size)
    sp, op = src.load(), out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = sp[x, y]
            if a == 0: continue
            v = (r + g + b) / 3 / 255
            k = 0.72 + v * 0.36
            op[x, y] = (min(255, round(236 * k)), min(255, round(238 * k)), min(255, round(242 * k)), a)
    return out


def main():
    foam().save(os.path.join(L.TEX, "wf_foam.png"))
    chain_white().save(os.path.join(L.TEX, "chain_white.png"))
    print("wf_foam, chain_white")


if __name__ == "__main__":
    main()
