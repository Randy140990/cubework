#!/usr/bin/env python3
"""
The Kanto Placement Map page's data (tools/placemap/kanto-placement-map.html reads placement.json beside it), from
tools/kanto_outdoor_map.py's data.json: each map's name, where it lies on John's map and how sure the fit is, and
everyone and everything on it.

    python3 tools/placemap/build_page.py <kanto_outdoor_map.py's out folder> <page folder>
"""
import json
import os
import shutil
import sys

SRC, OUT = sys.argv[1], sys.argv[2]
HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "..", "src", "main", "resources", "retromon", "data")
d = json.load(open(os.path.join(SRC, "data.json")))
names = {}
for l in open(os.path.join(DATA, "places.tsv"), encoding="utf-8"):
    a = l.rstrip("\n").split("\t")
    if len(a) > 2 and not l.startswith("#"): names[a[0]] = a[2]
ORDER = ["pallet_town", "route_1", "viridian_city", "route_22", "route_2", "pewter_city", "route_3", "route_4", "cerulean_city", "route_24", "route_25",
         "route_5", "route_6", "vermilion_city", "route_11", "route_9", "route_10", "lavender_town", "route_8", "route_7", "celadon_city", "saffron_city",
         "route_16", "route_17", "route_18", "fuchsia_city", "route_15", "route_14", "route_13", "route_12", "route_19", "route_20", "route_21",
         "cinnabar_island", "route_23", "indigo_plateau"]
maps = []
for mid, m in d["maps"].items():
    pl = m["place"]
    key = "route_21" if pl.startswith("route_21") else pl
    nm = names.get(pl) or names.get(key) or m["map"]
    if m["map"] == "Route21_North": nm = (names.get("route_21") or "Route 21") + " (north)"
    if m["map"] == "Route21_South": nm = (names.get("route_21") or "Route 21") + " (south)"
    if "Indigo" in m["map"]: key, nm = "indigo_plateau", "INDIGO PLATEAU"
    sure = m["sure"] or m["map"] == "PalletTown"  # (Pallet's doors agree with it to half a block.)
    maps.append({"id": m["map"], "place": pl, "name": nm, "order": ORDER.index(key) + (0.5 if m["map"] == "Route21_South" else 0) if key in ORDER else 99,
                 "x0": m["ox"], "z0": m["oz"], "x1": m["ox"] + 2 * m["w"], "z1": m["oz"] + 2 * m["h"], "sure": sure, "score": m["score"], "next": m["next"]})
maps.sort(key=lambda m: (m["order"], m["name"]))
things = []
for t in d["things"]:
    o = {"m": t["map"], "k": t["kind"], "l": t["label"], "gx": t["gx"], "gy": t["gy"], "x": t["x"], "y": t["y"], "z": t["z"], "on": t["on"]}
    for a, b in (("key", "key"), ("detail", "d"), ("note", "n")):
        if t.get(a): o[b] = t[a]
    if t.get("sight") is not None: o["s"] = t["sight"]
    if t.get("dirs"): o["f"] = t["dirs"]
    things.append(o)
# Where the game already puts some of them (spots John set before: Game.java's ITEM_SPOTS, TRAINER_SPOTS and SPOTS),
# as the corner of a step (x, z whole blocks; one standing at a block's middle goes to the corner nearest).
import math
import re
JAVA = open(os.path.join(HERE, "..", "..", "src", "main", "java", "com", "insidethecube", "retromon", "game", "Game.java"), encoding="utf-8").read()


def spots(name, cols):
    body = JAVA[JAVA.index("static final Object[][] %s = {" % name):]
    body = body[:body.index("};")]
    out = {}
    for m in re.finditer(r'\{"([a-z0-9_]+)",\s*' + r",\s*".join([r"(-?[\d.]+)"] * cols) + r"\}", body):
        v = [float(x) for x in m.groups()[1:]]
        cx, cz = (v[0], v[-1]) if cols == 3 else (v[0] + 0.5, v[1] + 0.5)
        out[m.group(1)] = (math.floor(cx + 0.5), math.floor(cz + 0.5))
    return out


MOD = {}
MOD.update(spots("ITEM_SPOTS", 3))
MOD.update(spots("TRAINER_SPOTS", 2))
PEOPLE_SPOTS = spots("SPOTS", 2)
place_of = {}
for l in open(os.path.join(DATA, "people.tsv"), encoding="utf-8"):
    a = l.rstrip("\n").split("\t")
    if len(a) > 2 and not l.startswith("#"): place_of[a[0]] = a[1]
for t in things:
    if t.get("key") in MOD: t["mx"], t["mz"] = MOD[t["key"]]
# The game's own townsfolk at spots of their own: FireRed's nearest of the same place (within 12 blocks), each once.
mplace = {m["id"]: m["place"] for m in maps}
taken = set()
pairs = []
for key, (x, z) in PEOPLE_SPOTS.items():
    pl = place_of.get(key)
    for i, t in enumerate(things):
        if t["k"] == "person" and mplace.get(t["m"]) == pl:
            pairs.append((math.hypot(t["x"] - x, t["z"] - z), key, i, x, z))
for dd, key, i, x, z in sorted(pairs):
    if dd > 12 or key in taken or "key" in things[i]: continue
    taken.add(key)
    things[i]["key"], things[i]["mx"], things[i]["mz"] = key, x, z
# Where several of a map's own spots agree on a shift from FireRed's fit (four or more within a block of their middle
# one), the fit was a step or more out there: the whole map follows them.
import statistics
from collections import defaultdict
SHIFT = defaultdict(list)
for t in things:
    if "mx" in t: SHIFT[t["m"]].append((t["mx"] - t["x"], t["mz"] - t["z"]))
for m in maps:
    v = SHIFT.get(m["id"], [])
    if len(v) < 4: continue
    dx, dz = round(statistics.median(a for a, _ in v)), round(statistics.median(b for _, b in v))
    if (dx, dz) == (0, 0) or sum(1 for a, b in v if abs(a - dx) <= 1 and abs(b - dz) <= 1) < 4: continue
    m["x0"] += dx; m["x1"] += dx; m["z0"] += dz; m["z1"] += dz
    m["sure"], m["byYours"] = True, True
    for t in things:
        if t["m"] == m["id"]:
            t["x"] += dx
            t["z"] += dz
for t in things:
    # (FireRed's stand-ins whose look the game picks in play.)
    if re.match(r"^VAR \d+$", t["l"]): t["l"] = "PERSON"
os.makedirs(OUT, exist_ok=True)
json.dump({"bounds": d["bounds"], "maps": maps, "things": things}, open(os.path.join(OUT, "placement.json"), "w"), separators=(",", ":"))
shutil.copy(os.path.join(SRC, "kanto.png"), os.path.join(OUT, "kanto.png"))
print(len(things), "things on", len(maps), "maps,", sum(1 for m in maps if m["sure"]), "lined up;", sum(1 for t in things if "mx" in t), "at the game's own spots")
