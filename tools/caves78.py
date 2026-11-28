#!/usr/bin/env python3
"""
(0.78, John's 28 Nov 14:54: "Ice cave, ice path and dark cave are available for you to integrate, and obviously
understanding the mechanics behind them and ladders up and down, etc. I'd like you to sort them out for me.")

The DARK CAVE and the ICE PATH as HeartGold has them (tools/darkcave/*.schem, tools/icepath/*.schem: the floors built
from PokéMaps' HeartGold maps, an HGSS tile two blocks by two, a level three blocks up), made into rooms of their own
in the interiors as the other caves are (tools/caves48.py does the building), and stitched together by HeartGold's
warps (pret/pokeheartgold's zone_event D42R0101 and D42R0102, D39R0101 to D39R0104):

  DARK CAVE  the Violet side (HeartGold's ROUTE 31 side): in from ROUTE 31 (its mouth by Violet, at the top of the
             cave's west corridor) and from ROUTE 46 (bottom right); through the doorway past the big pool (swum, as
             SURF) to the Blackthorn side (HeartGold's ROUTE 45 side), and out at its top right onto ROUTE 45. Dark:
             FLASH lights it, as ROCK TUNNEL.
  ICE PATH   1F in from ROUTE 44, out to BLACKTHORN CITY; ladders down to B1F; B1F's four STRENGTH boulders and
             four holes (fall through to B2F; push a boulder in and it falls through too, and lies on B2F's ice where
             it's needed to stop on); B2F (the Mahogany side, its ice), B3F (its ROCK SMASH rock) and B2F's Blackthorn
             side, back up to B1F's lower part and 1F's way out. Slide on the ice (the blue ice) till you hit something.

HeartGold's tiles here are the schematics' own (block // 2): its warps and things are moved onto them by hand (each
floor's offset worked out from its ways through). Writes src/main/resources/retromon/interiors/dark_cave.json and
ice_path.json (rooms and doors), a .bin of blocks for each floor, tools/interior/caves78_data.js for the Interior
Viewer, and the boulders' rows in data/placeables.tsv and data/placed.tsv (after tools/fr_placeables.py).

    python3 -I tools/caves78.py
"""
import json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402
import caves48  # noqa: E402

RES = caves48.RES
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
VIEWER = os.path.join(HERE, "interior", "caves78_data.js")
SMASH = caves48.SMASH

# The world's mouths (John's 12:43 save): the black at the back of each doorway (you walk north into it, all five
# face south), and where you come out (on the ground in front, facing south).
MOUTHS = {
    "dark_cave_route_31": {"door": [[-1692, 25, 557], [-1691, 25, 557]], "out": [-1691.0, 25.0, 561.6]},
    "dark_cave_route_46": {"door": [[-1565, 21, 652], [-1564, 21, 652]], "out": [-1564.0, 21.0, 656.6]},
    "dark_cave_route_45": {"door": [[-1516, 31, 416], [-1515, 31, 416]], "out": [-1515.0, 31.0, 420.6]},
    "ice_path_route_44": {"door": [[-1566, 25, 400], [-1565, 25, 400]], "out": [-1565.0, 25.0, 404.6]},
    "ice_path_blackthorn": {"door": [[-1414, 35, 324], [-1413, 35, 324]], "out": [-1413.0, 35.0, 328.6]},
}

DARK_ROCKS = {"brown_mushroom_block", "mushroom_stem", "warped_wart_block", "dark_prismarine_slab", "andesite_stairs", "andesite_wall", "diorite_slab",
              "cobblestone"}
ICE_ROCKS = {"snow_block", "packed_ice"}

# Ice Path B1F: HeartGold's four holes (its (11,10), (10,18), (18,7), (19,19), four tiles in from the schematic's
# corner), where you land on B2F's Mahogany side when you fall (its (12,12), (10,21), (20,10), (21,23), two in), and
# where the boulder pushed into each lies on B2F (its (12,13), (11,21), (19,10), (20,22)): "It has sunk in and cannot
# be moved." The schematic had the boulders already in the holes and the fallen ones on B2F as crystals; both are
# taken out here, the boulders start next to their holes (one push each: the schematic's crystal maze is a tracing,
# and from HeartGold's own starts no boulder could reach its hole), and the fallen ones lie on B2F once they've fallen.
HOLES = [  # (key, B1F tile, B2F landing tile, B2F fallen tile)
    ("ice_path_b1f_hole_1", (15, 14), (14, 12), (14, 13)),
    ("ice_path_b1f_hole_2", (14, 22), (12, 21), (13, 21)),
    ("ice_path_b1f_hole_3", (22, 11), (22, 10), (21, 10)),
    ("ice_path_b1f_hole_4", (23, 23), (23, 23), (22, 22)),
]
B1F_BOULDERS = [(15, 13), (15, 22), (23, 11), (23, 22)]
# B2F Mahogany side's crystal lumps where the fallen boulders were traced (block x0, x1, z0, z1), taken out to ice.
B2F_LUMPS = [(28, 29, 26, 29), (26, 27, 42, 45), (42, 43, 20, 23), (44, 45, 44, 47)]
# The Violet side's two doorways at the top of its east corridors (the map ran off the screen there; HeartGold has no
# way out there): walled up.
DC_BLIND = [(88, 91), (110, 113)]

CAVES = {
    "dark_cave": {
        "json": "dark_cave.json", "name": "Dark Cave",
        "src": [("darkcave", "dark_cave_violet_entrance.schem"), ("darkcave", "dark_cave_blackthorn_entrance.schem")],
        "floors": [("dark_cave_violet_entrance", "Violet side", 0), ("dark_cave_blackthorn_entrance", "Blackthorn side", 0)],
        "W": None, "floor": 7, "up": 12, "ups": [12, 6], "origin0": 19456, "step": 256, "off": (0, 0),
        "rocks": DARK_ROCKS, "boulders": {}, "swim": True, "haze": [52, 58, 78, 0.16], "light": 6,
        "links": [], "falls": [],
        "doors": {
            "dark_cave_route_31": dict(MOUTHS["dark_cave_route_31"], mat=(16, 18, 60.55), **{"in": (17.0, 58.6)}),
            "dark_cave_route_46": dict(MOUTHS["dark_cave_route_46"], mat=(116, 118, 126.55), **{"in": (117.0, 124.6)}),
            "dark_cave_route_45": dict(MOUTHS["dark_cave_route_45"], floor=1),
        },
        "trainers": {},
        # HeartGold's items (its hidden ones left out, as the other caves'; its TM54 and BLACK FLUTE left out: FALSE
        # SWIPE isn't one of the mod's TMs, nor the flute one of its items), on the schematic's tiles (the Violet side's HeartGold (x, z) is about (x + 3, z + 3) at the
        # top, (x + 3, z + 6) at the bottom; the Blackthorn side's (x + 4, z + 3) to (x + 4, z + 6)).
        "items": {0: [("dark_cave_violet_entrance_potion", "potion", 13, 16), ("dark_cave_violet_entrance_hyper_potion", "hyper_potion", 54, 18),
                      ("dark_cave_violet_entrance_full_heal", "full_heal", 59, 48), ("dark_cave_violet_entrance_dire_hit", "dire_hit", 49, 57)],
                  1: [("dark_cave_blackthorn_entrance_revive", "revive", 46, 49)]},
        "mons": {},
        "signs": {},
    },
    "ice_path": {
        "json": "ice_path.json", "name": "Ice Path",
        "src": [("icepath", "ice_path_1f.schem"), ("icepath", "ice_path_b1f.schem"), ("icepath", "ice_path_b2f_mahogany_side.schem"),
                ("icepath", "ice_path_b2f_blackthorn_side.schem"), ("icepath", "ice_path_b3f.schem")],
        "floors": [("ice_path_1f", "1F", 0), ("ice_path_b1f", "B1F", 0), ("ice_path_b2f_mahogany_side", "B2F", 0),
                   ("ice_path_b2f_blackthorn_side", "B2F", 0), ("ice_path_b3f", "B3F", 0)],
        "W": None, "floor": 7, "up": 6, "ups": [6, 0, 0, 3, 3], "origin0": 19968, "step": 256, "off": (0, 0),
        "rocks": ICE_ROCKS, "boulders": {"cobbled_deepslate": SMASH}, "haze": [176, 208, 236, 0.10], "light": 11,
        # HeartGold's ladders (hole tile on the floor above, ladder tile on the floor below; the schematics' tiles).
        "links": [((0, 53, 11), (1, 10, 25)), ((0, 53, 24), (1, 10, 46)), ((1, 29, 12), (2, 28, 11)), ((1, 31, 44), (3, 8, 27)),
                  ((2, 18, 17), (4, 9, 15)), ((3, 15, 10), (4, 28, 16))],
        "falls": [((1,) + h[1], (2,) + h[2]) for h in HOLES],
        "doors": {
            "ice_path_route_44": dict(MOUTHS["ice_path_route_44"], mat=(18, 20, 126.55), **{"in": (19.0, 124.6)}),
            "ice_path_blackthorn": dict(MOUTHS["ice_path_blackthorn"], mat=(106, 108, 90.55), **{"in": (107.0, 88.6)}),
        },
        "trainers": {},
        # HeartGold's items (1F's HeartGold (x, z) is (x - 2, z + 4) here, B1F's and B3F's (x + 4, z + 4), B2F's
        # Mahogany side (x + 2, z), its Blackthorn side (x - 35, z + 5); B2F's TM72 left out: AVALANCHE isn't one of
        # the mod's moves).
        "items": {0: [("ice_path_1f_hm_waterfall", "hm_waterfall", 45, 14), ("ice_path_1f_protein", "protein", 51, 19), ("ice_path_1f_pp_up", "pp_up", 51, 39)],
                  1: [("ice_path_b1f_iron", "iron", 17, 52)],
                  2: [("ice_path_b2f_full_heal", "full_heal", 17, 15), ("ice_path_b2f_max_potion", "max_potion", 7, 12)],
                  4: [("ice_path_b3f_nevermeltice", "nevermeltice", 16, 17)]},
        "mons": {},
        "signs": {},
    },
}

# HeartGold's people (its words, msg_0122 and msg_0127): the man who dropped his HM on 1F's ice, the man in BLACKGLASSES
# in the Dark Cave's Blackthorn side.
PEOPLE = {
    "ice_path_1f": [{"key": "ice_path_1f_p0", "name": "GENTLEMAN", "skin": "gentleman", "t": (45, 11), "yaw": 0.0,
                     "lines": ["You see the item over there?", "It was me who dropped it, but I cannot get over there to pick it up.", "Why?",
                               "Walking on ice makes your feet freeze!", "My shoes are so worn that the soles are gone!"]}],
    "dark_cave_blackthorn_entrance": [{"key": "dark_cave_blackthorn_entrance_p0", "name": "SUPER NERD", "skin": "super_nerd", "t": (20, 15), "yaw": 0.0,
                                       "lines": ["Whoa! You startled me there!", "I had my BLACKGLASSES on, so I didn't notice you at all.",
                                                 "What am I doing here?", "Hey, don't you worry about it.",
                                                 "I'll give you a pair of BLACKGLASSES, so forget you saw me, OK?"]}],  # (his BLACKGLASSES: data/gifts.tsv)
}


def edit_floor(key, k, W, H, L, names):
    """The changes to John's blocks before the building (see the notes above)."""
    names = list(names)
    idx = lambda x, y, z: (y * L + z) * W + x
    base = lambda x, y, z: names[idx(x, y, z)].replace("minecraft:", "").split("[")[0]
    if key == "dark_cave" and k == 0:
        for x0, x1 in DC_BLIND:
            for x in range(x0, x1 + 1):
                for y in range(H):
                    for z in range(10, 17):
                        b = base(x, y, z)
                        if b == "black_concrete": names[idx(x, y, z)] = "minecraft:dripstone_block"
                        elif b == "smooth_sandstone": names[idx(x, y, z)] = "minecraft:cyan_terracotta"
    if key == "ice_path" and k == 1:
        for _key, (tx, tz), _l, _f in HOLES:
            for x in (2 * tx, 2 * tx + 1):
                for z in (2 * tz, 2 * tz + 1):
                    for y in range(3, 10): names[idx(x, y, z)] = "minecraft:air"
                    names[idx(x, 2, z)] = "minecraft:black_concrete"
        # The crystal where the second boulder starts (one tile of a lump).
        for x in (30, 31):
            for z in (44, 45):
                for y in range(7, 10): names[idx(x, y, z)] = "minecraft:air"
    if key == "ice_path" and k == 2:
        for x0, x1, z0, z1 in B2F_LUMPS:
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    for y in range(7, 11): names[idx(x, y, z)] = "minecraft:air"
                    names[idx(x, 6, z)] = "minecraft:blue_ice"
    if key == "ice_path" and k == 4:
        # B3F's ROCK SMASH rock: built where HeartGold has the NEVERMELTICE (its (12, 13)); moved a tile north, so it
        # stands between the room and the item's nook, as HeartGold's does.
        for x in (32, 33):
            for y in (7, 8):
                names[idx(x, y, 32)] = names[idx(x, y, 34)]
                names[idx(x, y, 33)] = names[idx(x, y, 35)]
                names[idx(x, y, 34)] = names[idx(x, y, 35)] = "minecraft:air"
    return names


_floors_of = caves48.floors_of


def floors_of(cfg):
    key = next(k for k, c in CAVES.items() if c is cfg)
    out = []
    for k, (W, H, L, names) in enumerate(_floors_of(cfg)):
        out.append((W, H, L, edit_floor(key, k, W, H, L, names)))
    return out


def smash_rocks(rid, W, H, L, names):
    """The ROCK SMASH rocks on a floor (the smashable grates, two by two by two): each one's blocks' box."""
    pts = set()
    for y in range(H):
        for z in range(L):
            for x in range(W):
                if names[(y * L + z) * W + x].replace("minecraft:", "").split("[")[0] == "cobbled_deepslate": pts.add((x, y, z))
    out, seen = [], set()
    for p in sorted(pts):
        if p in seen: continue
        st, g = [p], []
        while st:
            q = st.pop()
            if q in seen or q not in pts: continue
            seen.add(q); g.append(q)
            for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)): st.append((q[0] + d[0], q[1] + d[1], q[2] + d[2]))
        xs, ys, zs = [q[0] for q in g], [q[1] for q in g], [q[2] for q in g]
        out.append([min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)])
    return out


def main():
    caves48.CAVES.clear()
    caves48.CAVES.update(CAVES)
    caves48.floors_of = floors_of
    viewer = {}
    for key, cfg in CAVES.items():
        v = caves48.build(key)
        path = os.path.join(RES, cfg["json"])
        J = json.load(open(path))
        rooms, doors = J["rooms"], J["doors"]
        raw = floors_of(cfg)
        for k, (rid, _fl, _x) in enumerate(cfg["floors"]):
            R = rooms[rid]
            W, H, L, names = raw[k]
            # Ladders and holes are as caves48 made them; the room's own things here.
            if key == "ice_path":
                R["ice"] = True
                R["dark"] = True
            if key == "dark_cave":
                R["flash"] = True
            for pp in PEOPLE.get(rid, []):
                tx, tz = pp["t"]
                x, z = 2 * tx + 1.0, 2 * tz + 1.0
                person = {"key": pp["key"], "name": pp["name"], "skin": pp["skin"], "x": x, "z": z, "yaw": pp["yaw"], "lines": pp["lines"]}
                R["people"].append(person)
            sm = smash_rocks(rid, W, H, L, names)
            if sm:
                R["smash"] = [[b[0], b[1] - cfg["floor"], b[2], b[3] + 1, b[4] + 1 - cfg["floor"], b[5] + 1] for b in sm]
                for i, b in enumerate(sm):
                    R["acts"].append({"rect": [b[0] - 0.6, b[2] - 0.6, b[3] + 1.6, b[5] + 1.6], "label": "Look", "lines": ["This rock looks like it can be broken!"],
                                      "use": "smash:%d" % i})
        # The DARK CAVE's own ways through (not ladders): the Violet side's doorway past the pool (its level 3, nine
        # blocks up) to the Blackthorn side's at its bottom left (its floor), both ways; and out at the Blackthorn
        # side's top right (its level 2, six up), north into the doorway, onto ROUTE 45.
        if key == "dark_cave":
            vi, bl = rooms["dark_cave_violet_entrance"], rooms["dark_cave_blackthorn_entrance"]
            vi["warps"].append({"kind": "box", "x0": 58.0, "x1": 60.0, "z0": 11.8, "z1": 13.3, "y": 9.0, "to": "dark_cave_blackthorn_entrance",
                                "spawn": {"x": 21.0, "z": 126.4, "y": 0.0, "yaw": 180.0}})
            bl["warps"].append({"kind": "box", "x0": 20.0, "x1": 22.0, "z0": 128.5, "z1": 129.4, "y": 0.0, "to": "dark_cave_violet_entrance",
                                "spawn": {"x": 59.0, "z": 15.4, "y": 9.0, "yaw": 0.0}})
            bl["warps"].append({"kind": "north", "x0": 128.0, "x1": 130.0, "z": 13.45, "door": "dark_cave_route_45", "to": None})
            doors["dark_cave_route_45"]["in"] = {"x": 129.0, "z": 14.8, "yaw": 0.0, "y": 6.0}
            doors["dark_cave_route_31"]["in"]["y"] = 9.0
            # (Where you're put back if you fall out of a floor: on its lowest level, by its way through.)
            bl["spawn"] = bl["entry"] = {"x": 21.0, "z": 126.4, "yaw": 180.0}
            vi["spawn"] = vi["entry"] = {"x": 117.0, "z": 124.6, "yaw": 180.0}
        # (No mouth group: each way out is its own door.)
        for d in doors.values(): d.pop("group", None)
        if key == "ice_path":
            b1, b2 = rooms["ice_path_b1f"], rooms["ice_path_b2f_mahogany_side"]
            b1["holes"] = [{"key": hk, "x": 2 * t[0] + 1.0, "z": 2 * t[1] + 1.0} for hk, t, _l, _f in HOLES]
            b2["fallen"] = [{"key": hk, "x": 2 * f[0] + 1.0, "z": 2 * f[1] + 1.0} for hk, _t, _l, f in HOLES]
        json.dump({"rooms": rooms, "doors": doors}, open(path, "w"), indent=1)
        for rid, r in v.items():
            r["cave"] = key
            r["warps"] = rooms[rid]["warps"]
            r["people"] = rooms[rid]["people"]
        viewer.update(v)
    # The B1F boulders, for STRENGTH (Boulders.java) and the PLACER: rows after fr_placeables.py's.
    rows_cat, rows_placed = [], []
    for i, (tx, tz) in enumerate(B1F_BOULDERS, 1):
        k = "ice_path_b1f_boulder_%d" % i
        rows_cat.append([k, "ice_path_b1f", "boulder", k, "BOULDER", "-", tx, tz, "none", 0])
        rows_placed.append([k, "boulder", "ice_path_b1f", "ice_path_b1f", "%.2f" % (2 * tx + 1.5), "NaN", "%.2f" % (2 * tz + 1.5), "0", "placed"])
    # The item balls' items (data/field_items.tsv: key, place, item, its tile).
    rows_items = [[k2, cfg["floors"][fk][0], it, tx, tz] for cfg in CAVES.values() for fk, lst in cfg["items"].items() for (k2, it, tx, tz) in lst]
    p = os.path.join(DATA, "field_items.tsv")
    mine = {r[0] for r in rows_items}
    lines = [l for l in open(p, encoding="utf-8").read().split("\n") if l and l.split("\t")[0] not in mine]
    lines += ["\t".join(str(v) for v in r) for r in rows_items]
    open(p, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    for fn, rows in (("placeables.tsv", rows_cat), ("placed.tsv", rows_placed)):
        p = os.path.join(DATA, fn)
        lines = [l for l in open(p, encoding="utf-8").read().split("\n") if l and not l.startswith("ice_path_b1f_boulder_")]
        lines += ["\t".join(str(v) for v in r) for r in rows]
        open(p, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    js = ("// The DARK CAVE and the ICE PATH as the mod has them (tools/caves78.py, from the HeartGold builds): as CAVES48's, each\n"
          "// floor a row of cells for each z, two characters a cell; its people, items and ways in and out.\n"
          "Object.assign(CAVES48, " + json.dumps(viewer, separators=(",", ":")) + ");\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")


if __name__ == "__main__":
    main()
