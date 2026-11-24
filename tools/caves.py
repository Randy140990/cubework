#!/usr/bin/env python3
"""
Diglett's Cave as John built it (tools/caves/*.schem: its two entrance rooms and the cave, B1F, as Let's Go has
them), made into rooms of their own in the interiors (as the forest is, tools/forest.py):

  digletts_n    the north entrance (in from Route 2's cave mouth, north of the Route 2 gate); a ladder down
  digletts_b1f  the cave: from the ladder at its north-west end to the one at its south-east end; Diglett and Dugtrio
  digletts_s    the south entrance (in from the cave mouth on Route 11, east of Vermilion City); a ladder down

Writes src/main/resources/retromon/interiors/digletts_cave.json (the three rooms and the two doors in John's world)
and a .bin of blocks for each room (as viridian_forest.bin), and tools/interior/cave_data.js for the Interior Viewer.

In each entrance room the ladder goes down a hole by its east wall (step into it and you're down the ladder, at the
foot of the cave's ladder); in the cave, climbing a ladder takes you up into that end's entrance room, beside its
hole. Out of an entrance room through its doorway at the south: back out of the cave mouth. All of it dark and dusty.

    python3 -I tools/caves.py
"""
import gzip, io, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402
import cavecut  # noqa: E402

RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "cave_data.js")
SOFT = {"air", "ladder", "light"}  # (light: the daylight in the doorway)

# The cave mouths in John's world: the doorway's blocks at its back (you walk north into it), and where you come out
# (on the grass in front, facing south). Route 2's: stone brick pillars x -781 and -778, the dark back at z 365.
# Route 11's (east of Vermilion City): pillars x -292 and -289, the dark back at z 579.
DOORS = {
    "digletts_cave_n": {"room": "digletts_n", "door": [[-780, 30, 366], [-779, 30, 366]], "out": [-779.0, 30.0, 369.6]},
    "digletts_cave_s": {"room": "digletts_s", "door": [[-291, 31, 580], [-290, 31, 580]], "out": [-290.0, 31.0, 583.6]},
}
# Where the cave's ladders are (B1F), and where you come out at the foot of each.
# (0.61, John's 18:10) His new copies are the same cave with a border of rock round it, four blocks deep on the west and
# north (and the east, and the entrances' south): every spot placed by hand here moves four east and four south (P).
P = 4
B1F_LADDER_NW = dict(x0=6 + P, x1=8 + P, z0=6 + P, z1=7 + P)
B1F_LADDER_SE = dict(x0=164 + P, x1=166 + P, z0=142 + P, z1=143 + P)
B1F_FROM_N = {"x": 6.9 + P, "z": 7.8 + P, "yaw": 0.0}
B1F_FROM_S = {"x": 164.9 + P, "z": 143.8 + P, "yaw": 0.0}
# In an entrance room: in from the doorway (facing north), and up from the ladder (beside the hole, facing south).
ENTRANCE_IN = {"x": 9.0 + P, "z": 12.6 + P, "yaw": 180.0}
ENTRANCE_UP = {"x": 12.9 + P, "z": 11.0 + P, "yaw": 0.0}
ENTRANCE_DOOR = dict(x0=8.0 + P, x1=10.0 + P, z=14.3 + P)

ROOMS = [
    # id, schematic, origin, floor (the schematic's y you stand at), name, place, people
    ("digletts_n", "digletts_cave_north_entrance.schem", (4352, 64, 0), 3, "Diglett's Cave", "digletts_cave_route_2",
     [{"key": "digletts_cave_route_2_p0", "name": "FISHING GURU", "skin": "fishing_guru", "x": 6.5 + P, "z": 9.5 + P, "yaw": 270.0,
       "lines": ["I went to ROCK TUNNEL, but it's dark and scary.", "If a POKéMON's FLASH could light it up..."]}]),
    ("digletts_b1f", "digletts_cave_b1f.schem", (4608, 64, 0), 2, "Diglett's Cave", "digletts_cave", []),
    ("digletts_s", "digletts_cave_south_entrance.schem", (4864, 64, 0), 3, "Diglett's Cave", "digletts_cave_route_11",
     [{"key": "digletts_cave_route_11_p0", "name": "GAMBLER", "skin": "gambler", "x": 6.5 + P, "z": 10.5 + P, "yaw": 270.0,
       "lines": ["What a surprise! DIGLETTs dug this long tunnel!", "It goes right to VIRIDIAN CITY!"]}]),
]
# The cave's dust in the air (the client's haze over the screen, and motes drifting about): its colour and how thick.
HAZE = [138, 104, 70, 0.24]


def load(name):
    W, H, L, inv, ids, _b, _r = schem.blocks(schem.load(os.path.join(HERE, "caves", name)))
    return W, H, L, [inv[i] for i in ids]


def write_bin(path, W, H, L, names):
    palette, pid, seq = [], {}, []
    for n in names:
        if n not in pid:
            pid[n] = len(palette); palette.append(n)
        seq.append(pid[n])
    out = io.BytesIO()
    out.write(b"RMF1"); out.write(struct.pack(">hhhh", W, H, L, len(palette)))
    for n in palette:
        b = n.encode("utf-8"); out.write(struct.pack(">h", len(b))); out.write(b)

    def varint(v):
        while True:
            c = v & 0x7F; v >>= 7
            if v: out.write(bytes([c | 0x80]))
            else:
                out.write(bytes([c])); return
    i = 0
    while i < len(seq):
        j = i
        while j < len(seq) and seq[j] == seq[i]: j += 1
        varint(j - i); varint(seq[i]); i = j
    with gzip.open(path, "wb") as f: f.write(out.getvalue())
    return os.path.getsize(path)


def main():
    rooms, viewer = {}, {}
    for rid, src, origin, floor, name, place, people in ROOMS:
        W, H, L, names = load(src)
        if rid != "digletts_b1f":
            # (The way out at the south, 14:15: daylight at the end of the doorway, so you don't walk off into the dark,
            # and its light coming in.)
            for x in (8 + P, 9 + P):
                for y in (3, 4, 5):
                    names[(y * L + 15 + P) * W + x] = "minecraft:white_concrete"
                names[(3 * L + 14 + P) * W + x] = "minecraft:light[level=15,waterlogged=false]"
                names[(4 * L + 12 + P) * W + x] = "minecraft:light[level=11,waterlogged=false]"

        def base(x, y, z):
            if not (0 <= x < W and 0 <= y < H and 0 <= z < L): return "air"
            return names[(y * L + z) * W + x].replace("minecraft:", "").split("[")[0]

        def open_(x, z):
            return base(x, floor - 1, z) not in SOFT and base(x, floor, z) in SOFT and base(x, floor + 1, z) in SOFT

        # (15:21) The rocks as copper grates in the room (the mod draws a boulder on each): a grate hides nothing round it,
        # so the floor under a rock and the wall behind it are drawn as they are (no square, no black).
        BIG, SMALL = "minecraft:waxed_copper_grate[waterlogged=false]", "minecraft:waxed_exposed_copper_grate[waterlogged=false]"
        rocks = [BIG if n.replace("minecraft:", "").split("[")[0] in ("tuff", "cobblestone") else
                 SMALL if n.replace("minecraft:", "").split("[")[0] in ("tuff_slab", "cobblestone_slab", "andesite_slab") else n for n in names]
        # (0.52) Cut away on the camera's side, as the rooms are (tools/cavecut.py).
        rocks, _n = cavecut.cut(rocks, W, H, L)
        rocks = cavecut.fill(rocks, W, H, L, rid)
        size = write_bin(os.path.join(RES, rid + ".bin"), W, H, L, rocks)
        # The cave's floor, as boxes (where its wild POKéMON come up): each row's runs, joined down where they line up.
        rects = []
        if rid == "digletts_b1f":
            rows = {}
            for z in range(L):
                x = 0
                while x < W:
                    if open_(x, z):
                        x0 = x
                        while x < W and open_(x, z): x += 1
                        rows.setdefault(z, []).append((x0, x))
                    else:
                        x += 1
            opened = {}
            for z in range(L + 1):
                now = set(rows.get(z, []))
                for run in list(opened):
                    if run not in now: rects.append([run[0], opened.pop(run), run[1], z])
                for run in now:
                    if run not in opened: opened[run] = z
        # Faint unseen lamps over the open floor every 5 blocks (it's dark, but you can see your way).
        lights = []
        for z in range(2, L, 5):
            for x in range(2, W, 5):
                for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1), (2, 0), (0, 2)):
                    if open_(x + dx, z + dz):
                        lights.append([x + dx, z + dz]); break
        if rid == "digletts_b1f":
            warps = [dict({"kind": "up", "to": "digletts_n", "spawn": ENTRANCE_UP}, **B1F_LADDER_NW),
                     dict({"kind": "up", "to": "digletts_s", "spawn": ENTRANCE_UP}, **B1F_LADDER_SE)]
            spawn = B1F_FROM_N
        else:
            door = "digletts_cave_n" if rid == "digletts_n" else "digletts_cave_s"
            warps = [{"kind": "mat", "x0": ENTRANCE_DOOR["x0"], "x1": ENTRANCE_DOOR["x1"], "z": ENTRANCE_DOOR["z"], "door": door, "to": None},
                     {"kind": "down", "x0": 11.0 + P, "z0": 7.0 + P, "z1": 10.0 + P, "to": "digletts_b1f", "spawn": B1F_FROM_N if rid == "digletts_n" else B1F_FROM_S}]
            spawn = ENTRANCE_IN
        for p in people: assert open_(int(p["x"]), int(p["z"])), (rid, p["key"])
        for s in (spawn, ENTRANCE_UP if rid != "digletts_b1f" else B1F_FROM_S):
            assert open_(int(s["x"]), int(s["z"])), (rid, s)
        acts = []
        if rid != "digletts_b1f":
            acts.append({"rect": [11.6 + P, 7.6 + P, 14.4 + P, 10.2 + P], "label": "Look", "lines": ["A ladder going down into DIGLETT's CAVE."]})
        rooms[rid] = {
            "origin": list(origin), "W": W, "D": L, "H": H - floor, "name": name, "floor": "", "place": place,
            "schem": rid + ".bin", "base": -floor, "dark": True, "haze": HAZE,
            "spawn": spawn, "entry": spawn, "cam": {"x": W / 2, "z": L / 2, "dist": 30}, "cells": [],
            "warps": warps, "people": people, "acts": acts, "grass": rects, "items": [], "lights": lights, "pools": [], "panes": [], "clocks": [],
        }
        print("%-13s %3d x %2d x %3d  bin %5d bytes  floor boxes %3d  lamps %3d" % (rid, W, H, L, size, len(rects), len(lights)))
        # The viewer's copy: each cell's floor height (0 open floor, else how high the rock goes), and what's there.
        cells = []
        for z in range(L):
            row = []
            for x in range(W):
                if base(x, floor - 1, z) == "black_concrete" or (base(x, floor - 1, z) == "ladder"):
                    row.append("K0"); continue
                if any(base(x, y, z) == "ladder" for y in range(floor, min(H, floor + 6))):
                    row.append("L0"); continue
                if open_(x, z):
                    g = base(x, floor - 1, z)
                    row.append({"sand": "s", "coarse_dirt": "c", "packed_mud": "m", "smooth_sandstone": "S"}.get(g, "c") + "0"); continue
                top = 0
                for y in range(H - 1, floor - 1, -1):
                    if base(x, y, z) != "air": top = y - floor + 1; break
                k = base(x, floor, z)
                row.append(("r" if k in ("tuff", "cobblestone", "andesite_slab", "tuff_slab", "cobblestone_slab") else "w") + chr(48 + min(top, 40)))
            cells.append("".join(row))
        viewer[rid] = {"W": W, "D": L, "rows": cells, "people": people, "spawn": spawn, "warps": warps, "name": name}
    json.dump({"rooms": rooms, "doors": DOORS}, open(os.path.join(RES, "digletts_cave.json"), "w"), indent=1)
    js = ("// Diglett's Cave as the mod has it (tools/caves.py, from John's schematics): for each room a row of cells for each\n"
          "// z, two characters a cell (what's there, how high), its people and its ways in and out.\n"
          "const CAVES = " + json.dumps(viewer, separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")


main()
