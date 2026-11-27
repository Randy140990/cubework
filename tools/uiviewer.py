#!/usr/bin/env python3
"""Builds the Retromon UI Viewer: tools/uiviewer/* with the creature engine (tools/monviewer.html) and the Interior
Viewer's world, figures and creatures (tools/interior/*), and the creatures' light data from a built Creature Viewer page.

    python3 tools/uiviewer.py <built creature-viewer.html> <out.html>
"""
import base64, json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from interiorviewer import worldmap, packtex

HERE = os.path.dirname(os.path.abspath(__file__))

def light_data(page):
    s = open(page).read()
    i = s.index("const DATA = ") + len("const DATA = ")
    data, _ = json.JSONDecoder().raw_decode(s[i:])
    species = {}
    for sid, d in data["species"].items():
        st = d.get("stats") or {}
        species[sid] = {"dex": st.get("dex", 0), "pname": (st.get("name") or sid).title().replace("_", " "),
                        "stats": {"types": st.get("types", ["NORMAL"]), "stats": st.get("stats", {})},
                        "ours": {"info": d["ours"]["info"]}, "theirs": {"info": d["theirs"]["info"]}}
    moves = [{k: m.get(k) for k in ("name", "type", "category", "power", "accuracy", "pp", "desc")} for m in data["moves"]]
    return {"families": data["families"], "species": species, "moves": moves}

def skindata():
    import base64, glob
    root = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures")
    out = {}
    for grp, sub, pat in (("npc", "entity/npc", "*.png"), ("player", "entity/player", "*.png"), ("outfit", "outfit", "*.png"), ("gear", "gear", "*.png")):
        out[grp] = {}
        for f in sorted(glob.glob(os.path.join(root, sub, pat))):
            n = os.path.splitext(os.path.basename(f))[0]
            if grp == "gear" and not (n.startswith("bag_") or n in ("cap_plain", "cap_pokeball", "cap_greatball", "cap_ultraball")): continue
            out[grp][n] = "data:image/png;base64," + base64.b64encode(open(f, "rb").read()).decode()
    return out

MAP_BOX = (-1250, 120, -46, 1000)   # Kanto, in blocks (x0, z0, x1, z1); the top stops short of the island to the north
JOHTO_BOX = (-2780, 140, -1150, 1000)   # Johto: just above the Lake of Rage to just below the forest point in the south; (0.70) out to Route 48 in the west

METHOD = {"grass": "Grass", "water": "Surfing", "old_rod": "Fishing", "good_rod": "Fishing", "super_rod": "Fishing", "headbutt": "Headbutt",
          "rock_smash": "Rock Smash", "contest": "Bug-Catching Contest", "swarm": "Swarms"}
SPOT_NAME = {"viridian_forest": "VIRIDIAN FOREST", "pokemon_tower": "POKéMON TOWER", "safari_zone": "SAFARI ZONE", "pokemon_mansion": "POKéMON MANSION",
             "cerulean_cave": "CERULEAN CAVE", "digletts_cave": "DIGLETT's CAVE", "power_plant": "POWER PLANT", "tohjo_falls": "TOHJO FALLS"}

def mapdata():
    """The real Town Map (baked from the world, as TownMap draws it) of Kanto and of Johto, with their places, for the Town Map screen."""
    import base64, io, re
    sys.path.insert(0, HERE)
    from townmap_render import render
    data = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
    rows = lambda f: [l.rstrip("\n").split("\t") for l in open(os.path.join(data, f), encoding="utf-8") if l.strip() and not l.startswith("#")]
    land = {r[0]: r[1] for r in rows("place_landmarks.tsv")}
    names = {r[0]: r[2] for r in rows("places.tsv")}
    wildrows = rows("wild.tsv")
    java = open(os.path.join(HERE, "..", "src", "main", "java", "com", "insidethecube", "retromon", "client", "Adventure.java"), encoding="utf-8").read()
    signs = dict(re.findall(r'entry\("([A-Z .\']+)", "([^"]+)"\)', java))
    def region(box, tsv, reg):
        im = render(box, 1)
        buf = io.BytesIO(); im.save(buf, "PNG", optimize=True)
        pts = {r[0]: r for r in rows("map_points.tsv") if r[1] == reg}
        mine = []
        for l in open(os.path.join(HERE, "uiviewer", tsv), encoding="utf-8"):
            if l.startswith("#") or not l.strip(): continue
            mine.append(l.rstrip("\n").split("\t"))
        keys = [c[0] for c in mine]
        # Which point a place's wild POKéMON belong to: its own point, a point it's a part of (safari_zone_east ->
        # safari_zone), or the point of its landmark.
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
        out = []
        for c in mine:
            key, x, z = c[0], int(c[1]), int(c[2])
            mp = pts.get(key)
            name = SPOT_NAME.get(key) or (mp[5] if mp else names.get(key, key.upper().replace("_", " ")))
            kind = mp[4] if mp and key not in SPOT_NAME else "spot" if key in SPOT_NAME or not mp else mp[4]
            o = {"key": key, "name": name, "kind": kind, "x": x, "z": z, "wild": wild.get(key, []), "how": how.get(key, []), "here": sorted(set(here.get(key, [])) - {name})[:8], "tag": signs.get(name, "")}
            if len(c) > 3 and c[3]: o["zone"] = [int(v) for v in c[3].split(",")]
            out.append(o)
        return {"img": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode(), "box": box, "w": im.width, "h": im.height, "points": out}
    k = region(MAP_BOX, "kanto_points.tsv", "kanto")
    k["johto"] = region(JOHTO_BOX, "johto_points.tsv", "johto")
    return k

def emote_pngs():
    """The emote bubbles (tools/emotes.py's), as the game has them, for the screens that show one."""
    d = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "gui", "emote")
    out = {}
    for n in ("heart", "star", "exclaim", "note", "happy", "dots"):
        with open(os.path.join(d, n + ".png"), "rb") as f:
            out[n] = "data:image/png;base64," + base64.b64encode(f.read()).decode()
    return out


def wordmark_png():
    import base64
    f = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "textures", "gui", "v", "wordmark_ours.png")
    return "data:image/png;base64," + base64.b64encode(open(f, "rb").read()).decode()

def engine():
    s = open(os.path.join(HERE, "monviewer.html")).read()
    return s[s.index("// ---------- Molang"):s.index("// ---------- the stage ----------")]

def main():
    page, out = sys.argv[1], sys.argv[2]
    head = open(os.path.join(HERE, "uiviewer", "head.html")).read()
    shared = [os.path.join(HERE, "interior", n) for n in ("tex.js", "world.js", "figure.js", "creature.js", "rooms.js")]
    own = [os.path.join(HERE, "uiviewer", n) for n in ("lang.js", "ui.js", "items.js", "faces.js", "scenes.js", "screens1.js", "screens2.js", "screens3.js", "roster", "screens4.js", "main.js")]
    def src(p):
        # The Person Viewer's roster (everyone in the mod), wrapped so its names don't clash: the cast screens show the same people.
        if p.endswith("roster"):
            pv = os.path.join(HERE, "personviewer")
            return "const ROSTER = (() => {\n" + open(os.path.join(pv, "roster.js")).read() + "\n" + open(os.path.join(pv, "more.js")).read() + "\nreturn { GROUPS, PEOPLE };\n})();"
        return open(p).read()
    js = "\n".join(src(p) for p in shared + own)
    data = json.dumps(light_data(page), separators=(",", ":"))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const DATA = " + data + ";\nconst WORDMARK_OURS = " + json.dumps(wordmark_png()) + ";\nconst EMOTE = " + json.dumps(emote_pngs()) + ";\nconst SKINDATA = " + json.dumps(skindata(), separators=(",", ":")) + ";\nconst MAPDATA = " + json.dumps(mapdata(), separators=(",", ":")) + ";\nconst WORLDMAP = " + json.dumps(worldmap(), separators=(",", ":")) + ";\nconst PACKTEX = " + json.dumps(packtex(), separators=(",", ":")) + ";\nconst D2R = Math.PI / 180;\nconst $ = (s, el = document) => el.querySelector(s);\n"
            + engine() + "\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
