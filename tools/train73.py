#!/usr/bin/env python3
"""
(0.73, John's 28 Nov 09:22) The MAGNET TRAIN that runs between GOLDENROD's and SAFFRON's stations on John's line
(game/MagnetTrain.java puts it on the track and moves it): one long two-ended car of our own design, a nose at each
end so it runs either way. White, its windows in a dark band, a deep blue stripe with a gold line under it along its
sides, a pale grey roof, a dark skirt over its magnets; its noses rounded down in steps, the windscreens dark glass, two
lamps low on each end. 16.5 blocks long, 2.75 wide (on John's five-block line, its rails two apart), under 3 high.

Written as a creature's model (its Bedrock geometry, box UV) so the world draws it as it draws them:
    assets/retromon/pokemon/magnet_train/{model.geo.json, info.json, anim.json}
    assets/retromon/textures/pokemon/magnet_train.png

    python3 -I tools/train73.py
"""
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")

WHITE = (242, 243, 240, 255)
SHADE = (222, 224, 220, 255)
GLASS = (38, 50, 74, 255)
PANE = (62, 82, 119, 255)
SHINE = (120, 146, 190, 255)
BLUE = (43, 76, 155, 255)
GOLD = (224, 184, 74, 255)
ROOF = (217, 220, 223, 255)
ROOF_LINE = (196, 200, 204, 255)
SKIRT = (58, 63, 71, 255)
SKIRT_TOP = (86, 92, 102, 255)
DOOR = (214, 217, 220, 255)
DOOR_EDGE = (150, 156, 162, 255)
LAMP = (255, 246, 200, 255)


def livery(y):
    """A side's colour at a height (model units from the rail): the window band, the stripe, the white."""
    if 26 <= y < 36: return GLASS
    if 15 <= y < 20: return BLUE
    if 14 <= y < 15: return GOLD
    if y < 11: return SHADE
    return WHITE


class Tex:
    def __init__(self, w, h):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.im.load()

    def put(self, x, y, c):
        if 0 <= x < self.im.size[0] and 0 <= y < self.im.size[1]: self.px[x, y] = c


def faces(T, u, v, w, h, d, side, end, top, bottom):
    """Paints a box-UV cube's six faces: side(i, j) for the x sides (i along z, j down from its top), end(i, j) for
    the two ends (i across, j down), top(i, k) and bottom(i, k)."""
    for k in range(d):
        for i in range(w):
            T.put(u + d + i, v + k, top(i, k))
            T.put(u + d + w + i, v + k, bottom(i, k))
    for j in range(h):
        for i in range(d):
            T.put(u + i, v + d + j, side(i, j))
            T.put(u + d + w + i, v + d + j, side(d - 1 - i, j))
        for i in range(w):
            T.put(u + d + i, v + d + j, end(i, j))
            T.put(u + 2 * d + w + i, v + d + j, end(w - 1 - i, j))


def main():
    T = Tex(512, 256)
    cubes = []

    def cube(origin, size, uv):
        cubes.append({"origin": list(origin), "size": list(size), "uv": list(uv)})

    # The skirt over the magnets: four lengths of 56 (sharing one picture).
    def skirt_side(i, j): return SKIRT_TOP if j == 0 else SKIRT
    faces(T, 0, 0, 40, 8, 56, skirt_side, lambda i, j: SKIRT, lambda i, k: SKIRT, lambda i, k: SKIRT)
    for z in (-112, -56, 0, 56):
        cube((-20, 2, z), (40, 8, 56), (0, 0))

    # The body: six lengths of 32, windows (W) or a door (D) in the middle of each: W D W W D W.
    def body_side(y0, door):
        def f(i, j):
            y = y0 - 1 - j
            if door and 11 <= i < 21:
                # A door: its grey frame, its window in the band, a seam down its middle.
                if i in (11, 20) or y < 11: return DOOR_EDGE
                if 26 <= y < 36: return PANE if i not in (15, 16) else GLASS
                return DOOR
            c = livery(y)
            if c == GLASS:
                # The panes, a white post between each, a glint along each pane's top.
                if i % 8 in (0, 7): return WHITE
                return SHINE if y == 34 else PANE
            return c
        return f
    white = lambda i, k: WHITE
    for (u, v, y0, y1, door) in ((0, 64, 10, 24, False), (160, 64, 10, 24, True), (0, 112, 24, 40, False), (160, 112, 24, 40, True)):
        faces(T, u, v, 44, y1 - y0, 32, body_side(y1, door), lambda i, j: WHITE, white, white)
    for n, z in enumerate((-96, -64, -32, 0, 32, 64)):
        door = n in (1, 4)
        cube((-22, 10, z), (44, 14, 32), (160 if door else 0, 64))
        cube((-22, 24, z), (44, 16, 32), (160 if door else 0, 112))
    # The roof.
    faces(T, 320, 64, 36, 6, 32, lambda i, j: ROOF if j else WHITE, lambda i, j: ROOF, lambda i, k: ROOF_LINE if i in (17, 18) else ROOF, lambda i, k: ROOF)
    for z in (-96, -64, -32, 0, 32, 64):
        cube((-18, 40, z), (36, 6, 32), (320, 64))

    # The noses, one each end (sharing their pictures; the ends painted alike, so either way round is the front):
    # the cab (as tall as the body to just under the roof, its windscreen wrapping round), then two steps down to the tip.
    def nose1_side(i, j):
        y = 40 - 1 - j
        if 28 <= y < 38 and i >= 4: return GLASS if (i + y) % 9 else SHINE
        return livery(y)
    def nose1_end(i, j):
        y = 40 - 1 - j
        if 28 <= y < 38 and 2 <= i < 42: return GLASS if (i - y) % 11 else SHINE
        return livery(y)
    faces(T, 320, 112, 44, 30, 16, nose1_side, nose1_end, lambda i, k: GLASS if k >= 10 and 3 <= i < 41 else WHITE, lambda i, k: SHADE)
    def nose2_end(i, j):
        y = 30 - 1 - j
        if 14 <= y < 18 and (3 <= i < 9 or 27 <= i < 33): return LAMP
        if 22 <= y < 30 and 2 <= i < 34: return GLASS
        return livery(y)
    def nose2_side(i, j):
        y = 30 - 1 - j
        if 22 <= y < 30: return GLASS
        return livery(y)
    faces(T, 0, 160, 36, 20, 12, nose2_side, nose2_end, lambda i, k: GLASS if k < 8 else WHITE, lambda i, k: SHADE)
    faces(T, 100, 160, 24, 10, 8, lambda i, j: livery(20 - 1 - j), lambda i, j: livery(20 - 1 - j), white, lambda i, k: SHADE)
    faces(T, 200, 160, 32, 6, 14, skirt_side, lambda i, j: SKIRT, lambda i, k: SKIRT, lambda i, k: SKIRT)
    for s in (1, -1):
        def at(z0, d): return z0 if s > 0 else -z0 - d
        cube((-22, 10, at(96, 16)), (44, 30, 16), (320, 112))
        cube((-18, 10, at(112, 12)), (36, 20, 12), (0, 160))
        cube((-12, 10, at(124, 8)), (24, 10, 8), (100, 160))
        # The skirt under the cab.
        cube((-16, 4, at(112, 14)), (32, 6, 14), (200, 160))

    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.retromon.magnet_train", "texture_width": 512, "texture_height": 256,
                        "visible_bounds_width": 18, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": [{"name": "train", "pivot": [0, 0, 0]}, {"name": "body", "parent": "train", "pivot": [0, 0, 0], "cubes": cubes}]}]}
    d = os.path.join(ASSETS, "pokemon", "magnet_train")
    os.makedirs(d, exist_ok=True)
    json.dump(geo, open(os.path.join(d, "model.geo.json"), "w"), separators=(",", ":"))
    json.dump({"scale": 1.0, "idle": None, "ground": None, "texture": "retromon:textures/pokemon/magnet_train.png", "layers": [], "height": 2.9},
              open(os.path.join(d, "info.json"), "w"))
    json.dump({"format_version": "1.8.0", "animations": {}}, open(os.path.join(d, "anim.json"), "w"))
    T.im.save(os.path.join(ASSETS, "textures", "pokemon", "magnet_train.png"))
    print(len(cubes), "cubes")


if __name__ == "__main__":
    main()
