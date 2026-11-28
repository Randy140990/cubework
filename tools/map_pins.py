#!/usr/bin/env python3
"""Writes the Town Map's places as the UI Viewer has them (tools/uiviewer/kanto_points.tsv and johto_points.tsv, where
John's moved places go) into the mod's data: src/main/resources/retromon/data/map_pins.tsv. One row a place:

    region  key  x  z  kind  name  zone(x1,z1,x2,z2)  how(a|b)  wild(a,b)  here(a|b)

The same claims as the viewer's mapdata() (tools/uiviewer.py): a place's wild POKéMON belong to its own pin, a pin it's
a part of (safari_zone_east -> safari_zone), or the pin of its landmark.

    python3 tools/map_pins.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
METHOD = {"grass": "Grass", "water": "Surfing", "old_rod": "Fishing", "good_rod": "Fishing", "super_rod": "Fishing", "headbutt": "Headbutt",
          "rock_smash": "Rock Smash", "contest": "Bug-Catching Contest", "swarm": "Swarms"}
SPOT_NAME = {"viridian_forest": "VIRIDIAN FOREST", "pokemon_tower": "POKéMON TOWER", "safari_zone": "SAFARI ZONE", "pokemon_mansion": "POKéMON MANSION",
             "cerulean_cave": "CERULEAN CAVE", "digletts_cave": "DIGLETT's CAVE", "power_plant": "POWER PLANT", "tohjo_falls": "TOHJO FALLS",
             # ((0.76) Johto's far west.)
             "johto_safari_zone": "SAFARI ZONE", "johto_safari_gate": "SAFARI ZONE GATE", "cliff_edge_gate": "CLIFF EDGE GATE", "cliff_cave": "CLIFF CAVE"}


def rows(f):
    return [l.rstrip("\n").split("\t") for l in open(os.path.join(DATA, f), encoding="utf-8") if l.strip() and not l.startswith("#")]


def main():
    land = {r[0]: r[1] for r in rows("place_landmarks.tsv")}
    names = {r[0]: r[2] for r in rows("places.tsv")}
    wildrows = rows("wild.tsv")
    out = ["# The Town Map's places, as the UI Viewer has them (written by tools/map_pins.py from tools/uiviewer/*_points.tsv).",
           "# region\tkey\tx\tz\tkind\tname\tzone\thow\twild\there"]
    for reg, tsv in (("kanto", "kanto_points.tsv"), ("johto", "johto_points.tsv")):
        pts = {r[0]: r for r in rows("map_points.tsv") if r[1] == reg}
        mine = [l.rstrip("\n").split("\t") for l in open(os.path.join(HERE, "uiviewer", tsv), encoding="utf-8") if l.strip() and not l.startswith("#")]
        keys = [c[0] for c in mine]

        def claim(pl):
            if pl in keys: return pl
            for k in sorted(keys, key=len, reverse=True):
                if pl.startswith(k + "_"): return k
            lm = land.get(pl, pl)
            return lm if lm in keys else None
        wild, how, here = {}, {}, {}
        for r in wildrows:
            k = claim(r[0])
            if not k: continue
            how.setdefault(k, [])
            m = METHOD.get(r[1], r[1])
            if m not in how[k]: how[k].append(m)
            for slot in r[4].split(","):
                sp = slot.split(":")[0]
                if sp and sp not in wild.setdefault(k, []): wild[k].append(sp)
        for pl, lm in land.items():
            k = claim(pl)
            if k and pl != k and pl in names and not pl.startswith(k + "_"): here.setdefault(k, []).append(names[pl])
        for c in mine:
            key, x, z = c[0], int(c[1]), int(c[2])
            mp = pts.get(key)
            name = SPOT_NAME.get(key) or (mp[5] if mp else names.get(key, key.upper().replace("_", " ")))
            kind = mp[4] if mp and key not in SPOT_NAME else "spot" if key in SPOT_NAME or not mp else mp[4]
            if not mp and key.startswith("route_"): kind = "route"
            zone = c[3] if len(c) > 3 and c[3] else ""
            hs = sorted(set(here.get(key, [])) - {name})[:8]
            out.append("\t".join([reg, key, str(x), str(z), kind, name, zone or "-", "|".join(how.get(key, [])) or "-", ",".join(wild.get(key, [])) or "-", "|".join(hs) or "-"]))
    open(os.path.join(DATA, "map_pins.tsv"), "w", encoding="utf-8").write("\n".join(out) + "\n")
    print(len(out) - 2, "places")


if __name__ == "__main__":
    main()
