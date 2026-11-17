#!/usr/bin/env python3
"""
Copies the Pokémon models, animations, textures and cries Route One uses out of a Cobblemon jar, into
src/main/resources/assets/kantojohto. Cobblemon's models and textures are licensed CC BY-NC 3.0 by the
Cobblemon team: the licence goes with them (pokemon/LICENSE-cobblemon-models.txt) and CREDITS.txt says
what was changed (only renamed and repacked; the models themselves are unchanged).

Usage: assets.py <Cobblemon jar>
"""
import json, os, re, sys, zipfile

SPECIES = [("bulbasaur", "0001_bulbasaur"), ("charmander", "0004_charmander"), ("squirtle", "0007_squirtle"), ("pidgey", "0016_pidgey"),
           ("rattata", "0019_rattata"), ("geodude", "0074_geodude"), ("onix", "0095_onix")]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "kantojohto")

def path_of(ref, kind):
    ns, p = ref.split(":")
    if kind == "model":
        return None, p  # name of a .geo, found by search
    return f"assets/{ns}/{p}", None

def main(jar):
    z = zipfile.ZipFile(jar)
    names = z.namelist()
    sounds = {}
    licence = None
    for sid, folder in SPECIES:
        res = sorted(n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/resolvers/{folder}/") and n.endswith(".json"))
        base = json.loads(z.read(res[0]))
        var = base["variations"][0]
        geo_name = var["model"].split(":")[1]  # e.g. bulbasaur_male.geo
        geo = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/models/{folder}/") and n.endswith(geo_name + ".json")][0]
        anim = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/animations/{folder}/") and n.endswith(f"{sid}.animation.json")][0]
        poser_ref = var.get("poser", f"cobblemon:{sid}").split(":")[1]
        poser = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/posers/{folder}/") and n.endswith(f"/{poser_ref}.json")]
        tex = "assets/cobblemon/" + var["texture"].split(":")[1]
        species = json.loads(z.read(f"data/cobblemon/species/generation1/{sid}.json"))
        d = os.path.join(OUT, "pokemon", sid)
        os.makedirs(d, exist_ok=True)
        open(os.path.join(d, "model.geo.json"), "wb").write(z.read(geo))
        open(os.path.join(d, "anim.json"), "wb").write(z.read(anim))
        tdir = os.path.join(OUT, "textures", "pokemon")
        os.makedirs(tdir, exist_ok=True)
        open(os.path.join(tdir, f"{sid}.png"), "wb").write(z.read(tex))
        # Which animation it idles with in battle (and on the ground), from its poser.
        idle, ground = None, None
        if poser:
            pj = json.loads(z.read(poser[0]))
            for pose in pj.get("poses", {}).values():
                for a in pose.get("animations", []):
                    m = re.match(r"q\.bedrock\('([a-z_]+)', '([a-z_]+)'\)", a)
                    if not m: continue
                    if pose.get("isBattle") and not idle: idle = m.group(2)
                    if "STAND" in pose.get("poseTypes", []) and not pose.get("isBattle") and not pose.get("isTouchingWater") and not ground: ground = m.group(2)
        anims = json.loads(z.read(anim))["animations"]
        def has(n): return n and f"animation.{sid}.{n}" in anims
        if not has(idle): idle = ground if has(ground) else ("ground_idle" if has("ground_idle") else None)
        layers = []
        for i, layer in enumerate(var.get("layers", [])):
            t = layer.get("texture")
            frames = t.get("frames") if isinstance(t, dict) else [t]
            out_frames = []
            for j, f in enumerate(frames):
                name = f"{sid}_layer{i}_{j}.png"
                open(os.path.join(tdir, name), "wb").write(z.read("assets/cobblemon/" + f.split(":")[1]))
                out_frames.append(f"kantojohto:textures/pokemon/{name}")
            layers.append({"frames": out_frames, "fps": t.get("fps", 0) if isinstance(t, dict) else 0, "emissive": layer.get("emissive", False), "translucent": layer.get("translucent", False)})
        info = {"scale": species.get("baseScale", 1.0), "idle": f"animation.{sid}.{idle}" if idle else None,
                "ground": f"animation.{sid}.{ground}" if has(ground) else None, "texture": f"kantojohto:textures/pokemon/{sid}.png", "layers": layers,
                "height": species.get("hitbox", {}).get("height", 1.0)}
        json.dump(info, open(os.path.join(d, "info.json"), "w"), indent=1)
        cry = f"assets/cobblemon/sounds/pokemon/{sid}/{sid}_cry.ogg"
        if cry in names:
            os.makedirs(os.path.join(OUT, "sounds", "cry"), exist_ok=True)
            open(os.path.join(OUT, "sounds", "cry", f"{sid}.ogg"), "wb").write(z.read(cry))
            sounds[f"cry.{sid}"] = {"sounds": [{"name": f"kantojohto:cry/{sid}", "volume": 0.8}]}
        licence = z.read(geo.rsplit("/", 1)[0] + "/license")
        print(sid, geo_name, "idle:", info["idle"], "layers:", len(layers), "scale:", info["scale"])
    json.dump(sounds, open(os.path.join(OUT, "sounds.json"), "w"), indent=1)
    open(os.path.join(OUT, "pokemon", "LICENSE-cobblemon-models.txt"), "wb").write(licence)
    open(os.path.join(OUT, "pokemon", "CREDITS.txt"), "w").write(
        "Pokemon models, animations, textures and cries: by the Cobblemon team (https://cobblemon.com),\n"
        "licensed under Creative Commons Attribution-NonCommercial 3.0 (see LICENSE-cobblemon-models.txt).\n"
        "Changes: renamed and repacked for Route One; the models, animations and textures are otherwise unchanged.\n"
        "Route One is free and non-commercial.\n")

main(sys.argv[1])
