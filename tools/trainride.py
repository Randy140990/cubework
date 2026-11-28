#!/usr/bin/env python3
"""
(0.78, John's 28 Nov 12:31 and 14:54) The MAGNET TRAIN's ride, between the two tunnels: "have the cutscene where it
looks like the train is moving from left to right. So it'll come into the middle where there's the trees either side
in our style going past you, and then obviously it'll go out the other side of the screen". "that middle animation
which I asked to happen isn't happening, where you can see like the train's travelling".

A room of its own in the interiors (magnet_train_ride, x 21248): a long strip of open country, John's own line along
the middle of it (a stretch of his GOLDENROD viaduct copied out of his 12:43 save, its quartz piers every thirteen
blocks, repeated the length of the room), grass under it and rows of trees in our style (the forest's: oak trunks,
azalea and birch leaves, drawn leafy) along both sides. game/MagnetTrain.java runs the train across it from the left
to the right, the camera held at the start, then going along with it through the middle (the trees rushing past),
then held at the end as it goes off to the right.

Writes src/main/resources/retromon/interiors/magnet_train_ride.json and magnet_train_ride.bin.

    python3 -I tools/trainride.py <John's world folder>
"""
import json, os, random, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import spotworld  # noqa: E402
import caves48  # noqa: E402

RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
W, D, H = 320, 44, 24
FLOOR = 4  # where you'd stand on the grass (the grass at y 3)
LINE_Z = 18  # the line's north edge (John's z 683): seven wide, its deck's top at FLOOR + 8
ORIGIN = (21248, 64, 0)
# The stretch of his line copied (x -2090 to -2078: from one pier to the next), from his ground's top (y 20) up.
SRC = (-2090, -2078, 20, 29, 683, 689)
LINE_BLOCKS = ("quartz", "concrete", "yellow_terracotta")


def main():
    world = sys.argv[1]
    Wd = spotworld.World(world)
    x0, x1, y0, y1, z0, z1 = SRC
    names, ids = Wd.box(x0, x1, y0, y1, z0, z1)
    unit = {}
    for yy in range(ids.shape[0]):
        for zz in range(ids.shape[1]):
            for xx in range(ids.shape[2]):
                n = names[ids[yy, zz, xx]]
                if any(k in n for k in LINE_BLOCKS): unit[(xx, yy, zz)] = n
    period = x1 - x0 + 1
    grid = [["minecraft:air"] * (W * D) for _ in range(H)]

    def put(x, y, z, n):
        if 0 <= x < W and 0 <= y < H and 0 <= z < D: grid[y][z * W + x] = n
    for z in range(D):
        for x in range(W):
            for y in range(0, FLOOR - 1): put(x, y, z, "minecraft:dirt")
            put(x, FLOOR - 1, z, "minecraft:grass_block[snowy=false]")
    # The line, the whole length.
    for x in range(W):
        for (ux, uy, uz), n in unit.items():
            if ux == x % period: put(x, FLOOR + uy, LINE_Z + uz, n)
    # Rows of trees either side of it, staggered, a little apart: a trunk of oak and a crown of leaves (the forest's).
    rnd = random.Random(78)
    leaves = ["minecraft:azalea_leaves[distance=1,persistent=true,waterlogged=false]", "minecraft:birch_leaves[distance=1,persistent=true,waterlogged=false]"]

    def tree(cx, cz, h):
        leaf = leaves[rnd.randrange(2)]
        for y in range(FLOOR, FLOOR + h): put(cx, y, cz, "minecraft:oak_wood[axis=y]")
        top = FLOOR + h
        for dy in range(-2, 2):
            r = 2 if dy in (-1, 0) else 1
            for dz in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if abs(dx) == r and abs(dz) == r and r > 1: continue
                    put(cx + dx, top + dy, cz + dz, leaf)
    for rows in ((3, 8, 13), (29, 34, 39)):
        for i, z in enumerate(rows):
            for x in range(2 + (i % 2) * 3, W - 2, 6):
                tree(x + rnd.randrange(-1, 2), z + rnd.randrange(-1, 2), 3 + rnd.randrange(2))
    flat = [grid[y][i] for y in range(H) for i in range(W * D)]
    size = caves48.write_bin(os.path.join(RES, "magnet_train_ride.bin"), W, H, D, flat)
    room = {
        "origin": list(ORIGIN), "W": W, "D": D, "H": H - FLOOR, "name": "Magnet Train", "floor": "", "place": "magnet_train_ride", "schem": "magnet_train_ride.bin",
        "base": -FLOOR, "dark": False, "tile": [1.0, 1.0], "spawn": {"x": W / 2, "z": 41.5, "yaw": 180.0}, "entry": {"x": W / 2, "z": 41.5, "yaw": 180.0},
        "cam": {"x": W / 2, "z": D / 2, "dist": 30}, "cells": [], "warps": [], "people": [], "acts": [], "grass": [], "items": [], "mons": [], "lights": [],
        "pools": [], "panes": [], "clocks": [],
        # (Where the train runs, for the game: the middle of the line's deck, and its top over the grass.)
        "line": {"z": LINE_Z + 3.5, "y": float(len({p[1] for p in unit}) and max(p[1] for p in unit if "concrete" in unit[p]) + 1)},
    }
    json.dump({"rooms": {"magnet_train_ride": room}, "doors": {}}, open(os.path.join(RES, "magnet_train_ride.json"), "w"), indent=1)
    print("bin", size, "line", room["line"], "unit blocks", len(unit))


if __name__ == "__main__":
    main()
