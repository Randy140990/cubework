"""
0.72.1's whirlpool (John's 28 Nov 07:41: "they keep like flashing on and off. I think they just need to remain, but
actually rotate"): 0.70's funnel picture (tools/looks70b.py's whirlpool, its first frame), turned round a whole turn
over 32 frames, so it spins and nothing else about it changes. Every pixel is either there or not (its soft rim
dithered by how far out it is, the same in every frame), so the game can draw it with the solid blocks, not sorted
in among the sea's see-through faces, which is what made it flash.

Writes textures/block/area/whirlpool.png and its .mcmeta.
"""
import json
import math
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402
import looks70b  # noqa: E402

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def spinning(size=80, frames=32):
    base = looks70b.whirlpool(size, 1).convert("RGBA")
    out = Image.new("RGBA", (size, size * frames))
    for f in range(frames):
        im = base.rotate(360.0 * f / frames, resample=Image.BICUBIC)
        px = im.load()
        for y in range(size):
            for x in range(size):
                r, g, b, a = px[x, y]
                rr = math.hypot((x + 0.5 - size / 2) / (size / 2), (y + 0.5 - size / 2) / (size / 2))
                # How solid it is from how far out it is only (so the rim's dots stay put as it turns).
                want = 0.0 if rr > 1 else min(1.0, (1 - rr) * 1.8) ** 1.5 * 0.95 + (0.05 if rr < 0.9 else 0)
                on = rr < 0.07 or want * 16 > BAYER[y % 4][x % 4] + 0.5
                px[x, y] = (r, g, b, 255 if on and a > 0 else 0) if on else (0, 0, 0, 0)
        out.paste(im, (0, f * size))
    return out


def main():
    L.save("whirlpool", spinning())
    with open(os.path.join(L.TEX, "whirlpool.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": False}}, fh)


if __name__ == "__main__":
    main()
