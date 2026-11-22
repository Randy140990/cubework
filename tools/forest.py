#!/usr/bin/env python3
"""
Viridian Forest as John built it (tools/forest/viridian_forest.schem, 88 x 18 x 140, x east and z south), made into
the mod's forest: a room of its own in the interiors (as the buildings' insides are), between Route 2's two gates.

Writes:
  src/main/resources/retromon/interiors/viridian_forest.bin   its blocks (palette, then runs), gzipped
  src/main/resources/retromon/interiors/viridian_forest.json  the room: ways in and out, people, trainers, items, signs, grass
  tools/interior/forest_data.js                               the same forest for the Interior Viewer (a block a cell)

What's changed from the schematic, as John asked (22 Nov 2026):
  - the light blue wool round the bottom of both gatehouses is gone (grass under it);
  - the first sign you come to out of the south gatehouse (FireRed's fifth TRAINER TIPS, by the door) is gone.

Where things are: FireRed's forest (pret/pokefirered, 54 x 69 tiles) was laid out by John at x = 8 + 4c/3 and
z = 2r + 2 for tile column c and row r, so a tile's middle is x = 8 + (4c + 2)/3, z = 2r + 3. The trainers, people
and item balls stand where FireRed has them (its map.json), each moved to the nearest open ground if a tree's there.

    python3 -I tools/forest.py
"""
import gzip, io, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402

SRC = os.path.join(HERE, "forest", "viridian_forest.schem")
RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "forest_data.js")

# Where the forest sits in the interiors (far past the buildings' rooms, which are 64 apart along x from 0).
ORIGIN = (4096, 64, 0)
# The schematic's y 0 is the dirt, 1 the ground: you stand at y 2, the room's floor.
FLOOR = 2

SOFT = {"air", "tall_grass", "large_fern", "short_grass", "fern", "poppy", "red_tulip", "dandelion", "light"}
GRASS = {"tall_grass", "large_fern"}


def tile(c, r):
    return 8 + (4 * c + 2) / 3.0, 2 * r + 3.0


# FireRed's people and trainers (map.json's object events), its item balls and its signs (bg events).
TRAINERS = [
    # key, FireRed tile, facing, title (the trainers' own rows are in data/trainers.tsv)
    ("viridian_forest_rick", (47, 45), "left"),
    ("viridian_forest_doug", (47, 29), "left"),
    ("viridian_forest_sammy", (7, 22), "left"),
    ("viridian_forest_anthony", (43, 6), "down"),
    ("viridian_forest_charlie", (16, 5), "up"),
]
PEOPLE = [
    ("viridian_forest_p0", "YOUNGSTER", "youngster", (29, 58), "up", ["I came here with some friends!", "They're out for POKéMON fights!"]),
    ("viridian_forest_p1", "BOY", "boy", (45, 58), "left", ["I ran out of POKé BALLs to catch POKéMON with!", "You should carry extras!"]),
]
ITEMS = [
    # key (data/field_items.tsv), FireRed tile. The two FireRed hides in the grass (a POTION, an ANTIDOTE) are balls here too.
    ("viridian_forest_poke_ball", (5, 41)),
    ("viridian_forest_antidote", (40, 21)),
    ("viridian_forest_potion", (21, 34)),
    ("viridian_forest_potion_2", (49, 60)),
    ("viridian_forest_hidden_potion", (3, 22)),
    ("viridian_forest_hidden_antidote", (28, 57)),
]
SIGNS = {
    # the sign's west block (x, z) in the schematic: what it says
    (60, 120): ["TRAINER TIPS", "If you want to avoid battles, stay away from grassy areas!"],
    (45, 90): ["For poison, use ANTIDOTE! Get it at POKéMON MARTs!"],
    (65, 54): ["TRAINER TIPS", "Contact PROF.OAK via PC to get your POKéDEX evaluated!"],
    (20, 60): ["TRAINER TIPS", "No stealing of POKéMON from other trainers! Catch only wild POKéMON!"],
    (16, 26): ["LEAVING VIRIDIAN FOREST", "PEWTER CITY AHEAD"],
}
# The sign by the south gatehouse's door (FireRed's TRAINER TIPS 5): taken away.
GONE_SIGN = (49, 122)
YAW = {"down": 0.0, "left": 90.0, "up": 180.0, "right": 270.0}

# The gatehouses' doors: the south one faces north (you walk south up its steps into it), the north one faces south.
SOUTH_DOOR = dict(x0=45.0, x1=51.0, z=126.3)  # its steps: x 45 to 50, z 125 to 126
NORTH_DOOR = dict(x0=13.0, x1=19.0, z=22.3)   # its steps: x 13 to 18, z 22 to 23
# Where you come out of each: on the path in front of the steps, facing away from the door.
FROM_SOUTH = {"x": 48.0, "z": 123.6, "yaw": 180.0}
FROM_NORTH = {"x": 15.5, "z": 24.8, "yaw": 0.0}
# ...and where the gates (Route 2's two gate rooms) put you: by their way through to the forest.
GATE_S_FROM_FOREST = {"x": 7.0, "z": 1.4, "yaw": 0.0}
GATE_N_FROM_FOREST = {"x": 7.0, "z": 12.5, "yaw": 180.0}


def main():
    root = schem.load(SRC)
    W, H, L, inv, ids, _bes, _r = schem.blocks(root)
    names = [inv[i] for i in ids]

    def idx(x, y, z):
        return (y * L + z) * W + x

    def at(x, y, z):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < L): return "minecraft:air"
        return names[idx(x, y, z)]

    def base(x, y, z):
        return at(x, y, z).replace("minecraft:", "").split("[")[0]

    # John's two changes.
    wool = 0
    for i, n in enumerate(names):
        if n.startswith("minecraft:light_blue_wool"):
            names[i] = "minecraft:air"; wool += 1
    gone = 0
    for x in (GONE_SIGN[0], GONE_SIGN[0] + 1):
        for y in range(H):
            if base(x, y, GONE_SIGN[1]) == "pale_oak_shelf":
                names[idx(x, y, GONE_SIGN[1])] = "minecraft:air"; gone += 1
    print("light blue wool taken away:", wool, " sign blocks taken away:", gone)

    # Where you can stand: ground under you, room for you (y 2 and 3).
    def open_(x, z):
        return 0 <= x < W and 0 <= z < L and base(x, FLOOR - 1, z) not in SOFT and base(x, FLOOR, z) in SOFT and base(x, FLOOR + 1, z) in SOFT

    def nearest(fx, fz, label):
        cx, cz = int(fx), int(fz)
        if open_(cx, cz): return fx, fz
        best = None
        for r in range(1, 5):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) != r or not open_(cx + dx, cz + dz): continue
                    d = (cx + dx + 0.5 - fx) ** 2 + (cz + dz + 0.5 - fz) ** 2
                    if best is None or d < best[0]: best = (d, cx + dx + 0.5, cz + dz + 0.5)
            if best: break
        print("  %s moved off a tree: (%.2f, %.2f) -> (%.2f, %.2f)" % (label, fx, fz, best[1], best[2]))
        return best[1], best[2]

    people = []
    for key, (c, r), facing in TRAINERS:
        x, z = nearest(*tile(c, r), key)
        people.append({"key": key, "name": "BUG CATCHER " + key.rsplit("_", 1)[1].upper(), "skin": "bug_catcher", "x": round(x, 3), "z": round(z, 3), "yaw": YAW[facing], "lines": [], "role": "trainer"})
    for key, name, skin, (c, r), facing, lines in PEOPLE:
        x, z = nearest(*tile(c, r), key)
        people.append({"key": key, "name": name, "skin": skin, "x": round(x, 3), "z": round(z, 3), "yaw": YAW[facing], "lines": lines})
    items = []
    for key, (c, r) in ITEMS:
        x, z = nearest(*tile(c, r), key)
        items.append({"key": key, "x": round(x, 3), "z": round(z, 3), "item": "POKé BALL" if "poke_ball" in key else "ANTIDOTE" if "antidote" in key else "POTION"})

    # The signs: A in front of one reads it.
    acts = []
    for (sx, sz), lines in SIGNS.items():
        assert base(sx, FLOOR, sz) == "pale_oak_shelf" and base(sx + 1, FLOOR, sz) == "pale_oak_shelf", (sx, sz)
        acts.append({"rect": [sx - 0.1, sz - 0.1, sx + 2.1, sz + 1.1], "label": "Read", "lines": lines})

    # The tall grass, as boxes (for wild POKéMON to appear in): each row's runs, joined down where they line up.
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
    rects, open_r = [], {}
    for z in range(L + 1):
        now = set(rows.get(z, []))
        for run in list(open_r):
            if run not in now:
                z0 = open_r.pop(run); rects.append([run[0], z0, run[1], z])
        for run in now:
            if run not in open_r: open_r[run] = z
    grass_cells = sum((r[2] - r[0]) * (r[3] - r[1]) for r in rects)

    # Moonlight: faint lamps (light level 6, unseen) over the open ground every 6 blocks, so it's a night you can see in.
    lights = []
    for z in range(3, L, 6):
        for x in range(3, W, 6):
            for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1)):
                if open_(x + dx, z + dz):
                    lights.append([x + dx, z + dz]); break

    room = {
        "origin": list(ORIGIN), "W": W, "D": L, "H": H - FLOOR, "name": "Viridian Forest", "floor": "", "place": "viridian_forest",
        "schem": "viridian_forest.bin", "base": -FLOOR, "dark": True, "tile": [4 / 3.0, 2.0],
        "spawn": FROM_SOUTH, "entry": FROM_SOUTH, "cam": {"x": W / 2, "z": L / 2, "dist": 30},
        "cells": [],
        "warps": [
            {"kind": "mat", "x0": SOUTH_DOOR["x0"], "x1": SOUTH_DOOR["x1"], "z": SOUTH_DOOR["z"], "door": None, "to": "gate_s", "spawn": GATE_S_FROM_FOREST},
            {"kind": "north", "x0": NORTH_DOOR["x0"], "x1": NORTH_DOOR["x1"], "z": NORTH_DOOR["z"], "door": None, "to": "gate_n", "spawn": GATE_N_FROM_FOREST},
        ],
        "people": people, "acts": acts, "grass": rects, "items": items, "lights": lights, "pools": [], "panes": [], "clocks": [],
    }
    os.makedirs(RES, exist_ok=True)
    json.dump({"rooms": {"viridian_forest": room}}, open(os.path.join(RES, "viridian_forest.json"), "w"), indent=1)

    # The blocks: the palette, then (count, palette index) runs over y, then z, then x.
    palette, pid = [], {}
    seq = []
    for n in names:
        if n not in pid:
            pid[n] = len(palette); palette.append(n)
        seq.append(pid[n])
    out = io.BytesIO()
    out.write(b"RMF1")
    out.write(struct.pack(">hhhh", W, H, L, len(palette)))
    for n in palette:
        b = n.encode("utf-8"); out.write(struct.pack(">h", len(b))); out.write(b)

    def varint(v):
        while True:
            c = v & 0x7F; v >>= 7
            if v: out.write(bytes([c | 0x80]))
            else:
                out.write(bytes([c])); return
    i, runs = 0, 0
    while i < len(seq):
        j = i
        while j < len(seq) and seq[j] == seq[i]: j += 1
        varint(j - i); varint(seq[i]); runs += 1; i = j
    with gzip.open(os.path.join(RES, "viridian_forest.bin"), "wb") as f: f.write(out.getvalue())
    print("blocks", len(seq), "palette", len(palette), "runs", runs, "bin", os.path.getsize(os.path.join(RES, "viridian_forest.bin")), "bytes")
    print("grass boxes", len(rects), "cells", grass_cells, " moonlight lamps", len(lights))

    # The Interior Viewer's copy: for each cell its ground and what stands on it, and how high.
    #   ground: g grass, p path, m moss;  on it: . nothing, " tall grass, * flowers, W a tree's trunk, T its leaves, U gatehouse, S sign
    cells = []
    for z in range(L):
        row = []
        for x in range(W):
            g = {"grass_block": "g", "dirt_path": "p", "moss_block": "m"}.get(base(x, FLOOR - 1, z), "g")
            top = 0
            for y in range(H - 1, FLOOR - 1, -1):
                if base(x, y, z) != "air":
                    top = y - FLOOR + 1; break
            b2 = base(x, FLOOR, z)
            gate = any(base(x, y, z) in ("polished_deepslate", "andesite", "polished_andesite", "birch_planks", "stripped_oak_log", "stripped_jungle_log",
                                         "polished_andesite_slab", "polished_andesite_stairs", "birch_stairs", "deepslate_tiles", "polished_deepslate_slab",
                                         "light_blue_stained_glass_pane", "black_wool", "stone", "stripped_spruce_wood")
                       for y in range(FLOOR, H))
            if gate: what = "U"
            elif b2 == "pale_oak_shelf": what = "S"
            elif not open_(x, z) and top > 0: what = "W" if b2 == "oak_wood" else "T"
            elif b2 in GRASS: what = '"'
            elif b2 in ("poppy", "red_tulip"): what = "*"
            else: what = "."
            row.append(g + what + chr(48 + min(top, 40)))
        cells.append("".join(row))
    js = ("// Viridian Forest as the mod has it (tools/forest.py, from John's schematic): a row of cells for each z, three\n"
          "// characters a cell (ground, what stands there, its height), and where the people, trainers, items and signs are.\n"
          "const FOREST = " + json.dumps({"W": W, "D": L, "rows": cells, "people": people, "items": items, "signs": [[sx, sz, l] for (sx, sz), l in SIGNS.items()],
                                         "fromSouth": FROM_SOUTH, "fromNorth": FROM_NORTH, "southDoor": SOUTH_DOOR, "northDoor": NORTH_DOOR},
                                        separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")
    for p in people: print("  %-28s %6.2f %6.2f yaw %5.1f" % (p["key"], p["x"], p["z"], p["yaw"]))
    for it in items: print("  %-28s %6.2f %6.2f" % (it["key"], it["x"], it["z"]))
    for k, v in (("from south", FROM_SOUTH), ("from north", FROM_NORTH)):
        print("  %s open: %s" % (k, open_(int(v["x"]), int(v["z"]))))


main()
