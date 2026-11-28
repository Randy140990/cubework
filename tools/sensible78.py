#!/usr/bin/env python3
"""
(0.78, John's 28 Nov 15:03, Fuchsia from above) "there's a person and a Pokemon, I guess, on the top" (of a roof);
"Pointless having a Pokemon hidden behind a tree, bearing in mind the camera angles. So again, if we need to adjust
some people to be a bit more sensible, then let's do that."

Goes through everyone the mod puts down outside at a best guess (data/placed.tsv's people, trainers and POKéMON out in
the world; John's own placings, config/retromon_placed.tsv, win over these and aren't touched) and finds the ones:

  - up on something: standing on a roof, a wall or in a tree (the top block where they stand three or more above the
    ground round about, and it's a roof's, a wall's or leaves);
  - hidden behind a tree: leaves standing up over the ground just in front of them, on the camera's side (south, two
    either side, up to four in front), or over their heads.

Each is moved to the nearest good spot within 14 blocks: open ground at the ground's height (grass, a path, paving,
sand), nothing leafy over it or in front of it, away from the walls of a building, and not on top of anyone else.
Writes data/placed.tsv with the moves, and prints them.

    python3 -I tools/sensible78.py <bake dir> [place ...]
"""
import math, os, sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
BAKE = sys.argv[1]
ONLY = set(sys.argv[2:])
cache = {}


def col(x, z):
    rx, rz = x >> 9, z >> 9
    if (rx, rz) not in cache:
        p = os.path.join(BAKE, "%d.%d.npz" % (rx, rz))
        cache[(rx, rz)] = np.load(p) if os.path.exists(p) else None
    d = cache[(rx, rz)]
    if d is None: return "void", -99
    return str(d["names"][d["id"][z - rz * 512, x - rx * 512]]).replace("minecraft:", "").split("[")[0], int(d["y"][z - rz * 512, x - rx * 512])


GROUND = ("grass_block", "dirt_path", "dirt", "coarse_dirt", "sand", "gravel", "moss_block", "terracotta", "podzol", "mud", "packed_mud", "snow", "snow_block",
          "short_grass", "tall_grass", "fern")
PAVING = ("_terracotta", "glazed_terracotta", "concrete", "smooth_stone", "stone_bricks", "polished_andesite", "polished_diorite", "polished_granite", "bricks",
          "planks", "quartz", "tuff", "calcite", "andesite", "diorite", "deepslate_tiles", "sandstone", "carpet")
UP = ("stairs", "slab", "wall", "leaves", "deepslate_tile", "fence", "trapdoor", "planks", "log", "wood", "glass", "lantern", "chain")


def ground_y(x, z, r=8):
    # (The ground round about: low in what's there, not under the water, so a big roof all round isn't taken for it.)
    ys = sorted(y for n, y in (col(x + dx, z + dz) for dx in range(-r, r + 1) for dz in range(-r, r + 1)) if n != "water")
    return ys[len(ys) // 8] if ys else col(x, z)[1]


def leafy_front(x, z, gy):
    for dx in range(-2, 3):
        for dz in range(0, 5):
            n, y = col(x + dx, z + dz)
            if "leaves" in n and y >= gy + 2: return True
    n, y = col(x, z - 1)
    return "leaves" in n and y >= gy + 2


def walls_near(x, z, gy):
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            n, y = col(x + dx, z + dz)
            if y >= gy + 2: return True
    return False


def bad(x, z):
    n, y = col(x, z)
    gy = ground_y(x, z)
    if y >= gy + 3 and (n not in GROUND or any(k in n for k in UP)): return "up on " + n
    if "leaves" in n: return "in a tree"
    if leafy_front(x, z, gy): return "behind a tree"
    return None


def good(x, z, gy, taken):
    n, y = col(x, z)
    if abs(y - gy) > 1 or n == "water" or "leaves" in n: return False
    if not (n in GROUND or any(n.endswith(k) or k in n for k in PAVING)): return False
    if leafy_front(x, z, gy) or walls_near(x, z, gy): return False
    return all(math.hypot(x - a, z - b) >= 2.5 for a, b in taken)


def main():
    path = os.path.join(DATA, "placed.tsv")
    lines = open(path, encoding="utf-8").read().split("\n")
    rows = [l.split("\t") for l in lines]
    taken = [(float(r[4]), float(r[6])) for r in rows if len(r) >= 9 and r[3] == "-"]
    moved = 0
    for r in rows:
        if len(r) < 9 or r[3] != "-" or r[1] not in ("person", "trainer") or r[8] != "placed": continue
        if ONLY and r[2] not in ONLY: continue
        x, z = int(math.floor(float(r[4]))), int(math.floor(float(r[6])))
        why = bad(x, z)
        if not why: continue
        gy = ground_y(x, z)
        others = [t for t in taken if (t[0], t[1]) != (float(r[4]), float(r[6]))]
        best = None
        for rad in range(1, 15):
            for dx in range(-rad, rad + 1):
                for dz in range(-rad, rad + 1):
                    if max(abs(dx), abs(dz)) != rad: continue
                    if good(x + dx, z + dz, gy, others):
                        d = math.hypot(dx, dz)
                        if best is None or d < best[0]: best = (d, x + dx, z + dz)
            if best: break
        if not best:
            print("%-34s %-14s (%d, %d) %s: nowhere good near" % (r[0], r[2], x, z, why))
            continue
        old = (r[4], r[6])
        r[4], r[6] = "%.2f" % best[1], "%.2f" % best[2]
        taken = [t for t in taken if t != (float(old[0]), float(old[1]))] + [(float(r[4]), float(r[6]))]
        moved += 1
        print("%-34s %-14s (%d, %d) %s -> (%s, %s)" % (r[0], r[2], x, z, why, r[4], r[6]))
    open(path, "w", encoding="utf-8").write("\n".join("\t".join(r) for r in rows))
    print("moved", moved)


if __name__ == "__main__":
    main()
