#!/usr/bin/env python3
"""Writes Retromon Light's program stubs (world0 and interior), each a few defines and an include."""
import os, sys
root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "retromon-light", "shaders")
HDR = "#version 330 compatibility\n// Retromon Light. Copyright (c) 2026 Insid3 Th3 Cub3. All rights reserved.\n"
world = {
    "shadow": ("shadow", []),
    "gbuffers_terrain": ("lit", ["SUN_SHADOWS", "WAVING_PROGRAM"]),
    "gbuffers_block": ("lit", ["SUN_SHADOWS"]),
    "gbuffers_water": ("lit", ["SUN_SHADOWS", "WATER_PROGRAM"]),
    "gbuffers_entities": ("lit", ["SUN_SHADOWS", "ENTITY"]),
    "gbuffers_hand": ("lit", ["ENTITY"]),
    "gbuffers_hand_water": ("lit", []),
    "gbuffers_textured": ("lit", ["FLAT", "SUN_SHADOWS"]),
    "gbuffers_textured_lit": ("lit", ["FLAT", "SUN_SHADOWS"]),
    "gbuffers_weather": ("lit", ["FLAT"]),
    "gbuffers_clouds": ("lit", ["CLOUDS"]),
    "gbuffers_skybasic": ("sky", []),
    "gbuffers_skytextured": ("sky", ["SKY_TEXTURED"]),
    "gbuffers_basic": ("unlit", ["NO_TEXTURE"]),
    "gbuffers_line": ("unlit", ["NO_TEXTURE"]),
    "gbuffers_lightning": ("unlit", ["NO_TEXTURE", "GLOW"]),
    "gbuffers_spidereyes": ("unlit", ["GLOW"]),
    "gbuffers_beaconbeam": ("unlit", ["GLOW"]),
    "gbuffers_armor_glint": ("unlit", []),
    "gbuffers_damagedblock": ("unlit", []),
    "final": ("final", []),
}
inside = {}
for k, (f, d) in world.items():
    if k == "shadow":
        continue
    d = [x for x in d if x != "SUN_SHADOWS"]
    if k in ("gbuffers_clouds", "gbuffers_weather"):
        f, d = "sky", ["SKY_TEXTURED"]
    inside[k] = (f, ["INSIDE"] + d)
for folder, progs in (("world0", world), ("interior", inside)):
    d0 = os.path.join(root, folder)
    os.makedirs(d0, exist_ok=True)
    for name in os.listdir(d0):
        os.remove(os.path.join(d0, name))
    for k, (f, d) in progs.items():
        for st, ext in (("VSH", "vsh"), ("FSH", "fsh")):
            body = HDR + f"#define {st}\n" + "".join(f"#define {x}\n" for x in d) + f'#include "/program/{f}.glsl"\n'
            open(os.path.join(d0, f"{k}.{ext}"), "w").write(body)
print("stubs:", len(world), "world0,", len(inside), "interior")
