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
os.makedirs(OUT, exist_ok=True)
json.dump({"bounds": d["bounds"], "maps": maps, "things": things}, open(os.path.join(OUT, "placement.json"), "w"), separators=(",", ":"))
shutil.copy(os.path.join(SRC, "kanto.png"), os.path.join(OUT, "kanto.png"))
print(len(things), "things on", len(maps), "maps,", sum(1 for m in maps if m["sure"]), "lined up")
