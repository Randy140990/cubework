#!/usr/bin/env python3
"""
Checks one version's generated resources against that version's game: every model, parent and texture
they name must exist (in Cubework's resources or the vanilla jar). Prints what's missing.
Usage: refs.py <resources dir> <client.jar>
"""
import glob, json, os, sys, zipfile

res, jar = sys.argv[1], sys.argv[2]
names = set(zipfile.ZipFile(jar).namelist())
A = os.path.join(res, "assets", "cubework")
ours = set(os.path.relpath(p, os.path.join(A, "models"))[:-5].replace(os.sep, "/") for p in glob.glob(A + "/models/**/*.json", recursive=True))
missing_models, missing_textures = set(), set()
# Sprites painted in the game (the coloured and metal doors) aren't files.
painted = ("cubework:block/", "cubework:item/")


def model(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    if ns == "cubework":
        if path not in ours:
            missing_models.add(ref)
    elif "assets/minecraft/models/%s.json" % path not in names and not path.startswith("builtin/"):
        missing_models.add(ref)


def texture(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    if ns == "minecraft" and "assets/minecraft/textures/%s.png" % path not in names:
        missing_textures.add(ref)
    elif ns == "cubework" and path.startswith("block/pattern/"):
        # Patterned sprites are painted in the game from the vanilla texture of the same name.
        if "assets/minecraft/textures/block/%s.png" % path.split("/")[-1] not in names:
            missing_textures.add(ref)
    elif ns == "cubework" and not os.path.exists(os.path.join(A, "textures", path + ".png")) and "door" not in path:
        missing_textures.add(ref)


def walk(x):
    if isinstance(x, dict):
        for k, v in x.items():
            if k in ("model", "parent") and isinstance(v, str):
                model(v)
            elif k == "models" and isinstance(v, list) and all(isinstance(x, str) for x in v):
                for x in v:
                    model(x)
            elif k == "textures" and isinstance(v, dict):
                for t in v.values():
                    if isinstance(t, str) and not t.startswith("#"):
                        texture(t)
            else:
                walk(v)
    elif isinstance(x, list):
        for v in x:
            walk(v)


for p in glob.glob(A + "/blockstates/*.json") + glob.glob(A + "/joined/*.json") + glob.glob(A + "/looks/**/*.json", recursive=True) + glob.glob(A + "/items/*.json") + glob.glob(A + "/models/**/*.json", recursive=True):
    walk(json.load(open(p)))
# The textures the atlas sources paint from.
for atlas in glob.glob(os.path.join(res, "assets", "minecraft", "atlases", "*.json")):
    for src in json.load(open(atlas)).get("sources", []):
        for t in src.get("textures", []) + src.get("metals", []):
            texture(t)
print("missing models:", len(missing_models), sorted(missing_models)[:30])
print("missing textures:", len(missing_textures), sorted(missing_textures)[:30])
