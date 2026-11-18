#!/usr/bin/env python3
"""
The Town Map's colours: for every vanilla block, the colour it shows from above (the average of its top texture,
tinted as the plains biome tints grass, leaves and water), written to
src/main/resources/assets/retromon/townmap/palette.json as {"minecraft:oak_planks": "a2834f", ...}.

    python3 tools/mappalette.py <folder holding assets/minecraft/{blockstates,models,textures}>

Blocks the map should see through (grass tufts, torches, signs...) aren't listed; the map looks under them.
"""
import json, os, sys

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "townmap", "palette.json")

GRASS, FOLIAGE, WATER = (0x91, 0xBD, 0x59), (0x77, 0xAB, 0x2F), (0x3F, 0x76, 0xE4)
TINT = {
    "grass_block": GRASS, "short_grass": GRASS, "tall_grass": GRASS, "fern": GRASS, "large_fern": GRASS, "sugar_cane": GRASS,
    "oak_leaves": FOLIAGE, "jungle_leaves": FOLIAGE, "acacia_leaves": FOLIAGE, "dark_oak_leaves": FOLIAGE, "mangrove_leaves": (0x92, 0xC0, 0x48),
    "vine": FOLIAGE, "birch_leaves": (0x80, 0xA7, 0x55), "spruce_leaves": (0x61, 0x99, 0x61), "lily_pad": (0x20, 0x80, 0x30),
    "water": WATER, "bubble_column": WATER, "attached_melon_stem": FOLIAGE, "attached_pumpkin_stem": FOLIAGE,
}
KEYS = ("top", "up", "end", "all", "texture", "pattern", "cross", "plant", "particle", "side", "front")


def load(root, kind, name):
    p = os.path.join(root, "assets", "minecraft", kind, name + ".json")
    if not os.path.exists(p):
        return None
    with open(p) as f:
        return json.load(f)


def model_textures(root, model):
    """The model's textures, with its parents' filled in and #references resolved."""
    tex = {}
    seen = 0
    while model and seen < 12:
        seen += 1
        m = load(root, "models", model.replace("minecraft:", ""))
        if not m:
            break
        for k, v in (m.get("textures") or {}).items():
            tex.setdefault(k, v)
        model = m.get("parent")
    for _ in range(6):
        for k, v in list(tex.items()):
            if isinstance(v, str) and v.startswith("#"):
                tex[k] = tex.get(v[1:], v)
    return tex


def first_model(bs):
    if "variants" in bs:
        v = next(iter(bs["variants"].values()))
        if isinstance(v, list):
            v = v[0]
        return v.get("model")
    if "multipart" in bs:
        for part in bs["multipart"]:
            a = part.get("apply")
            if isinstance(a, list):
                a = a[0]
            if a and a.get("model"):
                return a["model"]
    return None


def average(root, texname):
    p = os.path.join(root, "assets", "minecraft", "textures", texname.replace("minecraft:", "") + ".png")
    if not os.path.exists(p):
        return None
    im = np.asarray(Image.open(p).convert("RGBA")).astype(np.float64)
    h = im.shape[1]
    im = im[:h]  # animated textures: the first frame
    a = im[..., 3] / 255.0
    w = (a > 0.5).astype(np.float64)
    if w.sum() < 1:
        return None
    rgb = (im[..., :3] * w[..., None]).sum(axis=(0, 1)) / w.sum()
    return rgb, w.mean()


def main():
    root = sys.argv[1]
    out = {}
    bsdir = os.path.join(root, "assets", "minecraft", "blockstates")
    for fn in sorted(os.listdir(bsdir)):
        name = fn[:-5]
        bs = load(root, "blockstates", name)
        model = first_model(bs) if bs else None
        tex = model_textures(root, model) if model else {}
        pick = None
        for k in KEYS:
            if k in tex and not str(tex[k]).startswith("#"):
                pick = tex[k]
                break
        if name in ("water", "bubble_column"):
            pick = "minecraft:block/water_still"
        if name == "lava":
            pick = "minecraft:block/lava_still"
        if not pick:
            continue
        av = average(root, pick)
        if not av:
            continue
        rgb, cover = av
        if name in TINT:
            t = np.array(TINT[name], np.float64)
            grey = rgb.mean()
            rgb = t * (grey / 255.0) * 1.15 if name != "water" else t
        rgb = np.clip(rgb, 0, 255).astype(int)
        out["minecraft:" + name] = "%02x%02x%02x" % tuple(rgb)
    # Snowy grass from above is snow.
    if "minecraft:snow_block" in out:
        out["minecraft:grass_block[snowy]"] = out["minecraft:snow_block"]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(out, f, separators=(",", ":"), sort_keys=True)
    print(len(out), "blocks ->", OUT)


if __name__ == "__main__":
    main()
