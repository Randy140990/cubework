#!/usr/bin/env python3
"""Builds the Retromon Interior Viewer: one page from tools/interior/*, with the creature engine taken from the
Creature Viewer (tools/monviewer.html) so followers move exactly as they do there, and the creatures' light data
taken from a built Creature Viewer page. Their models are fetched at run time from mons/<sid>.json beside the page.

    python3 tools/interiorviewer.py <built creature-viewer.html> <out.html>
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
                        "ours": {"info": d["ours"]["info"]}, "theirs": {"info": d["theirs"]["info"]}}
    fams = [f for f in data["families"] if all(m in species for m in f["members"])]
    return {"families": fams, "species": species}

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

def worldmap():
    """The world map picture (tools/worldmap.py), as raw RGB for the wall maps and the Town Map view."""
    import base64
    from PIL import Image
    im = Image.open(os.path.join(HERE, "interior", "worldmap.png")).convert("RGB")
    return {"w": im.width, "h": im.height, "d": base64.b64encode(im.tobytes()).decode()}

def packtex():
    """The pictures from John's asset pack for the rooms' plants and pots (tools/propsart.py), as raw RGBA."""
    import base64, glob
    from PIL import Image
    out = {}
    for f in sorted(glob.glob(os.path.join(HERE, "art", "shards_props", "*.png"))):
        n = os.path.splitext(os.path.basename(f))[0]
        if n in ("cobblestone", "mossy_cobblestone", "leaf_litter"): continue
        im = Image.open(f).convert("RGBA")
        out[n] = {"w": im.width, "h": im.height, "d": base64.b64encode(im.tobytes()).decode()}
    return out

def areatex():
    """The mod's textures for the looks only some places have (client/AreaLooks, tools/lookarea.py), and the Look
    pack's dripstone, as raw RGBA, for the "Pallet Town looks" view."""
    import base64, glob
    from PIL import Image
    out = {}
    root = os.path.join(HERE, "..", "src", "main", "resources")
    for f in sorted(glob.glob(os.path.join(root, "assets", "retromon", "textures", "block", "area", "*.png"))):
        n = os.path.splitext(os.path.basename(f))[0]
        im = Image.open(f).convert("RGBA")
        out[n] = {"w": im.width, "h": im.height, "d": base64.b64encode(im.tobytes()).decode()}
    im = Image.open(os.path.join(root, "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "block", "dripstone_block.png")).convert("RGBA")
    out["dripstone"] = {"w": im.width, "h": im.height, "d": base64.b64encode(im.tobytes()).decode()}
    return out

def boulders():
    """The mod's boulder models (tools/lookarea.py), their boxes, for the Diglett's Cave rooms."""
    root = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "models", "block", "area")
    out = {}
    for n in ("boulder_a", "boulder_b", "boulder_small", "iboulder_a", "iboulder_b", "iboulder_small", "rboulder_a", "rboulder_b", "rboulder_small"):
        m = json.load(open(os.path.join(root, n + ".json")))
        out[n] = [e["from"] + e["to"] for e in m["elements"]]
    # (0.53) The plateau's statues of our RHYDON (tools/mon_statue.py): each box, and its turn (axis, angle, origin).
    m = json.load(open(os.path.join(root, "prop_mon_statue.json")))
    out["prop_mon_statue"] = [e["from"] + e["to"] + ([e["rotation"]["axis"], e["rotation"]["angle"]] + e["rotation"]["origin"] if "rotation" in e else [])
                              for e in m["elements"]]
    return out

def engine():
    s = open(os.path.join(HERE, "monviewer.html")).read()
    a = s.index("// ---------- Molang")
    b = s.index("// ---------- the stage ----------")
    return s[a:b]

def main():
    page, out = sys.argv[1], sys.argv[2]
    d = os.path.join(HERE, "interior")
    head = open(os.path.join(d, "head.html")).read()
    js = "\n".join(open(os.path.join(d, n)).read() for n in ("tex.js", "world.js", "figure.js", "creature.js", "forest_data.js", "cave_data.js", "mtmoon_data.js", "safari_data.js", "cerulean_pool_data.js", "caves48_data.js", "rooms.js", "rooms47.js", "rooms48.js", "rooms50.js", "rooms52.js", "rooms53.js", "rooms54.js", "rooms55.js", "silph_data.js", "rooms56.js", "rooms57.js", "app.js"))
    data = json.dumps(light_data(page), separators=(",", ":"))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const DATA = " + data + ";\nconst WORLDMAP = " + json.dumps(worldmap(), separators=(",", ":")) + ";\nconst SKINDATA = " + json.dumps(skindata(), separators=(",", ":")) + ";\nconst PACKTEX = " + json.dumps(packtex(), separators=(",", ":")) + ";\nconst AREATEX = " + json.dumps(areatex(), separators=(",", ":")) + ";\nconst BOULDERS = " + json.dumps(boulders(), separators=(",", ":")) + ";\nconst D2R = Math.PI / 180;\nconst $ = (s, el = document) => el.querySelector(s);\n"
            + engine() + "\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
