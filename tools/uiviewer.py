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

def engine():
    s = open(os.path.join(HERE, "monviewer.html")).read()
    return s[s.index("// ---------- Molang"):s.index("// ---------- the stage ----------")]

def main():
    page, out = sys.argv[1], sys.argv[2]
    head = open(os.path.join(HERE, "uiviewer", "head.html")).read()
    shared = [os.path.join(HERE, "interior", n) for n in ("tex.js", "world.js", "figure.js", "creature.js", "rooms.js")]
    own = [os.path.join(HERE, "uiviewer", n) for n in ("ui.js", "scenes.js", "screens1.js", "screens2.js", "screens3.js", "main.js")]
    js = "\n".join(open(p).read() for p in shared + own)
    data = json.dumps(light_data(page), separators=(",", ":"))
    html = (head + '\n<script src="https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js"></script>\n<script>\n'
            + "const DATA = " + data + ";\nconst D2R = Math.PI / 180;\nconst $ = (s, el = document) => el.querySelector(s);\n"
            + engine() + "\n" + js + "\n</script>\n")
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    open(out, "w").write(html)
    print(out, len(html))

if __name__ == "__main__":
    main()
