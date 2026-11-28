#!/usr/bin/env python3
"""
(0.74) John's 28 Nov 10:42, Goldenrod: "the tram roof in goldenrod is white. I think I would like it to maybe be
something different", and the floor: "it's glazed terracotta and it's 4 squares together in a pattern, I think you
should still go with some sort of pattern so it still looks like it's [repeating] and tiling properly ... You've
changed it on the left-hand side, but you haven't extended it through the entire of goldenrod. So yeah, take that
pattern and extend it all the way through goldenrod".

  gold_dome        the MAGNET TRAIN station's curved roof in Goldenrod's teal (its station's own panels' colour), with
                   Saffron's dome's seams and sheen (64 px over four blocks, seamless)
  gold_plaza_path  the plaza's big cream flagstones (gold_plaza) in the yellow of Goldenrod's paths, so the paths keep
                   their colour in the plaza's pattern (64 px over four blocks)

    python3 tools/looks74.py
"""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402
import looks73  # noqa: E402

TEAL = (50, 158, 154)


def dome():
    looks73.GREEN = TEAL
    return looks73.dome()


def plaza_path():
    src = Image.open(os.path.join(L.TEX, "gold_plaza.png")).convert("RGBA")
    pave = Image.open(os.path.join(L.TEX, "gold_pave.png")).convert("RGB")
    pp = list(pave.getdata())
    want = tuple(sum(c[i] for c in pp) / len(pp) for i in range(3))
    sp = list(src.getdata())
    have = tuple(sum(c[i] for c in sp) / len(sp) for i in range(3))
    out = Image.new("RGBA", src.size)
    op, ip = out.load(), src.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = ip[x, y]
            op[x, y] = tuple(max(0, min(255, round(v * w / h))) for v, w, h in zip((r, g, b), want, have)) + (a,)
    return out


def main():
    dome().save(os.path.join(L.TEX, "gold_dome.png"))
    plaza_path().save(os.path.join(L.TEX, "gold_plaza_path.png"))
    print("gold_dome, gold_plaza_path")


if __name__ == "__main__":
    main()
