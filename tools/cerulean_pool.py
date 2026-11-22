#!/usr/bin/env python3
"""
Cerulean Gym's pool (John, 16:55): real water, as deep as a big pool, looking like the water outside; the white
walkways its edges. FireRed's layout on whole blocks (a step a block), so the mod can lay real blocks:

  - water five deep wherever there's no walkway (the pool: x 1..13, z 3..14 of the room),
  - white quartz under every walkway and along the pool's edges, down to the bottom (the walkways ARE the edges),
  - a dark prismarine bottom, so it reads deep.

Writes src/main/resources/retromon/interiors/cerulean_gym_pool.bin (the room's own blocks, "RMF1", laid after its
props, from y -6) and tools/interior/cerulean_pool_data.js (the same layout for the Interior Viewer's room).

    python3 -I tools/cerulean_pool.py
"""
import gzip
import io
import json
import os
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_BIN = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors", "cerulean_gym_pool.bin")
OUT_JS = os.path.join(HERE, "interior", "cerulean_pool_data.js")

W, D = 14, 19          # the room (its door nook at z 17..19)
POOL = (1, 3, 13, 14)  # x0, z0, x1, z1: the water's whole extent (cells x 1..12, z 3..13)
DEPTH = 5              # water from y -1 down to y -5; the bottom at y -6
# The walkways (x0, z0, x1, z1), FireRed's: the top one to MISTY, the left arm (DIANA), down the right, across the
# middle, the little loop at the bottom left, and the way up from the deck; SWIMMER LUIS's float.
WALKS = [(6, 4, 12, 6), (2, 5, 6, 6), (10, 6, 12, 10), (2, 8, 12, 10), (2, 10, 3, 12), (2, 11, 9, 12), (7, 10, 9, 14)]
FLOAT = (11, 11, 12, 12)
# The pool's white edge: the row along the stands, both sides, and the deck's first row.
EDGES = [(0, 2, W, 3), (0, 3, 1, 14), (13, 3, W, 14), (0, 14, W, 15)]


def inside(x, z, r):
    return r[0] <= x < r[2] and r[1] <= z < r[3]


def blocks():
    """{(x, y, z): name} of the pool's own blocks (y from -6 to -1)."""
    out = {}
    walk = lambda x, z: any(inside(x, z, r) for r in WALKS + [FLOAT])
    for x in range(W):
        for z in range(2, 15):
            edge = any(inside(x, z, r) for r in EDGES)
            pool = inside(x, z, POOL)
            if pool and not walk(x, z):
                for y in range(-DEPTH, 0):
                    out[(x, y, z)] = "minecraft:water[level=0]"
                out[(x, -DEPTH - 1, z)] = "minecraft:dark_prismarine"
            elif pool or edge:
                for y in range(-DEPTH - 1, 0):
                    out[(x, y, z)] = "minecraft:smooth_quartz"
    return out


def write_bin(path, b):
    base = -DEPTH - 1
    H = DEPTH + 1
    palette = ["minecraft:air"]
    idx = {"minecraft:air": 0}
    cells = []
    for y in range(H):
        for z in range(D):
            for x in range(W):
                n = b.get((x, y + base, z), "minecraft:air")
                if n not in idx:
                    idx[n] = len(palette)
                    palette.append(n)
                cells.append(idx[n])
    raw = io.BytesIO()
    raw.write(b"RMF1")
    raw.write(struct.pack(">hhhh", W, H, D, len(palette)))
    for n in palette:
        e = n.encode("utf-8")
        raw.write(struct.pack(">h", len(e)))
        raw.write(e)

    def varint(v):
        while True:
            b7 = v & 0x7F
            v >>= 7
            raw.write(bytes([b7 | (0x80 if v else 0)]))
            if not v:
                return
    i = 0
    while i < len(cells):
        j = i
        while j < len(cells) and cells[j] == cells[i]:
            j += 1
        varint(j - i)
        varint(cells[i])
        i = j
    with gzip.open(path, "wb") as f:
        f.write(raw.getvalue())
    return base


def main():
    b = blocks()
    base = write_bin(OUT_BIN, b)
    data = {"W": W, "D": D, "pool": POOL, "depth": DEPTH, "walks": WALKS, "float": FLOAT, "edges": EDGES, "base": base}
    with open(OUT_JS, "w") as f:
        f.write("// Cerulean Gym's pool (tools/cerulean_pool.py writes this): its layout on whole blocks, as the mod lays it.\n")
        f.write("const CERULEAN_POOL = " + json.dumps(data) + ";\n")
    n = sum(1 for v in b.values() if "water" in v)
    print(OUT_BIN, "water", n, "quartz", sum(1 for v in b.values() if "quartz" in v), "base", base)


if __name__ == "__main__":
    main()
