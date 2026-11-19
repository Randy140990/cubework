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

def engine():
    s = open(os.path.join(HERE, "monviewer.html")).read()
    a = s.index("// ---------- Molang")
    b = s.index("// ---------- the stage ----------")
    return s[a:b]

def main():
    page, out = sys.argv[1], sys.argv[2]
    d = os.path.join(HERE, "interior")
    head = open(os.path.join(d, "head.html")).read()
    js = "\n".join(open(os.path.join(d, n)).read() for n in ("tex.js", "world.js", "rooms.js", "app.js"))
    data = json.dumps(light_data(page), separators=(",", ":"))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const DATA = " + data + ";\nconst D2R = Math.PI / 180;\nconst $ = (s, el = document) => el.querySelector(s);\n"
            + engine() + "\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
