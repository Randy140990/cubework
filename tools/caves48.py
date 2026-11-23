#!/usr/bin/env python3
"""
Seafoam Islands and Rock Tunnel as John built them (17:42; tools/seafoam/seafoam_islands.schem: its five floors side by
side, 1F to B4F; tools/rocktunnel/*.schem: 1F and B1F), made into rooms of their own in the interiors, as Mt. Moon is
(tools/mtmoon.py), and stitched together as FireRed has them (its maps' warps, pret/pokefirered).

John's floors are FireRed's two blocks a step: Seafoam's x = 2 * step + 4, z = 2 * step + 8 (on each floor); Rock
Tunnel's x = 2 * step + 8, z = 2 * step + 8. Ladders down sit in pits with black at their foot; ladders up stand on the
floor; Seafoam's open pits (black at the bottom, no ladder) are FireRed's holes you fall through.

  Seafoam Islands  in from Route 20 by either mouth (FireRed's two, west and east) onto 1F; ladders and holes down to
                   B4F and ARTICUNO. Its rocks are ice (John's packed ice): the mod draws them as ice, its boulders
                   (cobblestone, the ones you'd push with STRENGTH) as clusters of ice shards. B3F and B4F are deep
                   water: you can swim it (SURF comes later).
  Rock Tunnel      in from the north mouth (by Route 10's POKéMON CENTER) at 1F's ladder up, out at 1F's south doorway
                   (on to Lavender); four ladders between 1F and B1F; its rocks craggy boulders; dark, as FireRed's.

Writes src/main/resources/retromon/interiors/seafoam_islands.json and rock_tunnel.json (rooms and doors), a .bin of
blocks for each floor, and tools/interior/caves48_data.js for the Interior Viewer.

    python3 -I tools/caves48.py
"""
import gzip, io, json, math, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import schem  # noqa: E402
import cavecut  # noqa: E402

RES = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")
VIEWER = os.path.join(HERE, "interior", "caves48_data.js")
FREE = {"air", "light", "short_grass", "pale_oak_shelf", "cave_air"}
YAW = {"down": 0.0, "left": 90.0, "up": 180.0, "right": 270.0}
FACING_YAW = {"south": 0.0, "north": 180.0, "east": 270.0, "west": 90.0}
DIRS = {"down": (0, 1), "up": (0, -1), "left": (-1, 0), "right": (1, 0)}
BIG, SMALL, SMASH = ("minecraft:waxed_copper_grate[waterlogged=false]", "minecraft:waxed_exposed_copper_grate[waterlogged=false]",
                     "minecraft:waxed_weathered_copper_grate[waterlogged=false]")

CAVES = {
    "seafoam": {
        "json": "seafoam_islands.json", "name": "Seafoam Islands", "src": [("seafoam", "seafoam_islands.schem")],
        "floors": [("seafoam_islands_1f", "1F", 0), ("seafoam_islands_b1f", "B1F", 104), ("seafoam_islands_b2f", "B2F", 208),
                   ("seafoam_islands_b3f", "B3F", 312), ("seafoam_islands_b4f", "B4F", 416)],
        "W": 84, "floor": 6, "up": 4, "origin0": 6656, "step": 128, "off": (4, 8),
        "rocks": {"packed_ice", "snow_block", "cobblestone", "dripstone_block", "blue_ice", "ice"},
        "boulders": {"cobblestone": BIG}, "haze": [150, 190, 230, 0.10], "light": 11,
        # (hole floor, step x, step z) -> (ladder floor, step x, step z): FireRed's ladders.
        "links": [((0, 10, 6), (1, 10, 6)), ((0, 31, 4), (1, 31, 4)), ((0, 28, 19), (1, 28, 19)),
                  ((1, 7, 3), (2, 7, 4)), ((1, 17, 9), (2, 17, 9)), ((1, 25, 19), (2, 25, 19)), ((1, 32, 14), (2, 32, 14)),
                  ((2, 7, 17), (3, 8, 14)), ((2, 32, 4), (3, 31, 4)), ((2, 31, 17), (3, 31, 16)),
                  ((3, 12, 9), (4, 15, 9)), ((3, 29, 5), (4, 32, 5))],
        # FireRed's holes you fall through: (floor, step) -> (floor below, where you land).
        "falls": [((0, 21, 8), (1, 21, 8)), ((0, 30, 8), (1, 29, 8)), ((1, 23, 8), (2, 22, 7)), ((1, 28, 8), (2, 29, 8)),
                  ((2, 24, 8), (3, 23, 9)), ((2, 27, 8), (3, 24, 9)), ((3, 6, 18), (4, 8, 17)), ((3, 9, 18), (4, 9, 17))],
        # The mouths in John's world (the black at the back of each doorway; you walk north into it) and 1F's doorways.
        "doors": {
            "seafoam_west": {"door": [[-643, 18, 919], [-642, 18, 919]], "out": [-642.0, 18.0, 922.4], "mat": (14, 20, 52.6), "in": (17.0, 50.5)},
            "seafoam_east": {"door": [[-618, 18, 929], [-617, 18, 929]], "out": [-617.0, 18.0, 932.4], "mat": (66, 72, 52.6), "in": (69.0, 50.5)},
        },
        "trainers": {},
        "items": {0: [("seafoam_islands_1f_ice_heal", "ice_heal", 11, 8)],
                  1: [("seafoam_islands_b1f_water_stone", "water_stone", 19, 18), ("seafoam_islands_b1f_revive", "revive", 24, 14)],
                  2: [("seafoam_islands_b2f_big_pearl", "big_pearl", 18, 15)],
                  3: [("seafoam_islands_b3f_nugget", "nugget", 5, 12)],
                  4: [("seafoam_islands_b4f_ultra_ball", "ultra_ball", 22, 19), ("seafoam_islands_b4f_water_stone", "water_stone", 13, 8)]},
        "mons": {4: [("articuno", "articuno", 9, 2, "down")]},
        "signs": {4: [(30, 2, ["DANGER", "Fast current!"]), (14, 18, ["BOULDERS might change the flow of the water!"])]},
    },
    "rock_tunnel": {
        "json": "rock_tunnel.json", "name": "Rock Tunnel",
        "src": [("rocktunnel", "rock_tunnel_1f.schem"), ("rocktunnel", "rock_tunnel_b1f.schem")],
        "floors": [("rock_tunnel_1f", "1F", 0), ("rock_tunnel_b1f", "B1F", 0)],
        "W": None, "floor": None, "up": 2, "origin0": 7296, "step": 128, "off": (8, 8),
        "rocks": {"andesite", "diorite", "cobblestone", "cobblestone_slab", "cobbled_deepslate", "andesite_slab"},
        "boulders": {"cobblestone": BIG, "cobblestone_slab": SMALL, "cobbled_deepslate": SMASH},
        "haze": [70, 60, 52, 0.16], "light": 6,
        "links": [((0, 45, 2), (1, 38, 28)), ((0, 4, 2), (1, 33, 3)), ((0, 20, 13), (1, 27, 12)), ((0, 45, 21), (1, 2, 3))],
        "falls": [],
        "doors": {
            "rock_tunnel_north": {"door": [[-138, 34, 283], [-137, 34, 283]], "out": [-137.0, 34.0, 286.4], "ladder": (0, 17, 2)},
            "rock_tunnel_south": {"door": [[-138, 35, 358], [-137, 35, 358]], "out": [-137.0, 35.0, 361.4], "mat": (44, 46, 84.4), "in": (45.0, 82.5)},
        },
        # FireRed's trainers (data/trainers.tsv's rows, Red and Blue's parties, FireRed's names): key, title, skin, step, facing.
        "trainers": {
            0: [("rock_tunnel_1f_4", "PICNICKER DANA", "picnicker", 26, 28, "down"), ("rock_tunnel_1f_5", "PICNICKER ARIANA", "picnicker", 39, 27, "left"),
                ("rock_tunnel_1f_6", "PICNICKER LEAH", "picnicker", 45, 24, "down"), ("rock_tunnel_1f_0", "HIKER LUCAS", "hiker", 20, 17, "down"),
                ("rock_tunnel_1f_1", "HIKER OLIVER", "hiker", 5, 20, "right"), ("rock_tunnel_1f_2", "HIKER LENNY", "hiker", 7, 5, "down"),
                ("rock_tunnel_1f_3", "POKéMANIAC ASHTON", "super_nerd", 27, 10, "down")],
            1: [("rock_tunnel_b1f_2", "POKéMANIAC COOPER", "super_nerd", 2, 6, "down"), ("rock_tunnel_b1f_1", "HIKER DUDLEY", "hiker", 5, 11, "down"),
                ("rock_tunnel_b1f_0", "PICNICKER SOFIA", "picnicker", 11, 15, "right"), ("rock_tunnel_b1f_4", "HIKER ALLEN", "hiker", 36, 11, "left"),
                ("rock_tunnel_b1f_6", "HIKER ERIC", "hiker", 41, 5, "down"), ("rock_tunnel_b1f_3", "POKéMANIAC STEVE", "super_nerd", 23, 23, "right"),
                ("rock_tunnel_b1f_5", "PICNICKER MARTHA", "picnicker", 15, 33, "up"), ("rock_tunnel_b1f_7", "POKéMANIAC WINSTON", "super_nerd", 29, 35, "left")],
        },
        "items": {0: [("rock_tunnel_1f_repel", "repel", 37, 5), ("rock_tunnel_1f_pearl", "pearl", 33, 22), ("rock_tunnel_1f_escape_rope", "escape_rope", 22, 22)],
                  1: [("rock_tunnel_b1f_revive", "revive", 2, 37), ("rock_tunnel_b1f_max_ether", "max_ether", 9, 2)]},
        "mons": {},
        "signs": {0: [(14, 33, ["ROCK TUNNEL", "CERULEAN CITY - LAVENDER TOWN"])]},
    },
}


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


def floors_of(cfg):
    """Each floor's blocks: (W, H, L, names)."""
    out = []
    if len(cfg["src"]) == 1:
        d, f = cfg["src"][0]
        W, H, L, inv, ids, _b, _r = schem.blocks(schem.load(os.path.join(HERE, d, f)))
        names = [inv[i] for i in ids]
        for rid, fl, x0 in cfg["floors"]:
            w = cfg["W"]
            sub = [names[(y * L + z) * W + x0 + x] for y in range(H) for z in range(L) for x in range(w)]
            out.append((w, H, L, sub))
    else:
        for d, f in cfg["src"]:
            W, H, L, inv, ids, _b, _r = schem.blocks(schem.load(os.path.join(HERE, d, f)))
            out.append((W, H, L, [inv[i] for i in ids]))
    return out


def build(key):
    cfg = CAVES[key]
    ox, oz = cfg["off"]
    blocks = floors_of(cfg)
    info = []
    for k, ((rid, fl, _x0), (W, H, L, names)) in enumerate(zip(cfg["floors"], blocks)):
        full = lambda x, y, z, names=names, W=W, H=H, L=L: names[(y * L + z) * W + x] if 0 <= x < W and 0 <= y < H and 0 <= z < L else "minecraft:air"
        base = lambda x, y, z, full=full: full(x, y, z).replace("minecraft:", "").split("[")[0]
        # Where you stand on this floor: the coarse dirt's (Rock Tunnel), the calcite's (Seafoam, two levels).
        floor = cfg["floor"]
        if floor is None:
            from collections import Counter
            c = Counter()
            for z in range(L):
                for x in range(W):
                    for y in range(H - 1):
                        if base(x, y, z) == "coarse_dirt" and base(x, y + 1, z) == "air":
                            c[y + 1] += 1; break
            floor = c.most_common(1)[0][0]
        rocks = cfg["rocks"]

        def stand(x, z, base=base, floor=floor, H=H):
            for y in range(min(H - 2, floor + cfg["up"]), floor - 1, -1):
                below, here, up = base(x, y - 1, z), base(x, y, z), base(x, y + 1, z)
                if below in FREE or below in ("ladder", "black_concrete", "water"): continue
                if here in FREE and up in FREE:
                    if below in rocks: return None
                    if below.endswith("_stairs") or below.endswith("_slab"): return y - 0.5
                    return y
                return None
            return None
        S = {(x, z): stand(x, z) for z in range(L) for x in range(W)}
        # Pits: black with a ladder in it (a ladder down) or none (a hole you fall through).
        blacks = {}
        for z in range(L):
            for x in range(W):
                for y in range(0, floor):
                    if base(x, y, z) == "black_concrete":
                        blacks[(x, z)] = (y, base(x, y + 1, z) == "ladder"); break
        pits, seen = [], set()
        for c0 in sorted(blacks):
            if c0 in seen: continue
            grp, todo = [], [c0]
            while todo:
                c = todo.pop()
                if c in seen or c not in blacks or blacks[c][0] != blacks[c0][0]: continue
                seen.add(c); grp.append(c)
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)): todo.append((c[0] + dx, c[1] + dz))
            pits.append({"cells": grp, "ladder": any(blacks[c][1] for c in grp), "y": blacks[c0][0]})
        # Ladders up: ladder blocks standing on the floor (or the upper level), not in a pit.
        lad = {}
        for z in range(L):
            for x in range(W):
                if (x, z) in blacks: continue
                for y in range(1, H):
                    if base(x, y, z) == "ladder":
                        f = full(x, y, z)
                        facing = f.split("facing=")[1].split(",")[0].rstrip("]") if "facing=" in f else "south"
                        lad[(x, z)] = (y, facing); break
        ladders, used = [], set()
        for (x, z), (y0, facing) in sorted(lad.items()):
            if (x, z) in used: continue
            grp = [(x, z)] + [(x + d, z) for d in (1, 2) if lad.get((x + d, z), (0, ""))[1] == facing] + [(x, z + d) for d in (1, 2) if lad.get((x, z + d), (0, ""))[1] == facing]
            used.update(grp)
            fx, fz = {"south": (0, 1), "north": (0, -1), "east": (1, 0), "west": (-1, 0)}[facing]
            xs, zs = [g[0] for g in grp], [g[1] for g in grp]
            foot = ((min(xs) + max(xs) + 1) / 2 + fx * 1.0, (min(zs) + max(zs) + 1) / 2 + fz * 1.0)
            ladders.append({"x0": min(xs), "x1": max(xs) + 1, "z0": min(zs), "z1": max(zs) + 1, "y": y0 - floor, "facing": facing, "foot": foot})
        info.append({"rid": rid, "fl": fl, "W": W, "H": H, "L": L, "names": names, "base": base, "floor": floor, "S": S, "pits": pits, "ladders": ladders,
                     "origin": (cfg["origin0"] + k * cfg["step"], 64, 0)})
        print("%-22s %3d x %2d x %3d floor %d  pits %s  ladders %s" % (rid, W, H, L, floor, [(min(c[0] for c in p["cells"]), min(c[1] for c in p["cells"]), "L" if p["ladder"] else "o") for p in pits],
                                                                  [(l["x0"], l["z0"], l["facing"], l["y"]) for l in ladders]))

    def at(t):
        return 2 * t[0] + ox + 1, 2 * t[1] + oz + 1

    def pit_at(k, tx, tz, ladder):
        x, z = at((tx, tz))
        best = None
        for p in info[k]["pits"]:
            if p["ladder"] != ladder: continue
            cx = sum(c[0] for c in p["cells"]) / len(p["cells"]) + 0.5
            cz = sum(c[1] for c in p["cells"]) / len(p["cells"]) + 0.5
            d = math.hypot(cx - x, cz - z)
            if best is None or d < best[0]: best = (d, p)
        assert best and best[0] < 4, (info[k]["rid"], tx, tz, best and best[0])
        return best[1]

    def ladder_at(k, tx, tz):
        x, z = at((tx, tz))
        l = min(info[k]["ladders"], key=lambda l: math.hypot((l["x0"] + l["x1"]) / 2 - x, (l["z0"] + l["z1"]) / 2 - z))
        assert math.hypot((l["x0"] + l["x1"]) / 2 - x, (l["z0"] + l["z1"]) / 2 - z) < 4, (info[k]["rid"], tx, tz)
        return l

    def standy(k, x, z):
        s = info[k]["S"].get((int(math.floor(x)), int(math.floor(z))))
        return None if s is None else round(s - info[k]["floor"], 2)

    def near_open(k, x, z, water=False, taken=()):
        S, I = info[k]["S"], info[k]
        best = None
        for r in range(0, 9):
            for dz in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    cx, cz = int(math.floor(x)) + dx, int(math.floor(z)) + dz
                    ok = S.get((cx, cz)) is not None or (water and I["base"](cx, I["floor"] - 1, cz) == "water")
                    if not ok or (cx, cz) in taken: continue
                    dd = (cx + 0.5 - x) ** 2 + (cz + 0.5 - z) ** 2
                    if best is None or dd < best[0]: best = (dd, cx, cz)
            if best: break
        assert best, (I["rid"], x, z)
        return best[1] + 0.5, best[2] + 0.5

    warps = {I["rid"]: [] for I in info}
    for (hk, htx, htz), (lk, ltx, ltz) in cfg["links"]:
        p = pit_at(hk, htx, htz, True)
        l = ladder_at(lk, ltx, ltz)
        hx0, hx1 = min(c[0] for c in p["cells"]), max(c[0] for c in p["cells"]) + 1
        hz0, hz1 = min(c[1] for c in p["cells"]), max(c[1] for c in p["cells"]) + 1
        up_spot = None
        for cand in (((hx0 + hx1) / 2, hz1 + 0.9), ((hx0 + hx1) / 2, hz0 - 0.9), (hx1 + 0.9, (hz0 + hz1) / 2), (hx0 - 0.9, (hz0 + hz1) / 2)):
            if standy(hk, *cand) is not None:
                up_spot = cand; break
        assert up_spot, (info[hk]["rid"], htx, htz)
        fx, fz = l["foot"]
        if standy(lk, fx, fz) is None: fx, fz = near_open(lk, fx, fz)
        warps[info[hk]["rid"]].append({"kind": "down", "x0": hx0 - 0.2, "x1": hx1 + 0.2, "z0": hz0 - 0.2, "z1": hz1 + 0.2, "to": info[lk]["rid"],
                                       "spawn": {"x": round(fx, 2), "z": round(fz, 2), "y": standy(lk, fx, fz), "yaw": FACING_YAW[l["facing"]]}})
        warps[info[lk]["rid"]].append({"kind": "up", "x0": l["x0"], "x1": l["x1"], "z0": l["z0"], "z1": l["z1"] - 1, "to": info[hk]["rid"],
                                       "spawn": {"x": round(up_spot[0], 2), "z": round(up_spot[1], 2), "y": standy(hk, *up_spot), "yaw": 0.0 if up_spot[1] > hz1 else 180.0}})
    for (hk, htx, htz), (lk, ltx, ltz) in cfg["falls"]:
        p = pit_at(hk, htx, htz, False)
        hx0, hx1 = min(c[0] for c in p["cells"]), max(c[0] for c in p["cells"]) + 1
        hz0, hz1 = min(c[1] for c in p["cells"]), max(c[1] for c in p["cells"]) + 1
        x, z = at((ltx, ltz))
        x, z = near_open(lk, x, z, water=True)
        y = standy(lk, x, z)
        warps[info[hk]["rid"]].append({"kind": "down", "x0": hx0 - 0.1, "x1": hx1 + 0.1, "z0": hz0 - 0.1, "z1": hz1 + 0.1, "to": info[lk]["rid"],
                                       "spawn": {"x": round(x, 2), "z": round(z, 2), "y": 0.0 if y is None else y, "yaw": 0.0}})
    doors = {}
    entry = {}
    for dk, d in cfg["doors"].items():
        door = {"room": info[0]["rid"], "door": d["door"], "out": d["out"]}
        if "mat" in d:
            x0, x1, mz = d["mat"]
            warps[info[0]["rid"]].append({"kind": "mat", "x0": float(x0), "x1": float(x1), "z": mz, "door": dk, "to": None})
            door["in"] = {"x": d["in"][0], "z": d["in"][1], "yaw": 180.0}
        if "ladder" in d:
            l = ladder_at(*d["ladder"])
            warps[info[0]["rid"]].append({"kind": "up", "x0": l["x0"], "x1": l["x1"], "z0": l["z0"], "z1": l["z1"] - 1, "door": dk, "to": None})
            fx, fz = l["foot"]
            door["in"] = {"x": round(fx, 2), "z": round(fz, 2), "yaw": 0.0}
        if len(cfg["doors"]) > 1: door["group"] = key + "_mouths"
        doors[dk] = door
        entry.setdefault(info[0]["rid"], door["in"])

    rooms, viewer = {}, {}
    for k, I in enumerate(info):
        rid, W, H, L, names, base, floor, S = I["rid"], I["W"], I["H"], I["L"], I["names"], I["base"], I["floor"], I["S"]
        taken = set()

        def line(x, z, facing, n=8):
            dx, dz = DIRS[facing]
            c = 0
            for i in range(1, n + 1):
                if S.get((int(math.floor(x + dx * i)), int(math.floor(z + dz * i)))) is None: break
                c = i
            return c
        people = []
        for key2, title, skin, tx, tz, facing in cfg["trainers"].get(k, []):
            x, z = near_open(k, *at((tx, tz)), taken=taken)
            taken.add((int(x), int(z)))
            if line(x, z, facing) < 2: facing = max(DIRS, key=lambda f: line(x, z, f))
            people.append({"key": key2, "name": title, "skin": skin, "x": x, "z": z, "yaw": YAW[facing], "lines": [], "role": "trainer", "y": standy(k, x, z), "facing": facing})
        items = []
        for key2, item, tx, tz in cfg["items"].get(k, []):
            x, z = near_open(k, *at((tx, tz)), taken=taken)
            taken.add((int(x), int(z)))
            items.append({"key": key2, "x": x, "z": z, "y": standy(k, x, z), "item": item})
        mons = []
        for key2, species, tx, tz, facing in cfg["mons"].get(k, []):
            x, z = near_open(k, *at((tx, tz)), taken=taken)
            taken.add((int(x), int(z)))
            mons.append({"key": key2, "species": species, "x": x, "z": z, "y": standy(k, x, z), "yaw": YAW[facing]})
        acts = []
        for tx, tz, lines in cfg["signs"].get(k, []):
            x, z = at((tx, tz))
            acts.append({"rect": [x - 2.0, z - 1.6, x + 1.0, z + 2.4], "label": "Read", "lines": lines})
        # Wild POKéMON anywhere on its open floor.
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
        lights = []
        for z in range(2, L, 5):
            for x in range(2, W, 5):
                for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1), (-1, -1), (2, 0), (0, 2)):
                    st = S.get((x + dx, z + dz))
                    if st is not None:
                        lights.append([x + dx, z + dz, cfg["light"], math.ceil(st) - floor]); break
        out = []
        for n in names:
            kk = n.replace("minecraft:", "").split("[")[0]
            out.append(cfg["boulders"].get(kk, n))
        # The rocks drawn by the mod over the grates: a rock under a rock is the same boulder (only the lowest is drawn).
        # (0.52) Cut away on the camera's side, as the rooms are (tools/cavecut.py).
        out, _n = cavecut.cut(out, W, H, L)
        size = write_bin(os.path.join(RES, rid + ".bin"), W, H, L, out)
        # (Where you are on a floor you come down to: where the first way down to it lands you.)
        arrive = [w["spawn"] for ws in warps.values() for w in ws if w.get("to") == rid and w["kind"] == "down"]
        spawn = entry.get(rid) or (arrive[0] if arrive else {"x": W / 2, "z": L / 2, "yaw": 0.0})
        rooms[rid] = {
            "origin": list(I["origin"]), "W": W, "D": L, "H": H - floor, "name": cfg["name"], "floor": I["fl"], "place": rid, "schem": rid + ".bin",
            "base": -floor, "dark": True, "haze": cfg["haze"], "tile": [2.0, 2.0], "spawn": spawn, "entry": spawn, "cam": {"x": W / 2, "z": L / 2, "dist": 30},
            "cells": [], "warps": warps[rid], "people": people, "acts": acts, "grass": rects, "items": [{k2: v for k2, v in i.items() if k2 != "item"} for i in items],
            "mons": mons, "lights": lights, "pools": [], "panes": [], "clocks": [],
        }
        print("%-22s bin %6d  people %d items %d mons %d warps %d lamps %d" % (rid, size, len(people), len(items), len(mons), len(warps[rid]), len(lights)))
        for w in warps[rid]: print("     ", w["kind"], w.get("to"), w.get("door"), {kk: w[kk] for kk in ("x0", "x1", "z0", "z1", "z") if kk in w}, w.get("spawn"))
        for p in people: print("      %-18s %-22s (%5.1f, %5.1f) y %s %s" % (p["key"], p["name"], p["x"], p["z"], p["y"], p["facing"]))
        for i in items + mons: print("      %-34s (%5.1f, %5.1f) y %s" % (i["key"], i["x"], i["z"], i["y"]))
        # The viewer's copy: two characters a cell (what's there, how high in half blocks over the floor).
        cells = []
        for z in range(L):
            row = []
            for x in range(W):
                st = S.get((x, z))
                if (x, z) in {c for p in I["pits"] for c in p["cells"]}:
                    pit = next(p for p in I["pits"] if (x, z) in p["cells"])
                    # (its height: where the pit's floor is, black at its foot, from the floor you walk on)
                    row.append(("L" if pit["ladder"] else "K") + chr(48 + 8 + max(-8, (pit["y"] + 1 - floor) * 2))); continue
                if st is not None:
                    below = base(x, int(math.ceil(st)) - 1, z)
                    kch = "^" if st != int(st) else {"end_stone": "s", "smooth_sandstone": "S", "rooted_dirt": "r", "dirt": "r"}.get(below, "f")
                    if any(base(x, y, z) == "ladder" for y in range(int(st), min(H, int(st) + 4))): kch = "L"
                    row.append(kch + chr(48 + int(round((st - floor) * 2)) + 8)); continue
                kb = None
                for y in range(floor - 1, min(H, floor + 3)):
                    b = base(x, y, z)
                    if b in cfg["boulders"]:
                        kb = ("g" if cfg["boulders"][b] == SMALL else "G") + chr(48 + (y - floor) * 2 + 8); break
                if kb:
                    row.append(kb); continue
                if base(x, floor - 1, z) == "water":
                    row.append("q8"); continue
                top, mat = 0, "w"
                for y in range(H - 1, -1, -1):
                    b = base(x, y, z)
                    if b not in FREE:
                        top = y - floor + 1
                        mat = "i" if b in ("packed_ice", "calcite", "end_stone") else "w"
                        break
                row.append(mat + chr(48 + 8 + max(0, min(top, 12)) * 2))
            cells.append("".join(row))
        viewer[rid] = {"W": W, "D": L, "rows": cells, "people": people, "items": items, "mons": mons, "spawn": spawn, "warps": warps[rid], "acts": acts,
                       "name": cfg["name"], "floor": I["fl"], "cave": key}
    json.dump({"rooms": rooms, "doors": doors}, open(os.path.join(RES, cfg["json"]), "w"), indent=1)
    return viewer


def main():
    viewer = {}
    for key in CAVES:
        viewer.update(build(key))
    js = ("// Seafoam Islands and Rock Tunnel as the mod has them (tools/caves48.py, from John's schematics): for each floor a row\n"
          "// of cells for each z, two characters a cell (what's there; its height over the floor in half blocks, plus 8),\n"
          "// its people, items, POKéMON and ways in and out.\n"
          "const CAVES48 = " + json.dumps(viewer, separators=(",", ":")) + ";\n")
    open(VIEWER, "w").write(js)
    print("viewer data", len(js), "bytes")


main()
