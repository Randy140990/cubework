#!/usr/bin/env python3
"""Downloads Minecraft client jars and writes their block, item, entity and particle lists (with English
names) and block models to out/<version>/, for Tempered Light's ID lists."""
import json, os, re, sys, urllib.request, zipfile

def get(url):
    with urllib.request.urlopen(url, timeout=180) as r:
        return r.read()

manifest = json.loads(get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
for mc in sys.argv[1:]:
    entry = next(v for v in manifest["versions"] if v["id"] == mc)
    info = json.loads(get(entry["url"]))
    jar = f"/tmp/{mc}.jar"
    open(jar, "wb").write(get(info["downloads"]["client"]["url"]))
    out = f"out/{mc}"
    os.makedirs(out, exist_ok=True)
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        lang = json.loads(z.read("assets/minecraft/lang/en_us.json"))
        def ids(pattern):
            return sorted(m.group(1) for n in names for m in [re.match(pattern, n)] if m)
        lists = {
            "blocks": ids(r"assets/minecraft/blockstates/(.+)\.json$"),
            "items": ids(r"assets/minecraft/items/(.+)\.json$"),
            "particles": ids(r"assets/minecraft/particles/(.+)\.json$"),
            "entities": sorted(k[len("entity.minecraft."):] for k in lang if k.startswith("entity.minecraft.") and k.count(".") == 2),
            "block_textures": ids(r"assets/minecraft/textures/block/(.+)\.png$"),
            "shaders": ids(r"assets/minecraft/shaders/(.+)$"),
        }
        for k, v in lists.items():
            with open(f"{out}/{k}.txt", "w") as f:
                for i in v:
                    nm = lang.get(f"block.minecraft.{i}") or lang.get(f"item.minecraft.{i}") or lang.get(f"entity.minecraft.{i}") or ""
                    f.write(f"{i}\t{nm}\n")
        os.makedirs(f"{out}/models", exist_ok=True)
        os.makedirs(f"{out}/blockstates", exist_ok=True)
        for n in names:
            if n.endswith("/"):
                continue
            if n.startswith("assets/minecraft/models/block/"):
                open(f"{out}/models/" + n.split("/")[-1], "wb").write(z.read(n))
            if n.startswith("assets/minecraft/blockstates/"):
                open(f"{out}/blockstates/" + n.split("/")[-1], "wb").write(z.read(n))
        if "version.json" in names:
            open(f"{out}/version.json", "wb").write(z.read("version.json"))
    print(mc, {k: len(v) for k, v in lists.items()})
