#!/usr/bin/env python3
"""
(0.78, John's 14:54) The LAKE OF RAGE's red GYARADOS: "our Gyarados equivalent in a primarily different colour". Its
own colours (gyarados_shiny: the mod draws a shiny one so, game/Game.java's red GYARADOS is shiny): Retromon's own
GYARADOS (mons/gyarados) with its slate-grey body deep crimson, its red-orange fins and crest gold, its cream and its
eyes as they are. Its model and moves are the GYARADOS's own (info.json points at the new texture).

    python3 tools/shiny78.py
"""
import colorsys, json, os, shutil

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")


def recolour(src, out):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.size[1]):
        for x in range(im.size[0]):
            r, g, b, a = px[x, y]
            if a == 0: continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.22 and 0.25 < v < 0.9 and b >= r - 2:
                # The slate body: crimson, its shading kept.
                nr, ng, nb = colorsys.hsv_to_rgb(0.985, 0.72, min(1, v * 1.02))
            elif s > 0.45 and (h < 0.07 or h > 0.94) and v > 0.72:
                # The red-orange fins and crest: gold.
                nr, ng, nb = colorsys.hsv_to_rgb(0.125, min(1, s * 0.85), min(1, v * 1.04))
            else:
                continue
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    im.save(out)


def main():
    src_dir, dst_dir = os.path.join(A, "mons", "gyarados"), os.path.join(A, "mons", "gyarados_shiny")
    os.makedirs(dst_dir, exist_ok=True)
    for f in ("model.geo.json", "anim.json"):
        shutil.copyfile(os.path.join(src_dir, f), os.path.join(dst_dir, f))
    info = json.load(open(os.path.join(src_dir, "info.json")))
    info["texture"] = "retromon:textures/mons/gyarados_shiny.png"
    json.dump(info, open(os.path.join(dst_dir, "info.json"), "w"), indent=1)
    recolour(os.path.join(A, "textures", "mons", "gyarados.png"), os.path.join(A, "textures", "mons", "gyarados_shiny.png"))
    print("gyarados_shiny")


if __name__ == "__main__":
    main()
