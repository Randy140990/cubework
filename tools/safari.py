#!/usr/bin/env python3
"""
The SAFARI ZONE as John built it (tools/safari/safari_zone.schem: FireRed's four areas, West, Center and East side by
side and North above, at two blocks a step, one connected build), made into a room of its own in the interiors (as
Viridian Forest is, tools/forest.py). Out of its entrance building's glass doors at the bottom (they face north into
the Center area) from FUCHSIA CITY's SAFARI ZONE gate; back in through them to the gate. Its five little houses (four
REST HOUSEs and the SECRET HOUSE) are rooms of the Interior Viewer's (rooms.js), in and out at their doors.

Writes src/main/resources/retromon/interiors/safari_zone.json (the room), safari_zone.bin (its blocks), and
tools/interior/safari_data.js for the Interior Viewer.

    python3 -I tools/safari.py
"""
import gzip, io, json, math, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402

SRC = os.path.join(HERE, "safari", "safari_zone.schem")
RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "safari_data.js")
ORIGIN = (6144, 64, 0)
FLOOR = 6  # the schematic's y you stand at on the grass (its ground is y 5)
FREE = {"air", "light", "short_grass", "tall_grass", "large_fern", "pale_oak_shelf", "wildflowers", "poppy", "dandelion", "lily_of_the_valley",
        "pink_petals", "moss_carpet", "leaf_litter"}
GRASS = {"tall_grass", "large_fern"}
# FireRed's four areas (pret pokefirered's SafariZone_* maps), each step two blocks: the area's top left in the
# schematic (tools/safari's notes, 14:01) and its size in blocks; the place its wild POKéMON come from.
AREAS = [("safari_zone_center", 116, 126, 102, 72), ("safari_zone_west", 8, 108, 96, 72), ("safari_zone_north", 106, 8, 114, 80),
         ("safari_zone_east", 236, 104, 108, 70)]
# FireRed's items in each area (its steps), as keys of field_items.tsv.
ITEMS = {
    "safari_zone_center": [("safari_zone_center_nugget", (24, 15))],
    "safari_zone_west": [("safari_zone_west_gold_teeth", (28, 14)), ("safari_zone_west_tm32", (17, 13)), ("safari_zone_west_max_potion", (16, 27)),
                         ("safari_zone_west_max_revive", (27, 24))],
    "safari_zone_east": [("safari_zone_east_max_potion", (13, 12)), ("safari_zone_east_full_restore", (37, 15)), ("safari_zone_east_tm11", (31, 18)),
                         ("safari_zone_east_leaf_stone", (36, 17))],
    "safari_zone_north": [("safari_zone_north_protein", (33, 5)), ("safari_zone_north_tm47", (28, 9)), ("safari_zone_north_quick_claw", (29, 17))],
}
# FireRed's signs (its steps): what they say.
SIGNS = {
    "safari_zone_center": [((25, 27), ["TRAINER TIPS", "Press START to check how many steps and SAFARI BALLs you have left!"]),
                           ((33, 20), ["CENTER AREA", "NORTH: AREA 2", "WEST: AREA 3", "EAST: AREA 1"]), ((30, 26), ["REST HOUSE"])],
    "safari_zone_west": [((27, 9), ["REQUEST NOTICE", "Please find the SAFARI ZONE WARDEN's lost GOLD TEETH.", "They're around here somewhere.",
                                    "Reward offered!", "Contact: WARDEN"]),
                         ((36, 10), ["TRAINER TIPS", "Zone Exploration Campaign!", "The Search for the SECRET HOUSE!"]),
                         ((35, 26), ["AREA 3", "WEST"]), ((22, 19), ["REST HOUSE"])],
    "safari_zone_east": [((18, 27), ["AREA 1", "EAST"]), ((43, 15), ["REST HOUSE"]),
                         ((17, 8), ["TRAINER TIPS", "The SECRET HOUSE is still ahead!"])],
    "safari_zone_north": [((13, 27), ["TRAINER TIPS", "SAFARI BALLs can be thrown at POKéMON to catch them!"]), ((23, 31), ["AREA 2", "NORTH"]),
                          ((29, 32), ["TRAINER TIPS", "Throw BAIT and a POKéMON is less likely to run away!"]),
                          ((34, 28), ["TRAINER TIPS", "Throw a ROCK and a POKéMON is easier to catch, but more likely to run away!"]),
                          ((44, 9), ["REST HOUSE"])],
}
# The five little houses' doors (the warped doors' west block, facing south), and their rooms (the Interior Viewer's).
HOUSES = [((176, 176), "safari_rest_center"), ((48, 145), "safari_rest_west"), ((194, 24), "safari_rest_north"), ((318, 132), "safari_rest_east"),
          ((33, 122), "safari_secret_house")]
# The entrance building's glass doors (they face north into the Center area), and where you come out of them.
ENTRANCE = dict(x0=166.0, x1=170.0, z=188.4)
FROM_GATE = {"x": 168.0, "z": 182.4, "yaw": 180.0}
# Where the gate (the Interior Viewer's safari_gate) puts you coming back: by its way through to the north.
GATE_FROM_SAFARI = {"x": 7.0, "z": 2.6, "yaw": 0.0}


def area_of(x, z):
    best = None
    for place, x0, z0, w, d in AREAS:
        dx = max(x0 - x, 0, x - (x0 + w)); dz = max(z0 - z, 0, z - (z0 + d))
        dd = dx * dx + dz * dz
        if best is None or dd < best[0]: best = (dd, place)
    return best[1]


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
    W, H, L, inv, ids, _b, _r = schem.blocks(schem.load(SRC))
    names = [inv[i] for i in ids]
    full = lambda x, y, z: names[(y * L + z) * W + x] if 0 <= x < W and 0 <= y < H and 0 <= z < L else "minecraft:air"
    base = lambda x, y, z: full(x, y, z).replace("minecraft:", "").split("[")[0]

    def stand(x, z):
        for y in range(FLOOR, FLOOR + 3):
            below, here, up = base(x, y - 1, z), base(x, y, z), base(x, y + 1, z)
            if below in FREE or below == "water": continue
            if below.endswith(("_fence", "_wall", "_leaves", "_log", "_wood")): return None
            if here in FREE and up in FREE:
                if below.endswith("_slab") and "type=bottom" in full(x, y - 1, z): return y - 0.5
                return y
        return None
    S = {(x, z): stand(x, z) for z in range(L) for x in range(W)}

    def nearest(x, z, label, taken=()):
        best = None
        for r in range(0, 10):
            for dz in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    cx, cz = int(math.floor(x)) + dx, int(math.floor(z)) + dz
                    if S.get((cx, cz)) is None or (cx, cz) in taken: continue
                    dd = (cx + 0.5 - x) ** 2 + (cz + 0.5 - z) ** 2
                    if best is None or dd < best[0]: best = (dd, cx, cz)
            if best: break
        assert best, label
        return best[1] + 0.5, best[2] + 0.5

    corner = {p: (x0, z0) for p, x0, z0, _w, _d in AREAS}
    tile = lambda place, c, r: (corner[place][0] + 2 * c + 1.0, corner[place][1] + 2 * r + 1.0)
    taken, items = set(), []
    for place, lst in ITEMS.items():
        for key, (c, r) in lst:
            x, z = nearest(*tile(place, c, r), key, taken)
            taken.add((int(x), int(z)))
            items.append({"key": key, "x": round(x, 3), "z": round(z, 3), "y": S[(int(x), int(z))] - FLOOR})
    # The signs: John's (pale oak shelves, two wide); each says what FireRed's nearest sign says.
    shelves = sorted({(x, z) for z in range(L) for x in range(W) for y in range(FLOOR, FLOOR + 2) if base(x, y, z) == "pale_oak_shelf"})
    signs = []
    for (x, z) in shelves:
        if (x - 1, z) in shelves: continue
        place = area_of(x, z)
        best = None
        for (c, r), lines in SIGNS.get(place, []):
            tx, tz = tile(place, c, r)
            d = math.hypot(tx - x, tz - z)
            if best is None or d < best[0]: best = (d, lines)
        signs.append({"rect": [x - 0.4, z - 0.4, x + 2.4, z + 1.4], "label": "Read", "lines": best[1] if best else ["SAFARI ZONE"]})
    # The tall grass (where its wild POKéMON are), as boxes.
    rows = {}
    for z in range(L):
        x = 0
        while x < W:
            if base(x, FLOOR, z) in GRASS:
                x0 = x
                while x < W and base(x, FLOOR, z) in GRASS: x += 1
                rows.setdefault(z, []).append((x0, x))
            else:
                x += 1
    rects, opened = [], {}
    for z in range(L + 1):
        now = set(rows.get(z, []))
        for run in list(opened):
            if run not in now: rects.append([run[0], opened.pop(run), run[1], z])
        for run in now:
            if run not in opened: opened[run] = z
    warps = [{"kind": "mat", "x0": ENTRANCE["x0"], "x1": ENTRANCE["x1"], "z": ENTRANCE["z"], "door": None, "to": "safari_gate", "spawn": GATE_FROM_SAFARI}]
    for (dx, dz), rid in HOUSES:
        assert base(dx, FLOOR + 1, dz) == "warped_door", (rid, base(dx, FLOOR + 1, dz))
        warps.append({"kind": "north", "x0": dx - 0.1, "x1": dx + 2.1, "z": dz + 1.3, "door": None, "to": rid, "spawn": {"x": 6.0, "z": 9.5, "yaw": 180.0}})
    houses = [{"room": rid, "out": {"x": dx + 1.0, "z": dz + 3.0, "y": (S.get((dx, dz + 3)) or FLOOR) - FLOOR, "yaw": 0.0}} for (dx, dz), rid in HOUSES]
    size = write_bin(os.path.join(RES, "safari_zone.bin"), W, H, L, names)
    # The areas (which place you're in, for its wild POKéMON and the pill): their boxes, the paths between going to the nearer.
    areas = [{"place": p, "rect": [x0, z0, x0 + w, z0 + d]} for p, x0, z0, w, d in AREAS]
    room = {
        "origin": list(ORIGIN), "W": W, "D": L, "H": H - FLOOR, "name": "Safari Zone", "floor": "", "place": "safari_zone_center",
        "schem": "safari_zone.bin", "base": -FLOOR, "dark": False, "spawn": FROM_GATE, "entry": FROM_GATE, "cam": {"x": W / 2, "z": L / 2, "dist": 30},
        "cells": [], "warps": warps, "people": [], "acts": signs, "grass": rects, "items": items, "lights": [], "pools": [], "panes": [], "clocks": [],
        "areas": areas, "houses": houses,
    }
    json.dump({"rooms": {"safari_zone": room}, "doors": {}}, open(os.path.join(RES, "safari_zone.json"), "w"), indent=1)
    print("safari_zone %d x %d x %d  bin %d bytes  grass boxes %d  items %d  signs %d  warps %d" % (W, H, L, size, len(rects), len(items), len(signs), len(warps)))
    for i in items: print("   ", i)
    # The viewer's copy: three characters a cell (the ground; what stands there; how high).
    G = {"grass_block": "g", "dirt_path": "p", "moss_block": "m", "water": "w", "sand": "s", "dripstone_block": "r", "brown_mushroom_block": "r",
         "polished_diorite": "d", "polished_diorite_slab": "d", "white_concrete": "d"}
    cells = []
    for z in range(L):
        row = []
        for x in range(W):
            top, k = 0, "."
            for y in range(H - 1, FLOOR - 1, -1):
                n = base(x, y, z)
                if n not in FREE and n != "water":
                    top = y - FLOOR + 1
                    k = ("T" if n.endswith("_leaves") else "W" if n in ("oak_wood", "spruce_log") else "F" if n in ("spruce_fence",)
                         else "R" if n in ("dripstone_block", "brown_mushroom_block", "jungle_stairs") else "U")
                    break
            g = G.get(base(x, FLOOR - 1, z), "g")
            if base(x, FLOOR, z) == "water" or base(x, FLOOR - 1, z) == "water": g = "w"
            if k == "." and base(x, FLOOR, z) in GRASS: k = "t"
            row.append(g + k + chr(48 + min(max(top, 0), 40)))
        cells.append("".join(row))
    js = ("// The SAFARI ZONE as the mod has it (tools/safari.py, from John's schematic): a row of cells for each z, three\n"
          "// characters a cell (the ground; what stands there; how high), its items, signs and ways in and out.\n"
          "const SAFARI = " + json.dumps({"W": W, "D": L, "rows": cells, "items": items, "acts": signs, "warps": warps, "spawn": FROM_GATE, "areas": areas},
                                          separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")


main()
