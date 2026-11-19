#!/usr/bin/env python3
"""Builds the Retromon Person Viewer: everyone in the mod (tools/personviewer/roster.js) on Minecraft's player model in
their own skins (textures/entity/npc), using the Interior Viewer's figure code. Creature names for the teams come from a
built Creature Viewer page.

    python3 tools/personviewer.py <built creature-viewer.html> <out.html>
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from interiorviewer import skindata

def names(page):
    s = open(page).read()
    i = s.index("const DATA = ") + len("const DATA = ")
    data, _ = json.JSONDecoder().raw_decode(s[i:])
    out = {}
    for sid, d in data["species"].items():
        st = d.get("stats") or {}
        ours = d["ours"]["info"].get("name") or sid.upper()
        out[sid] = [ours, (st.get("name") or sid).replace("_", " ").title()]
    return out

def main():
    page, out = sys.argv[1], sys.argv[2]
    head = open(os.path.join(HERE, "personviewer", "head.html")).read()
    js = "\n".join(open(os.path.join(HERE, *p)).read() for p in (("interior", "figure.js"), ("personviewer", "roster.js"), ("personviewer", "app.js")))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const SKINDATA = " + json.dumps(skindata(), separators=(",", ":")) + ";\nconst NAMES = " + json.dumps(names(page), separators=(",", ":"))
            + ";\nconst $ = (s, el = document) => el.querySelector(s);\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
