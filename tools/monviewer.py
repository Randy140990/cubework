"""
A page to compare Retromon's own creatures with Cobblemon's models, side by side in 3D, animated as the mod
animates them, with each species' stats and their sizes in blocks.

Run: python3 tools/monviewer.py <out.html>
"""

import base64, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")

FAMILIES = [("Leaf armadillos", ["bulbasaur", "ivysaur", "venusaur"]), ("Ember goats", ["charmander", "charmeleon", "charizard"]),
            ("Sea tapirs", ["squirtle", "wartortle", "blastoise"]), ("Quails", ["pidgey", "pidgeotto", "pidgeot"]),
            ("Jerboas", ["rattata", "raticate"]), ("Quill porcupines ♀", ["nidoran_f", "nidorina", "nidoqueen"]),
            ("Quill porcupines ♂", ["nidoran_m", "nidorino", "nidoking"]), ("Mongooses", ["mankey", "primeape"]), ("Shrikes", ["spearow", "fearow"]),
            ("Gliders", ["pichu", "pikachu", "raichu"])]
KEEP = ("idle", "ground")


def data_uri(identifier):
    ns, path = identifier.split(":", 1)
    p = os.path.join(ROOT, "..", ns, path)
    return "data:image/png;base64," + base64.b64encode(open(p, "rb").read()).decode()


def model(folder):
    info = json.load(open(os.path.join(folder, "info.json")))
    geo = json.load(open(os.path.join(folder, "model.geo.json")))
    anims = json.load(open(os.path.join(folder, "anim.json"))).get("animations", {}) if os.path.exists(os.path.join(folder, "anim.json")) else {}
    wanted = {info.get("idle"), info.get("ground")}
    wanted |= {p.get("anim") for p in info.get("poses", {}).values()}
    wanted |= set(info.get("actions", {}).values())
    anims = {k: v for k, v in anims.items() if k in wanted}
    g = geo["minecraft:geometry"][0]
    cubes = sum(len(b.get("cubes", [])) for b in g["bones"])
    layers = []
    for layer in info.get("layers", []):
        layers.append({"frames": [data_uri(f) for f in layer["frames"]], "fps": layer.get("fps", 8)})
    from PIL import Image
    import numpy as np
    ns, path = info["texture"].split(":", 1)
    im = Image.open(os.path.join(ROOT, "..", ns, path)).convert("RGBA")
    # Texels painted: on the texture, or (for parts that only show glowing, such as flames) on a glowing layer.
    painted = np.array(im)[..., 3] > 0
    for layer in info.get("layers", []):
        for f in layer["frames"][:1]:
            lns, lpath = f.split(":", 1)
            g_im = Image.open(os.path.join(ROOT, "..", lns, lpath)).convert("RGBA")
            if g_im.size == im.size:
                painted |= np.array(g_im)[..., 3] > 0
    texels = int(painted.sum())
    return {"geo": g, "anims": anims, "info": {k: info.get(k) for k in ("scale", "idle", "ground", "parts", "poses", "actions", "name", "move", "attack_move")},
            "texture": data_uri(info["texture"]), "layers": layers,
            "detail": {"cubes": cubes, "bones": len(g["bones"]), "texture": f"{im.size[0]}×{im.size[1]}", "texels": texels, "animations": len(anims)}}


def species_rows():
    out = {}
    for line in open(os.path.join(DATA, "species.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        evo = f[15]
        nxt = None
        if evo and evo != "-":
            parts = evo.split(";")[0].split(":")
            if parts[0] == "level":
                nxt = {"how": f"Level {int(parts[1])}", "level": int(parts[1]), "into": parts[2]}
            elif parts[0] == "item":
                nxt = {"how": parts[1].replace("_", " ").title(), "into": parts[2]}
            elif parts[0] == "happiness":
                nxt = {"how": "Friendship", "into": parts[2]}
        out[f[1]] = {"dex": int(f[0]), "name": f[2], "types": [t for t in (f[3], f[4]) if t and t != "-"],
                     "stats": {"HP": int(f[5]), "Attack": int(f[6]), "Defense": int(f[7]), "Sp. Atk": int(f[9]), "Sp. Def": int(f[10]), "Speed": int(f[8])},
                     "catch": int(f[11]), "exp": int(f[12]), "growth": f[13].replace("_", " ").title(), "evolves": nxt}
    return out


def main(out_path):
    rows = species_rows()
    data = {"families": [], "species": {}}
    for fam, members in FAMILIES:
        data["families"].append({"name": fam, "members": members})
        for sid in members:
            ours = model(os.path.join(ROOT, "mons", sid))
            theirs = model(os.path.join(ROOT, "pokemon", sid))
            data["species"][sid] = {"stats": rows[sid], "ours": ours, "theirs": theirs}
    for sid, d in data["species"].items():
        ev = d["stats"]["evolves"]
        if ev:
            ev["oursName"] = data["species"].get(ev["into"], {}).get("ours", {}).get("info", {}).get("name")
            ev["theirsName"] = rows[ev["into"]]["name"]
    page = open(os.path.join(HERE, "monviewer.html")).read()
    page = page.replace("/*DATA*/null", json.dumps(data, separators=(",", ":")))
    open(out_path, "w").write(page)
    print(out_path, os.path.getsize(out_path) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1])
