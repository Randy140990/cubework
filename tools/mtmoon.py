#!/usr/bin/env python3
"""
Mt. Moon as John built it (tools/mtmoon/*.schem: 1F, B1F and B2F as FireRed has them, two blocks a step), made into
rooms of their own in the interiors, as Diglett's Cave is (tools/caves.py):

  mt_moon_1f   in from the mouth by Route 4's POKéMON CENTER (west of the mountain, at the top of Route 3); three
               holes down (each a ladder) into three parts of B1F
  mt_moon_b1f  four parts, none joined to another: each has a ladder up to 1F or a hole down to B2F (the north-east
               part, the way out: its ladder up comes out of the mouth on Route 4, east of the mountain)
  mt_moon_b2f  one floor on two levels (wooden steps between), Team Rocket, SUPER NERD MIGUEL and the two fossils

Which hole goes to which ladder (FireRed's way through): 1F's north-west hole to B1F's west part, its middle hole to
B1F's middle part, its east hole to B1F's south part; B1F's west part down to B2F's north-east ladder, its middle part
to B2F's middle ladder, its south part to B2F's south-west ladder; B2F's north-west ladder (by the fossils) up into
B1F's north-east part, and out.

Where the people and items are: FireRed's (mapgenie's maps, as John marked them), laid over John's floors from the
ladders, each moved to the nearest open floor. Not dark (FireRed's Mt. Moon is lit), but dim, a faint cool haze.

Writes src/main/resources/retromon/interiors/mt_moon.json (the rooms and their doors in John's world), a .bin of
blocks for each room, and tools/interior/mtmoon_data.js for the Interior Viewer.

    python3 -I tools/mtmoon.py
"""
import gzip, io, json, math, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402
import cavecut  # noqa: E402

RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "mtmoon_data.js")
FREE = {"air", "light", "short_grass", "pale_oak_shelf"}
ROCKS = {"granite", "polished_granite", "granite_slab", "polished_granite_slab", "dripstone_block", "white_terracotta"}
YAW = {"down": 0.0, "left": 90.0, "up": 180.0, "right": 270.0}
DIRS = {"down": (0, 1), "up": (0, -1), "left": (-1, 0), "right": (1, 0)}

# The mouths in John's world: the dark back of each doorway (you walk north into it) and where you come out (south of
# it, facing south). The one by the Center is the way in (Route 3's end); the framed one east of the mountain, its
# sign beside it, the way out onto Route 4; the plain one west of that goes in to the same way out.
DOORS = {
    "mt_moon_route_3": {"room": "mt_moon_1f", "door": [[-590, 34, 256], [-589, 34, 256]], "out": [-589.0, 34.0, 259.9]},
    "mt_moon_route_4": {"room": "mt_moon_b1f", "door": [[-554, 34, 256], [-553, 34, 256]], "out": [-553.0, 34.0, 259.9], "group": "mt_moon_exit"},
    "mt_moon_route_4_west": {"room": "mt_moon_b1f", "door": [[-563, 34, 256], [-562, 34, 256]], "out": [-562.0, 34.0, 258.4], "group": "mt_moon_exit"},
}

# id, schematic, origin, floor (the schematic's y you stand on), place, the tile (blocks a step), mapgenie's frame
ROOMS = [
    ("mt_moon_1f", "mt_moon_1f.schem", (5120, 64, 0), 3, "mt_moon_1f"),
    ("mt_moon_b1f", "mt_moon_b1f.schem", (5376, 64, 0), 3, "mt_moon_b1f"),
    ("mt_moon_b2f", "mt_moon_b2f.schem", (5632, 64, 0), 4, "mt_moon_b2f"),
]
TILE = [2.6, 2.5]
HAZE = [92, 96, 128, 0.10]
LIGHT = 9

# mapgenie's maps (John's screenshots) to John's floors: x = (px - a) / s + b, z = (py - c) / t + d, fitted to the ladders.
MAPS = {
    "mt_moon_1f": (527, 8.49, 0.6, 117, 8.2, 1.2),
    "mt_moon_b1f": (713, 8.41, 11.5, 360, 8.36, 10.5),
    "mt_moon_b2f": (719, 8.45, 15.5, 503, 8.29, 24.5),
}

# The trainers (their rows in data/trainers.tsv), FireRed's: key, title, skin, where on mapgenie, facing.
TRAINERS = {
    "mt_moon_1f": [
        ("mt_moon_1f_0", "HIKER MARCOS", "hiker", (663, 300), "down"),
        ("mt_moon_1f_1", "YOUNGSTER JOSH", "youngster", (774, 428), "right"),
        ("mt_moon_1f_2", "LASS MIRIAM", "lass", (1139, 190), "down"),
        ("mt_moon_1f_3", "SUPER NERD JOVAN", "super_nerd", (1085, 757), "up"),
        ("mt_moon_1f_4", "LASS IRIS", "lass_2", (902, 594), "down"),
        ("mt_moon_1f_5", "BUG CATCHER KENT", "bug_catcher", (663, 594), "down"),
        ("mt_moon_1f_6", "BUG CATCHER ROBBY", "bug_catcher_2", (1194, 667), "right"),
    ],
    "mt_moon_b1f": [],
    "mt_moon_b2f": [
        ("mt_moon_b2f_0", "SUPER NERD MIGUEL", "super_nerd_2", (852, 517), "down"),
        ("mt_moon_b2f_1", "TEAM ROCKET GRUNT", "rocket", (835, 667), "up"),
        ("mt_moon_b2f_2", "TEAM ROCKET GRUNT", "rocket_2", (1218, 533), "left"),
        ("mt_moon_b2f_3", "TEAM ROCKET GRUNT", "rocket_3", (1251, 684), "left"),
        ("mt_moon_b2f_4", "TEAM ROCKET GRUNT", "rocket_4", (935, 784), "up"),
    ],
}
# Others: key, name, skin, where on mapgenie, facing, lines.
PEOPLE = {
    "mt_moon_1f": [("mt_moon_1f_p0", "HIKER", "hiker_2", (1305, 262), "left",
                    ["MT. MOON is a quiet place. Or it was, until those suspicious men in black turned up.", "They're digging for something down below."])],
    "mt_moon_b1f": [],
    "mt_moon_b2f": [],
}
# Item balls: key (data/field_items.tsv), where on mapgenie. B1F's mushrooms come back each day (field_items: daily).
ITEMS = {
    "mt_moon_1f": [("mt_moon_1f_moon_stone", (591, 157)), ("mt_moon_1f_parlyz_heal", (571, 522)), ("mt_moon_1f_tm09", (737, 762)),
                   ("mt_moon_1f_potion", (1012, 707)), ("mt_moon_1f_rare_candy", (1305, 762)), ("mt_moon_1f_escape_rope", (1342, 506))],
    "mt_moon_b1f": [("mt_moon_b1f_big_mushroom_w", (763, 513)), ("mt_moon_b1f_tinymushroom_m", (1096, 349)), ("mt_moon_b1f_tinymushroom_ne", (1428, 348)),
                    ("mt_moon_b1f_tinymushroom_s", (1311, 879)), ("mt_moon_b1f_big_mushroom_s1", (1062, 895)), ("mt_moon_b1f_big_mushroom_s2", (1079, 877))],
    "mt_moon_b2f": [("mt_moon_b2f_antidote", (685, 522)), ("mt_moon_b2f_star_piece", (1135, 770)), ("mt_moon_b2f_ether", (1283, 520)),
                    ("mt_moon_b2f_tm46", (1217, 421)), ("mt_moon_b2f_revive", (968, 602)), ("mt_moon_b2f_moon_stone", (1035, 440))],
}
# The two fossils (on their stand, where Miguel found them): taking one, he takes the other.
FOSSILS = [("mt_moon_b2f_dome_fossil", (852, 452)), ("mt_moon_b2f_helix_fossil", (869, 452))]
SIGN_1F = (42, 56)


def load(name):
    W, H, L, inv, ids, _b, _r = schem.blocks(schem.load(os.path.join(HERE, "mtmoon", name)))
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
    holes_of, ladders_of, stand_of = {}, {}, {}
    data = {}
    for rid, src, origin, floor, place in ROOMS:
        W, H, L, names = load(src)
        if rid == "mt_moon_1f":
            # (The way out at the south, 14:15: daylight at the back of its doorway, and its light coming in.)
            for x in (40, 41):
                for y in (3, 4, 5):
                    names[(y * L + 82) * W + x] = "minecraft:white_concrete"
                names[(3 * L + 81) * W + x] = "minecraft:light[level=15,waterlogged=false]"
                names[(4 * L + 79) * W + x] = "minecraft:light[level=12,waterlogged=false]"
        full = lambda x, y, z, names=names, W=W, H=H, L=L: names[(y * L + z) * W + x] if 0 <= x < W and 0 <= y < H and 0 <= z < L else "minecraft:air"
        base = lambda x, y, z, full=full: full(x, y, z).replace("minecraft:", "").split("[")[0]

        # Where you stand in each cell (the schematic's y, from the floor up to two above it: B2F's upper level), or None.
        def stand(x, z, base=base, floor=floor, H=H):
            for y in range(floor, min(H - 1, floor + 3)):
                below, here, up = base(x, y - 1, z), base(x, y, z), base(x, y + 1, z)
                if below in FREE or below == "ladder" or below == "black_concrete" or below == "water": continue
                if below in ROCKS: return None
                if here in FREE and up in FREE:
                    if below.endswith("_stairs"): return y - 0.5
                    if below.endswith("_slab"):
                        return None if below.startswith(("granite", "polished_granite")) else y - 0.5
                    return y
            return None
        S = {(x, z): stand(x, z) for z in range(L) for x in range(W)}
        stand_of[rid] = S
        holes = []
        seen = set()
        for z in range(L):
            for x in range(W):
                if base(x, 0, z) == "black_concrete" and (x, z) not in seen and base(x, floor - 1, z) in FREE | {"ladder"}:
                    grp, todo = [], [(x, z)]
                    while todo:
                        c = todo.pop()
                        if c in seen or base(c[0], 0, c[1]) != "black_concrete" or base(c[0], floor - 1, c[1]) not in FREE | {"ladder"}: continue
                        seen.add(c); grp.append(c)
                        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)): todo.append((c[0] + dx, c[1] + dz))
                    if grp: holes.append(grp)
        # Ladders up: ladder blocks from the floor you stand on (or B2F's upper level) upwards.
        lad = {}
        for z in range(L):
            for x in range(W):
                for y in range(floor, H):
                    if base(x, y, z) == "ladder":
                        f = full(x, y, z)
                        facing = f.split("facing=")[1].split(",")[0].rstrip("]") if "facing=" in f else "south"
                        lad.setdefault((x, z), (y, facing))
                        break
        ladders, used = [], set()
        for (x, z), (y0, facing) in sorted(lad.items()):
            if (x, z) in used: continue
            grp = [(x, z)]
            for dx in (1, 2):
                if (x + dx, z) in lad and lad[(x + dx, z)][1] == facing: grp.append((x + dx, z))
            for dz in (1, 2):
                if (x, z + dz) in lad and lad[(x, z + dz)][1] == facing: grp.append((x, z + dz))
            used.update(grp)
            fx, fz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
            xs, zs = [g[0] for g in grp], [g[1] for g in grp]
            foot = ((min(xs) + max(xs) + 1) / 2 + fx * 1.0, (min(zs) + max(zs) + 1) / 2 + fz * 1.0)
            ladders.append({"x0": min(xs), "x1": max(xs) + 1, "z0": min(zs), "z1": max(zs) + 1, "y": y0 - floor, "facing": facing, "foot": foot})
        holes_of[rid], ladders_of[rid] = holes, ladders
        data[rid] = (W, H, L, names, base, floor, origin, place)
        print("%-12s %3d x %2d x %3d  holes %s  ladders %s" % (rid, W, H, L, [(min(c[0] for c in h), min(c[1] for c in h)) for h in holes],
                                                          [(l["x0"], l["z0"], l["facing"], l["y"]) for l in ladders]))

    def hole_at(rid, x, z):
        best = None
        for h in holes_of[rid]:
            cx, cz = sum(c[0] for c in h) / len(h) + 0.5, sum(c[1] for c in h) / len(h) + 0.5
            d = math.hypot(cx - x, cz - z)
            if best is None or d < best[0]: best = (d, h)
        return best[1]

    def ladder_at(rid, x, z):
        return min(ladders_of[rid], key=lambda l: math.hypot((l["x0"] + l["x1"]) / 2 - x, (l["z0"] + l["z1"]) / 2 - z))

    # The way through (see the top): holes and ladders by where they are on John's floors.
    LINKS = [
        (("mt_moon_1f", 15, 17), ("mt_moon_b1f", 11, 10)),
        (("mt_moon_1f", 42, 32), ("mt_moon_b1f", 54, 12)),
        (("mt_moon_1f", 67, 36), ("mt_moon_b1f", 90, 46)),
        (("mt_moon_b1f", 48, 40), ("mt_moon_b2f", 66, 26)),
        (("mt_moon_b1f", 38, 14), ("mt_moon_b2f", 54, 46)),
        (("mt_moon_b1f", 56, 76), ("mt_moon_b2f", 39, 66)),
        (("mt_moon_b1f", 82, 12), ("mt_moon_b2f", 15, 24)),
    ]
    warps = {rid: [] for rid, *_ in ROOMS}
    for (hr, hx, hz), (lr, lx, lz) in LINKS:
        h = hole_at(hr, hx, hz)
        l = ladder_at(lr, lx, lz)
        hx0, hx1 = min(c[0] for c in h), max(c[0] for c in h) + 1
        hz0, hz1 = min(c[1] for c in h), max(c[1] for c in h) + 1
        # Beside the hole, coming up: the open floor next to it, on the side away from its ladder (south, else east).
        up_spot = None
        for cand in ((hx0 + hx1) / 2, hz1 + 0.9), ((hx0 + hx1) / 2, hz0 - 0.9), (hx1 + 0.9, (hz0 + hz1) / 2), (hx0 - 0.9, (hz0 + hz1) / 2):
            if stand_of[hr].get((int(math.floor(cand[0])), int(math.floor(cand[1])))) is not None:
                up_spot = cand; break
        assert up_spot, (hr, hx, hz)
        fx, fz = l["foot"]
        assert stand_of[lr].get((int(math.floor(fx)), int(math.floor(fz)))) is not None, (lr, l)
        warps[hr].append({"kind": "down", "x0": hx0 - 0.2, "x1": hx1 + 0.2, "z0": hz0 - 0.2, "z1": hz1 + 0.2, "to": lr,
                          "spawn": {"x": round(fx, 2), "z": round(fz, 2), "yaw": {"south": 0.0, "north": 180.0, "east": 270.0, "west": 90.0}[l["facing"]]}})
        warps[lr].append({"kind": "up", "x0": l["x0"], "x1": l["x1"], "z0": l["z0"], "z1": l["z1"] - 1, "to": hr,
                          "spawn": {"x": round(up_spot[0], 2), "z": round(up_spot[1], 2), "yaw": 0.0 if up_spot[1] > hz1 else 180.0}})
    # B1F's way out: its north-east ladder up, out of the mouth on Route 4.
    ex = ladder_at("mt_moon_b1f", 94, 12)
    warps["mt_moon_b1f"].append({"kind": "up", "x0": ex["x0"], "x1": ex["x1"], "z0": ex["z0"], "z1": ex["z1"] - 1, "door": "mt_moon_route_4", "to": None})
    exit_foot = {"x": round(ex["foot"][0], 2), "z": round(ex["foot"][1], 2), "yaw": 0.0}
    # 1F's way out: the doorway at the south (its two blocks of sandstone), back out of the mouth by the Center.
    warps["mt_moon_1f"].append({"kind": "mat", "x0": 40.0, "x1": 42.0, "z": 81.2, "door": "mt_moon_route_3", "to": None})
    entry_1f = {"x": 41.0, "z": 78.4, "yaw": 180.0}
    DOORS["mt_moon_route_3"]["in"] = entry_1f
    DOORS["mt_moon_route_4"]["in"] = exit_foot
    DOORS["mt_moon_route_4_west"]["in"] = exit_foot

    for rid, src, origin, floor, place in ROOMS:
        W, H, L, names, base, floor, origin, place = data[rid]
        S = stand_of[rid]
        a, s, b, c, t, d = MAPS[rid]
        to_floor = lambda px, py: ((px - a) / s + b, (py - c) / t + d)

        def nearest(x, z, label, taken=()):
            best = None
            for r in range(0, 8):
                for dz in range(-r, r + 1):
                    for dx in range(-r, r + 1):
                        cx, cz = int(math.floor(x)) + dx, int(math.floor(z)) + dz
                        if S.get((cx, cz)) is None or (cx, cz) in taken: continue
                        # Not on a hole's ladder or right by one.
                        dd = (cx + 0.5 - x) ** 2 + (cz + 0.5 - z) ** 2
                        if best is None or dd < best[0]: best = (dd, cx, cz)
                if best: break
            assert best, (rid, label)
            return best[1] + 0.5, best[2] + 0.5

        def line(x, z, facing, n=8):
            dx, dz = DIRS[facing]
            k = 0
            for i in range(1, n + 1):
                if S.get((int(math.floor(x + dx * i)), int(math.floor(z + dz * i)))) is None: break
                k = i
            return k
        taken = set()
        people = []
        for key, title, skin, (px, py), facing in TRAINERS[rid]:
            x, z = nearest(*to_floor(px, py), key, taken)
            taken.add((int(x), int(z)))
            # (Facing an open way: the games' way if it's open for two blocks or more, else the most open way.)
            if line(x, z, facing) < 2:
                facing = max(DIRS, key=lambda f: line(x, z, f))
            people.append({"key": key, "name": title, "skin": skin, "x": round(x, 3), "z": round(z, 3), "yaw": YAW[facing], "lines": [], "role": "trainer",
                           "y": S[(int(x), int(z))] - floor, "facing": facing})
        for key, name, skin, (px, py), facing, lines in PEOPLE[rid]:
            x, z = nearest(*to_floor(px, py), key, taken)
            taken.add((int(x), int(z)))
            people.append({"key": key, "name": name, "skin": skin, "x": round(x, 3), "z": round(z, 3), "yaw": YAW[facing], "lines": lines, "y": S[(int(x), int(z))] - floor})
        items = []
        for key, (px, py) in ITEMS[rid] + (FOSSILS if rid == "mt_moon_b2f" else []):
            x, z = nearest(*to_floor(px, py), key, taken)
            taken.add((int(x), int(z)))
            items.append({"key": key, "x": round(x, 3), "z": round(z, 3), "y": S[(int(x), int(z))] - floor})
        acts = []
        if rid == "mt_moon_1f":
            sx, sz = SIGN_1F
            acts.append({"rect": [sx - 0.6, sz - 0.6, sx + 2.6, sz + 1.8], "label": "Read", "lines": ["Beware! ZUBAT is a blood sucker!"]})
        # Where its wild POKéMON come up: all its open floor (each row's runs, joined down where they line up).
        rows = {}
        for z in range(L):
            x = 0
            while x < W:
                if S.get((x, z)) is not None:
                    x0 = x
                    while x < W and S.get((x, z)) is not None: x += 1
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
        # Lamps you can't see over the open floor every 5 blocks (dim: FireRed's Mt. Moon isn't dark, but it's a cave).
        lights = []
        for z in range(2, L, 5):
            for x in range(2, W, 5):
                for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1), (2, 0), (0, 2)):
                    st = S.get((x + dx, z + dz))
                    if st is not None:
                        lights.append([x + dx, z + dz, LIGHT, math.ceil(st) - floor]); break
        # (15:21) The rocks as copper grates (the mod draws the boulders on them), so the floor and walls round them show.
        BIG, SMALL = "minecraft:waxed_copper_grate[waterlogged=false]", "minecraft:waxed_exposed_copper_grate[waterlogged=false]"
        out = []
        for n in names:
            k = n.replace("minecraft:", "").split("[")[0]
            out.append(BIG if k in ("granite", "polished_granite") else SMALL if k in ("granite_slab", "polished_granite_slab") else n)
        if rid == "mt_moon_b1f":
            # (15:21) B1F's walls low (four blocks over the floor, not six and twelve), so you can see yourself behind them.
            for y in range(floor + 4, H):
                for i in range(y * L * W, (y + 1) * L * W):
                    if out[i] != "minecraft:air": out[i] = "minecraft:air"
        # (0.52) Cut away on the camera's side, as the rooms are (tools/cavecut.py).
        out, _n = cavecut.cut(out, W, H, L)
        out = cavecut.fill(out, W, H, L, rid)
        size = write_bin(os.path.join(RES, rid + ".bin"), W, H, L, out)
        spawn = entry_1f if rid == "mt_moon_1f" else exit_foot if rid == "mt_moon_b1f" else warps[rid][0]["spawn"] if warps[rid] else {"x": W / 2, "z": L / 2, "yaw": 0.0}
        if rid == "mt_moon_b2f":
            l = ladder_at("mt_moon_b2f", 15, 24)
            spawn = {"x": round(l["foot"][0], 2), "z": round(l["foot"][1], 2), "yaw": 0.0}
        rooms[rid] = {
            "origin": list(origin), "W": W, "D": L, "H": H - floor, "name": "Mt. Moon", "floor": {"mt_moon_1f": "1F", "mt_moon_b1f": "B1F", "mt_moon_b2f": "B2F"}[rid],
            "place": place, "schem": rid + ".bin", "base": -floor, "dark": True, "haze": HAZE, "tile": TILE,
            "spawn": spawn, "entry": spawn, "cam": {"x": W / 2, "z": L / 2, "dist": 30}, "cells": [],
            "warps": warps[rid], "people": people, "acts": acts, "grass": rects, "items": items, "lights": lights, "pools": [], "panes": [], "clocks": [],
        }
        print("%-12s bin %5d bytes  floor boxes %3d  lamps %3d  people %d  items %d  warps %d" % (rid, size, len(rects), len(lights), len(people), len(items), len(warps[rid])))
        for p in people: print("     %-18s %-20s (%6.1f, %6.1f) y %s %s" % (p["key"], p["name"], p["x"], p["z"], p["y"], p.get("facing", "")))
        for i in items: print("     %-30s (%6.1f, %6.1f) y %s" % (i["key"], i["x"], i["z"], i["y"]))
        # The viewer's copy: each cell two characters (what's there, and how high: open floor its height above the
        # floor in half blocks; rock its height in blocks).
        KIND = {"andesite": "f", "sand": "s", "smooth_sandstone": "S", "gravel": "v", "tuff": "t", "water": "q", "spruce_stairs": "^", "spruce_planks": "f"}
        cells = []
        for z in range(L):
            row = []
            for x in range(W):
                st = S.get((x, z))
                if base(x, 0, z) == "black_concrete" and st is None:
                    row.append("L0" if any(base(x, y, z) == "ladder" for y in range(0, floor)) else "K0"); continue
                if st is not None:
                    below = base(x, int(math.ceil(st)) - 1, z)
                    k = "^" if st != int(st) else KIND.get(below, "f")
                    if any(base(x, y, z) == "ladder" for y in range(floor, min(H, floor + 8))): k = "L"
                    row.append(k + chr(48 + int(round((st - floor) * 2)))); continue
                k = base(x, floor, z)
                if k in ("granite", "polished_granite", "granite_slab", "polished_granite_slab"):
                    row.append(("g" if k.endswith("slab") else "G") + chr(48 + int(round((math.ceil(S.get((x, z)) or floor) - floor) * 2)))); continue
                if k == "tuff" or base(x, floor - 1, z) == "tuff":
                    row.append("t0"); continue
                if base(x, floor - 1, z) == "water" or k == "water":
                    row.append("q0"); continue
                top = 0
                for y in range(H - 1, floor - 1, -1):
                    if base(x, y, z) not in FREE: top = y - floor + 1; break
                row.append("w" + chr(48 + min(top if rid != "mt_moon_b1f" else min(top, 4), 40)))
            cells.append("".join(row))
        viewer[rid] = {"W": W, "D": L, "rows": cells, "people": people, "items": items, "spawn": spawn, "warps": warps[rid], "acts": acts,
                       "name": "Mt. Moon", "floor": rooms[rid]["floor"]}
    json.dump({"rooms": rooms, "doors": DOORS}, open(os.path.join(RES, "mt_moon.json"), "w"), indent=1)
    js = ("// Mt. Moon as the mod has it (tools/mtmoon.py, from John's schematics): for each floor a row of cells for each z,\n"
          "// two characters a cell (what's there, how high), its people, items and ways in and out.\n"
          "const MTMOON = " + json.dumps(viewer, separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")


main()
