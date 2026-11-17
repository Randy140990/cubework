#!/usr/bin/env python3
"""
Copies the Pokémon models, animations, textures and cries Retromon uses out of a Cobblemon jar, for all 251
Pokémon of Kanto and Johto, plus the Poké Ball model and its textures, into
src/main/resources/assets/retromon. Only the idle animations are kept, and the JSON is written compactly.

Cobblemon's models and textures are licensed CC BY-NC 3.0 by the Cobblemon team: the licence goes with them
(pokemon/LICENSE-cobblemon-models.txt) and CREDITS.txt says what was changed (only trimmed, renamed and
repacked; the models themselves are unchanged).

A few aren't in Cobblemon yet (Raikou, Entei, Suicune, Celebi): they borrow a stand-in's model until they are.

Usage: assets.py <Cobblemon jar>
"""
import json, os, re, shutil, sys, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "retromon")
DATA = os.path.join(ROOT, "src", "main", "resources", "retromon", "data", "species.tsv")
STAND_INS = {"raikou": ("jolteon", 1.6), "entei": ("flareon", 1.7), "suicune": ("vaporeon", 1.6), "celebi": ("bellossom", 1.0)}
BALLS = ["poke_ball", "great_ball", "ultra_ball", "master_ball", "safari_ball", "fast_ball", "friend_ball", "heavy_ball", "level_ball", "love_ball",
         "lure_ball", "moon_ball", "park_ball"]


def species_list():
    out = []
    for line in open(DATA, encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        r = line.rstrip("\n").split("\t")
        out.append((int(r[0]), r[1]))
    return out


def compact(data):
    return json.dumps(data, separators=(",", ":")).encode("utf-8")


def main(jar):
    z = zipfile.ZipFile(jar)
    names = z.namelist()
    nameset = set(names)
    sounds = {}
    licence = None
    for sub in ("pokemon",):
        d = os.path.join(OUT, sub)
        if os.path.isdir(d):
            for f in os.listdir(d):
                p = os.path.join(d, f)
                if os.path.isdir(p):
                    shutil.rmtree(p)
    tdir = os.path.join(OUT, "textures", "pokemon")
    if os.path.isdir(tdir):
        shutil.rmtree(tdir)
    os.makedirs(tdir, exist_ok=True)
    cdir = os.path.join(OUT, "sounds", "cry")
    if os.path.isdir(cdir):
        shutil.rmtree(cdir)
    os.makedirs(cdir, exist_ok=True)
    done, missing = [], []
    by_dex = {dex: sid for dex, sid in species_list()}
    for dex, sid in species_list():
        src_id, scale_mul = STAND_INS.get(sid, (sid, 1.0))
        src_dex = next((d for d, s in by_dex.items() if s == src_id), dex)
        cn = src_id.replace("_", "")
        prefix = f"assets/cobblemon/bedrock/pokemon/resolvers/{src_dex:04d}_"
        res = sorted(n for n in names if n.startswith(prefix) and n.endswith(".json"))
        if not res:
            missing.append(sid)
            continue
        folder = res[0].split("/")[5]
        base = json.loads(z.read(res[0]))
        var = base["variations"][0]
        geo_name = var["model"].split(":")[1]
        geos = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/models/{folder}/") and n.endswith(geo_name + ".json")]
        anims = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/animations/{folder}/") and n.endswith(".animation.json")]
        if not geos:
            missing.append(sid)
            continue
        geo = geos[0]
        poser_ref = var.get("poser", f"cobblemon:{cn}").split(":")[1]
        poser = [n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/posers/{folder}/") and n.endswith(f"/{poser_ref}.json")]
        tex_ref = var["texture"]
        if isinstance(tex_ref, dict):
            tex_ref = tex_ref["frames"][0]
        tex = "assets/cobblemon/" + tex_ref.split(":")[1]
        gen = "generation1" if src_dex <= 151 else "generation2"
        sp_path = f"data/cobblemon/species/{gen}/{cn}.json"
        species = json.loads(z.read(sp_path)) if sp_path in nameset else {}
        d = os.path.join(OUT, "pokemon", sid)
        os.makedirs(d, exist_ok=True)
        open(os.path.join(d, "model.geo.json"), "wb").write(compact(json.loads(z.read(geo))))
        if tex in nameset:
            open(os.path.join(tdir, f"{sid}.png"), "wb").write(z.read(tex))
        # Which animation it idles with in battle (and on the ground), from its poser.
        idle, ground = None, None
        if poser:
            pj = json.loads(z.read(poser[0]))
            for pose in pj.get("poses", {}).values():
                for a in pose.get("animations", []):
                    m = re.match(r"q\.bedrock\('([a-z0-9_]+)', '([a-z0-9_]+)'\)", a) if isinstance(a, str) else None
                    if not m:
                        continue
                    if pose.get("isBattle") and not idle:
                        idle = (m.group(1), m.group(2))
                    if "STAND" in pose.get("poseTypes", []) and not pose.get("isBattle") and not pose.get("isTouchingWater") and not ground:
                        ground = (m.group(1), m.group(2))
        all_anims = {}
        for a in anims:
            try:
                all_anims.update(json.loads(z.read(a)).get("animations", {}))
            except Exception:
                pass
        def key(pair):
            if not pair:
                return None
            k = f"animation.{pair[0]}.{pair[1]}"
            return k if k in all_anims else None
        idle_k, ground_k = key(idle), key(ground)
        if not idle_k:
            idle_k = ground_k or next((k for k in all_anims if k.endswith(".ground_idle")), None) or next((k for k in all_anims if "idle" in k), None)
        keep = {k: all_anims[k] for k in (idle_k, ground_k) if k}
        open(os.path.join(d, "anim.json"), "wb").write(compact({"format_version": "1.8.0", "animations": keep}))
        layers = []
        for i, layer in enumerate(var.get("layers", [])):
            t = layer.get("texture")
            if t is None:
                continue
            frames = t.get("frames") if isinstance(t, dict) else [t]
            out_frames = []
            for j, f in enumerate(frames):
                src = "assets/cobblemon/" + f.split(":")[1]
                if src not in nameset:
                    continue
                name = f"{sid}_layer{i}_{j}.png"
                open(os.path.join(tdir, name), "wb").write(z.read(src))
                out_frames.append(f"retromon:textures/pokemon/{name}")
            if out_frames:
                layers.append({"frames": out_frames, "fps": t.get("fps", 0) if isinstance(t, dict) else 0, "emissive": layer.get("emissive", False),
                               "translucent": layer.get("translucent", False)})
        info = {"scale": species.get("baseScale", 1.0) * scale_mul, "idle": idle_k, "ground": ground_k, "texture": f"retromon:textures/pokemon/{sid}.png",
                "layers": layers, "height": species.get("hitbox", {}).get("height", 1.0) * scale_mul, "standIn": src_id if src_id != sid else None}
        open(os.path.join(d, "info.json"), "wb").write(compact(info))
        cry = f"assets/cobblemon/sounds/pokemon/{cn}/{cn}_cry.ogg"
        if cry in nameset:
            open(os.path.join(cdir, f"{sid}.ogg"), "wb").write(z.read(cry))
            sounds[f"cry.{sid}"] = {"sounds": [{"name": f"retromon:cry/{sid}", "volume": 0.8}]}
        lic = geo.rsplit("/", 1)[0] + "/license"
        if lic in nameset and licence is None:
            licence = z.read(lic)
        done.append(sid)
    # The Poké Ball: one model, a texture per kind of ball.
    bd = os.path.join(OUT, "pokemon", "poke_ball")
    os.makedirs(bd, exist_ok=True)
    open(os.path.join(bd, "model.geo.json"), "wb").write(compact(json.loads(z.read("assets/cobblemon/bedrock/poke_balls/models/poke_ball.geo.json"))))
    open(os.path.join(bd, "anim.json"), "wb").write(compact({"format_version": "1.8.0", "animations": {}}))
    open(os.path.join(bd, "info.json"), "wb").write(compact({"scale": 0.5, "idle": None, "ground": None, "texture": "retromon:textures/pokemon/ball_poke_ball.png",
                                                             "layers": [], "height": 0.3}))
    for b in BALLS:
        src = f"assets/cobblemon/textures/item/poke_balls/models/{b}.png"
        if src in nameset:
            open(os.path.join(tdir, f"ball_{b}.png"), "wb").write(z.read(src))
    # Keep the sounds file's other entries (music and the like) and replace the cries.
    sj_path = os.path.join(OUT, "sounds.json")
    existing = json.load(open(sj_path)) if os.path.exists(sj_path) else {}
    existing = {k: v for k, v in existing.items() if not k.startswith("cry.")}
    existing.update(sounds)
    json.dump(existing, open(sj_path, "w"), indent=1)
    if licence:
        open(os.path.join(OUT, "pokemon", "LICENSE-cobblemon-models.txt"), "wb").write(licence)
    open(os.path.join(OUT, "pokemon", "CREDITS.txt"), "w").write(
        "Pokemon and Poke Ball models, animations, textures and cries: by the Cobblemon team (https://cobblemon.com),\n"
        "licensed under Creative Commons Attribution-NonCommercial 3.0 (see LICENSE-cobblemon-models.txt).\n"
        "Changes: trimmed to the idle animations, renamed and repacked for Retromon; the models, animations and textures\n"
        "are otherwise unchanged. Raikou, Entei, Suicune and Celebi use stand-in models until Cobblemon has them.\n"
        "Retromon is free and non-commercial.\n")
    print(len(done), "Pokémon;", "missing:", missing)


main(sys.argv[1])
