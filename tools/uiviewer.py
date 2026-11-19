#!/usr/bin/env python3
"""Builds the Retromon UI Viewer: tools/uiviewer/* with the creature engine (tools/monviewer.html) and the Interior
Viewer's world, figures and creatures (tools/interior/*), and the creatures' light data from a built Creature Viewer page.

    python3 tools/uiviewer.py <built creature-viewer.html> <out.html>
"""
import json, os, sys

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

MAP_BOX = (-1250, 100, -46, 1000)   # Kanto, in blocks (x0, z0, x1, z1)

def mapdata():
    """The real Town Map of Kanto (baked from the world, as TownMap draws it) and its places, for the Town Map screen."""
    import base64, io, re
    sys.path.insert(0, HERE)
    from townmap_render import render
    im = render(MAP_BOX, 1)
    buf = io.BytesIO(); im.save(buf, "PNG", optimize=True)
    data = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
    rows = lambda f: [l.rstrip("\n").split("\t") for l in open(os.path.join(data, f), encoding="utf-8") if l.strip() and not l.startswith("#")]
    pts = {r[0]: r for r in rows("map_points.tsv") if r[1] == "kanto"}
    land = {r[0]: r[1] for r in rows("place_landmarks.tsv")}
    names = {r[0]: r[2] for r in rows("places.tsv")}
    wild = {}
    for r in rows("wild.tsv"):
        k = land.get(r[0], r[0])
        for slot in r[4].split(","):
            sp = slot.split(":")[0]
            if sp and sp not in wild.setdefault(k, []): wild[k].append(sp)
    here = {}
    for place, k in land.items():
        if place != k and place in names: here.setdefault(k, []).append(names[place])
    java = open(os.path.join(HERE, "..", "src", "main", "java", "com", "insidethecube", "retromon", "client", "Adventure.java"), encoding="utf-8").read()
    signs = dict(re.findall(r'entry\("([A-Z .\']+)", "([^"]+)"\)', java))
    out = []
    for l in open(os.path.join(HERE, "uiviewer", "kanto_points.tsv"), encoding="utf-8"):
        if l.startswith("#") or not l.strip(): continue
        c = l.rstrip("\n").split("\t")
        key, x, z = c[0], int(c[1]), int(c[2])
        mp = pts.get(key)
        name = mp[5] if mp else names.get(key, key.upper().replace("_", " "))
        kind = mp[4] if mp else "spot"
        o = {"key": key, "name": name, "kind": kind, "x": x, "z": z, "wild": wild.get(key, []), "here": sorted(set(here.get(key, [])))[:8], "tag": signs.get(name, "")}
        if len(c) > 3 and c[3]: o["zone"] = [int(v) for v in c[3].split(",")]
        out.append(o)
    return {"img": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode(), "box": MAP_BOX, "w": im.width, "h": im.height, "points": out}

def engine():
    s = open(os.path.join(HERE, "monviewer.html")).read()
    return s[s.index("// ---------- Molang"):s.index("// ---------- the stage ----------")]

def main():
    page, out = sys.argv[1], sys.argv[2]
    head = open(os.path.join(HERE, "uiviewer", "head.html")).read()
    shared = [os.path.join(HERE, "interior", n) for n in ("tex.js", "world.js", "figure.js", "creature.js", "rooms.js")]
    own = [os.path.join(HERE, "uiviewer", n) for n in ("ui.js", "scenes.js", "screens1.js", "screens2.js", "screens3.js", "screens4.js", "main.js")]
    js = "\n".join(open(p).read() for p in shared + own)
    data = json.dumps(light_data(page), separators=(",", ":"))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const DATA = " + data + ";\nconst SKINDATA = " + json.dumps(skindata(), separators=(",", ":")) + ";\nconst MAPDATA = " + json.dumps(mapdata(), separators=(",", ":")) + ";\nconst D2R = Math.PI / 180;\nconst $ = (s, el = document) => el.querySelector(s);\n"
            + engine() + "\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
