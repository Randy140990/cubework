#!/usr/bin/env python3
"""
(0.70, John's 27 Nov 15:00) What arealooks70b needs from his save, written to tools/area_70b.json:

- "piertops": every bridge's posts' top block in its deck ("one rogue block which is the same texture as the planks"):
  the stripped logs and woods at a bridge's deck height (the rows that give its deck its boards) standing on more of
  its post (a stripped log or wood, or a fence, under it).
- "rims": the mud brick slabs round Kanto's ponds (a slab with water under it or beside it, two blocks round), boxed by
  pond, so they keep their rock while Kanto's steps go to Johto's boards.

    python3 -I tools/scan70b.py <world>
"""
import json
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookview  # noqa: E402
import spotworld  # noqa: E402

TSV = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data", "area_looks.tsv")
STRIPPED = ("stripped_spruce_wood", "stripped_spruce_log", "stripped_oak_wood", "stripped_oak_log")
POST = STRIPPED + ("spruce_fence", "oak_fence")
KANTO = (-1010, 100, 60, 1060)


def piertops(W):
    rows = [l.rstrip("\n").split("\t") for l in open(TSV) if not l.startswith("#")]
    decks = sorted(set((int(r[2]), int(r[3]), int(r[4]), int(r[5]), int(r[6])) for r in rows
                       if len(r) == 10 and r[0] == "cube" and r[8] == "bridge_boards" and r[6] != "-" and r[6] == r[7]))
    out = []
    for x0, z0, x1, z1, y in decks:
        names, ids = W.box(x0, x1, y - 1, y, z0, z1)
        bn = np.array([lookview.bname(n) for n in names])
        top, under = bn[ids[1]], bn[ids[0]]
        for z, x in zip(*np.nonzero(np.isin(top, STRIPPED) & np.isin(under, POST))):
            out.append([int(x0 + x), int(y), int(z0 + z)])
    return sorted(set(map(tuple, out)))


def rims(W):
    from scipy import ndimage
    x0, z0, x1, z1 = KANTO
    found = []
    for bx in range(x0, x1 + 1, 128):
        for bz in range(z0, z1 + 1, 128):
            ex, ez = min(bx + 127, x1), min(bz + 127, z1)
            try:
                names, ids = W.box(bx - 3, ex + 3, 10, 70, bz - 3, ez + 3)
            except Exception:
                continue
            bn = np.array([lookview.bname(n) for n in names])
            slab = np.isin(ids, np.nonzero(bn == "mud_brick_slab")[0])
            if not slab.any(): continue
            water = np.isin(ids, np.nonzero(bn == "water")[0])
            near = np.zeros_like(water)
            for dy in range(4):
                near[dy:] |= water[:water.shape[0] - dy]
            near = ndimage.binary_dilation(near, structure=np.ones((1, 5, 5), bool))
            for y, z, x in zip(*np.nonzero(slab & near)):
                X, Z = bx - 3 + x, bz - 3 + z
                if bx <= X <= ex and bz <= Z <= ez: found.append((int(X), int(10 + y), int(Z)))
    # Boxed by pond: the slabs within four of each other.
    boxes = []
    for p in sorted(found):
        for b in boxes:
            if b[0] - 4 <= p[0] <= b[2] + 4 and b[1] - 4 <= p[2] <= b[3] + 4 and b[4] - 1 <= p[1] <= b[5] + 1:
                b[0], b[1], b[2], b[3] = min(b[0], p[0]), min(b[1], p[2]), max(b[2], p[0]), max(b[3], p[2])
                b[4], b[5] = min(b[4], p[1]), max(b[5], p[1])
                break
        else:
            boxes.append([p[0], p[2], p[0], p[2], p[1], p[1]])
    return boxes


def main():
    W = spotworld.World(sys.argv[1])
    out = {"piertops": piertops(W), "rims": rims(W)}
    with open(os.path.join(HERE, "area_70b.json"), "w") as f:
        json.dump(out, f, indent=0)
    print(len(out["piertops"]), "post tops in decks;", len(out["rims"]), "ponds' rims:", out["rims"])


if __name__ == "__main__":
    main()
