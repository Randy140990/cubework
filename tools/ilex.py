#!/usr/bin/env python3
"""
The ILEX FOREST as John built it (tools/ilex/ilex_forest.schem, 152 x 22 x 224, x east and z south: HeartGold's forest,
64 by 96 of its tiles, two blocks a tile, his 4 x 4 trees, the pond, the shrine, the sign and the two gatehouses), made
into the mod's forest: a room of its own in the interiors, as Viridian Forest is (tools/forest.py).

The way through (17:07: "from when you go into Azalea Town and you go in and then you come out of the Ilex Forest
gate, on Route 34 is towards Goldenrod"):
  - AZALEA TOWN's west gate in John's world (its doorway on the town side, the east) comes out in the forest in front
    of the south gatehouse, facing north; up its steps and you're back out of Azalea's gate;
  - the north gatehouse's steps go out of ROUTE 34's Ilex Forest gate in John's world (its doorway on the route side,
    the north), and that gate's doorway comes back in, in front of the north gatehouse, facing south.

Where things are: HeartGold's map (pret/pokeheartgold, files/fielddata/eventdata/zone_event/114_D36R0101.json) laid
out at two blocks a tile, its tile (c, r) the blocks from (16 + 2c, 12 + 2r): a tile's middle is (17 + 2c, 13 + 2r).
The people, items, hidden items, the sign and the shrine stand where HeartGold has them (each moved to the nearest
open ground if a tree's there); its words are HeartGold's (msg_0115_D36R0101), in the first games' capitals. Its tall
grass is the light green patches (John's moss).

Writes:
  src/main/resources/retromon/interiors/ilex_forest.bin   its blocks (palette, then runs), gzipped
  src/main/resources/retromon/interiors/ilex_forest.json  the room and its two doors in John's world
  tools/interior/ilex_data.js                             the same forest for the Interior Viewer (a block a cell)

    python3 -I tools/ilex.py
"""
import gzip, io, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402

SRC = os.path.join(HERE, "ilex", "ilex_forest.schem")
RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "ilex_data.js")

# Where the forest sits in the interiors (past every other room).
ORIGIN = (10240, 64, 0)
# The schematic's y 4 is the dirt, 5 the ground (grass, moss): you stand at y 6, the room's floor.
FLOOR = 6
SOFT = {"air", "tall_grass", "large_fern", "short_grass", "fern", "light"}


def tile(c, r):
    return 17.0 + 2 * c, 13.0 + 2 * r


# HeartGold's facing (0 up, 1 down, 2 left, 3 right) as the mod's yaw.
YAW = {0: 180.0, 1: 0.0, 2: 90.0, 3: 270.0}

# HeartGold's people in the forest (the ones there for a visit through, not its later events), with its words.
PEOPLE = [
    ("ilex_forest_apprentice", "APPRENTICE", "youngster", (15, 64), 1, [
        "Oh, man... My boss is going to be angry...",
        "The FARFETCH'D that cut trees for charcoal took off in the forest.",
        "Can you catch them for me? FARFETCH'D have sensitive ears.",
        "If you step on a branch, they will face toward you. That's your chance.",
        "Sneak up from behind and catch them..."]),
    ("ilex_forest_charcoal_man", "CHARCOAL MAN", "gramps", (11, 75), 0, [
        "Do you want to apprentice as a charcoal maker with me?",
        "You'll be first-rate in ten years!"]),
    ("ilex_forest_headbutt_man", "BLACK BELT", "black_belt", (36, 37), 1, [
        "What am I doing?",
        "I'm shaking trees using HEADBUTT.",
        "It's fun. Try it with your POKéMON, too!"]),
]
# The lost FARFETCH'D (HeartGold's first, by the shrine's path): it stands in the forest.
MONS = [("pet:ilex_forest_farfetchd", "farfetchd", (25, 62), 3)]
# The item balls (data/field_items.tsv's keys) and HeartGold's hidden items (found by looking at the spot).
ITEMS = [("ilex_forest_revive", "revive", (42, 73)), ("ilex_forest_x_attack", "x_attack", (27, 42)),
         ("ilex_forest_antidote", "antidote", (20, 30)), ("ilex_forest_ether", "ether", (47, 16))]
HIDDEN = [("ilex_forest_hidden_antidote", "antidote", (15, 37)), ("ilex_forest_hidden_super_potion", "super_potion", (36, 26)),
          ("ilex_forest_hidden_full_heal", "full_heal", (39, 12)), ("ilex_forest_hidden_ether", "ether", (33, 14)),
          ("ilex_forest_hidden_tinymushroom", "tinymushroom", (50, 30)), ("ilex_forest_hidden_repel", "repel", (28, 26)),
          ("ilex_forest_hidden_big_mushroom", "big_mushroom", (10, 32)), ("ilex_forest_hidden_tinymushroom_2", "tinymushroom", (53, 48)),
          ("ilex_forest_hidden_tinymushroom_3", "tinymushroom", (42, 17))]
SIGN = ["TRAINER TIPS!", "ILEX FOREST is so overgrown with trees that you can't see the sky.",
        "Please watch out for items that may have been dropped."]
SHRINE = ["ILEX FOREST SHRINE...", "It's a tribute to the forest's protector..."]

# The gatehouses' doorways in the schematic (the Route 2 forest gate's, as Viridian Forest's): the north one faces
# south (its steps x 39 to 44, z 47 to 48, the dark back at z 43), the south one faces north (its steps x 37 to 42,
# z 180 to 181, the dark back at z 185).
NORTH_DOOR = dict(x0=39.0, x1=45.0, z=44.6, s0=46, s1=48, back=43)
SOUTH_DOOR = dict(x0=37.0, x1=43.0, z=184.0, s0=180, s1=182, back=185)
FROM_NORTH = {"x": 42.0, "z": 49.8, "yaw": 0.0}
FROM_SOUTH = {"x": 40.0, "z": 178.6, "yaw": 180.0}

# The gates in John's world: Route 34's Ilex Forest gate (its doorway on the route side, the north: the dark back at
# z 853, up a step from the path) and Azalea Town's west gate (its doorway on the town side, the east: the dark back at
# x -2049, up a step). The forest sides of both are walled in by trees.
DOORS = {
    "ilex_route_34": {"room": "ilex_forest", "door": [[x, 22, 853] for x in range(-2075, -2071)], "out": [-2073.0, 21.0, 847.4], "face": "n", "yaw": 180.0,
                      "in": FROM_NORTH},
    "ilex_azalea": {"room": "ilex_forest", "door": [[-2049, 22, 865], [-2049, 22, 866]], "out": [-2043.6, 21.0, 866.0], "face": "e", "yaw": -90.0,
                    "in": FROM_SOUTH},
}


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

    # The gatehouses' doorways from inside the forest: a dark way in with daylight at its end (as Viridian Forest's,
    # 15:21): the black at the back of each, lit.
    lit = 0
    for zb, zin in ((SOUTH_DOOR["back"], (SOUTH_DOOR["back"] - 1, SOUTH_DOOR["back"] - 2)), (NORTH_DOOR["back"], (NORTH_DOOR["back"] + 1, NORTH_DOOR["back"] + 2))):
        for x in range(W):
            for y in range(FLOOR + 2, FLOOR + 6):
                if base(x, y, zb) == "black_wool":
                    names[idx(x, y, zb)] = "minecraft:white_concrete"; lit += 1
        for x in range(W):
            if base(x, FLOOR + 3, zb) == "white_concrete" and base(x, FLOOR + 2, zin[0]) == "air":
                names[idx(x, FLOOR + 2, zin[0])] = "minecraft:light[level=15,waterlogged=false]"
                if base(x, FLOOR + 3, zin[1]) == "air": names[idx(x, FLOOR + 3, zin[1])] = "minecraft:light[level=10,waterlogged=false]"
    print("doorway daylight blocks:", lit)

    def open_(x, z):
        return 0 <= x < W and 0 <= z < L and base(x, FLOOR - 1, z) not in SOFT and base(x, FLOOR - 1, z) != "water" and base(x, FLOOR, z) in SOFT \
            and base(x, FLOOR + 1, z) in SOFT

    def nearest(fx, fz, label):
        cx, cz = int(fx), int(fz)
        if open_(cx, cz) and open_(cx - 1, cz) and open_(cx, cz - 1) and open_(cx - 1, cz - 1): return fx, fz
        best = None
        for r in range(0, 6):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    if max(abs(dx), abs(dz)) != r or not open_(cx + dx, cz + dz): continue
                    d = (cx + dx + 0.5 - fx) ** 2 + (cz + dz + 0.5 - fz) ** 2
                    if best is None or d < best[0]: best = (d, cx + dx + 0.5, cz + dz + 0.5)
            if best: break
        if best is None: raise SystemExit("nowhere open near %s" % label)
        print("  %s moved to open ground: (%.2f, %.2f) -> (%.2f, %.2f)" % (label, fx, fz, best[1], best[2]))
        return best[1], best[2]

    people = []
    for key, name, skin, (c, r), face, lines in PEOPLE:
        x, z = nearest(*tile(c, r), key)
        people.append({"key": key, "name": name, "skin": skin, "x": round(x, 3), "z": round(z, 3), "yaw": YAW[face], "lines": lines})
    mons = []
    for key, species, (c, r), face in MONS:
        x, z = nearest(*tile(c, r), key)
        mons.append({"key": key, "species": species, "x": round(x, 3), "z": round(z, 3), "yaw": YAW[face]})
    items = []
    for key, item, (c, r) in ITEMS:
        x, z = nearest(*tile(c, r), key)
        items.append({"key": key, "x": round(x, 3), "z": round(z, 3), "item": item})

    acts = []
    # The sign: John's two shelves (where HeartGold has it), read from in front.
    shelves = sorted({(x, z) for z in range(L) for x in range(W) if base(x, FLOOR, z) == "pale_oak_shelf"})
    sx, sz = shelves[0]
    acts.append({"rect": [sx - 0.1, sz - 0.1, sx + 2.1, sz + 1.6], "label": "Read", "lines": SIGN})
    # The shrine (its stone base, its roof): looked at from in front, the south.
    sh = [(x, z) for z in range(L) for x in range(W) if base(x, FLOOR, z) == "stone_bricks"]
    sx0, sx1 = min(p[0] for p in sh), max(p[0] for p in sh) + 1
    sz1 = max(p[1] for p in sh) + 1
    acts.append({"rect": [sx0 - 0.2, sz1 - 0.2, sx1 + 0.2, sz1 + 1.6], "label": "Look", "lines": SHRINE})
    # The hidden items: look at the ground there (the mod's "hidden:" use gives the item, once).
    hidden = []
    for key, item, (c, r) in HIDDEN:
        x, z = nearest(*tile(c, r), key)
        acts.append({"rect": [x - 0.9, z - 0.9, x + 0.9, z + 0.9], "label": "Look", "lines": None, "use": "hidden:%s:%s" % (item, key)})
        hidden.append({"key": key, "x": round(x, 3), "z": round(z, 3), "item": item})

    # The tall grass, as boxes: the light green patches (John's moss), each row's runs, joined down where they line up.
    rows = {}
    for z in range(L):
        x = 0
        while x < W:
            if base(x, FLOOR - 1, z) == "moss_block" and open_(x, z):
                x0 = x
                while x < W and base(x, FLOOR - 1, z) == "moss_block" and open_(x, z): x += 1
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

    # Light under the trees: faint lamps (level 6, unseen) over the open ground every 6 blocks (it's dim, never dark).
    lights = []
    for z in range(3, L, 6):
        for x in range(3, W, 6):
            for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1)):
                if open_(x + dx, z + dz):
                    lights.append([x + dx, z + dz]); break

    for k, v in (("from the south", FROM_SOUTH), ("from the north", FROM_NORTH)):
        assert open_(int(v["x"]), int(v["z"])), k
    room = {
        "origin": list(ORIGIN), "W": W, "D": L, "H": H - FLOOR, "name": "Ilex Forest", "floor": "", "place": "ilex_forest",
        "schem": "ilex_forest.bin", "base": -FLOOR, "dark": True, "tile": [2.0, 2.0],
        "spawn": FROM_SOUTH, "entry": FROM_SOUTH, "cam": {"x": W / 2, "z": L / 2, "dist": 30},
        "cells": [],
        "warps": [
            {"kind": "mat", "x0": SOUTH_DOOR["x0"], "x1": SOUTH_DOOR["x1"], "z": SOUTH_DOOR["z"], "door": "ilex_azalea", "to": None},
            {"kind": "north", "x0": NORTH_DOOR["x0"], "x1": NORTH_DOOR["x1"], "z": NORTH_DOOR["z"], "door": "ilex_route_34", "to": None},
        ],
        "people": people, "acts": acts, "grass": rects, "items": items, "lights": lights, "pools": [], "panes": [], "clocks": [],
        "mons": mons,
    }
    os.makedirs(RES, exist_ok=True)
    json.dump({"rooms": {"ilex_forest": room}, "doors": DOORS}, open(os.path.join(RES, "ilex_forest.json"), "w"), indent=1)

    # The blocks: the palette, then (count, palette index) runs over y, then z, then x.
    palette, pid, seq = [], {}, []
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
    with gzip.open(os.path.join(RES, "ilex_forest.bin"), "wb") as f: f.write(out.getvalue())
    print("blocks", len(seq), "palette", len(palette), "runs", runs, "bin", os.path.getsize(os.path.join(RES, "ilex_forest.bin")), "bytes")
    print("grass boxes", len(rects), "cells", grass_cells, " lamps", len(lights))

    # The Interior Viewer's copy: for each cell its ground and what stands on it, and how high.
    #   ground: g grass, m moss, ~ water, d dripstone;  on it: . nothing, W a tree's trunk, T its leaves, U gatehouse,
    #   S sign, H the shrine
    GATE = {"polished_deepslate", "andesite", "polished_andesite", "stripped_oak_log", "stripped_jungle_log", "polished_andesite_slab",
            "polished_andesite_stairs", "birch_stairs", "polished_deepslate_slab", "light_blue_stained_glass_pane", "black_wool", "white_concrete",
            "stone", "stripped_spruce_wood", "light_gray_concrete", "cyan_terracotta", "deepslate_tile_wall", "andesite_wall", "stripped_warped_stem",
            "deepslate_brick_wall", "warped_trapdoor", "jungle_stairs"}
    SHR = {"stone_bricks", "dark_oak_planks", "mangrove_planks", "mangrove_stairs", "mangrove_slab", "barrel", "spruce_fence"}
    cells = []
    for z in range(L):
        row = []
        for x in range(W):
            g0 = base(x, FLOOR - 1, z)
            g = {"grass_block": "g", "moss_block": "m", "water": "~", "dripstone_block": "d"}.get(g0, "g")
            top = 0
            for y in range(H - 1, FLOOR - 1, -1):
                if base(x, y, z) not in ("air", "light"):
                    top = y - FLOOR + 1; break
            col = {base(x, y, z) for y in range(FLOOR, H)}
            b2 = base(x, FLOOR, z)
            if col & GATE: what = "U"
            elif b2 == "pale_oak_shelf": what = "S"
            elif col & SHR: what = "H"
            elif top > 0 and not open_(x, z): what = "W" if b2 == "oak_wood" else "T"
            else: what = "."
            row.append(g + what + chr(48 + min(top, 40)))
        cells.append("".join(row))
    js = ("// The ILEX FOREST as the mod has it (tools/ilex.py, from John's schematic): a row of cells for each z, three\n"
          "// characters a cell (ground, what stands there, its height), and where the people, items and signs are.\n"
          "const ILEX = " + json.dumps({"W": W, "D": L, "rows": cells, "people": people, "mons": mons, "items": items, "hidden": hidden,
                                       "acts": acts, "fromSouth": FROM_SOUTH, "fromNorth": FROM_NORTH, "southDoor": SOUTH_DOOR, "northDoor": NORTH_DOOR},
                                      separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")
    for p in people + mons: print("  %-34s %6.2f %6.2f" % (p["key"], p["x"], p["z"]))
    for it in items + hidden: print("  %-34s %6.2f %6.2f %s" % (it["key"], it["x"], it["z"], it["item"]))


main()
