"""
(0.59.7, John 15:33: "I don't see bikes there as models ... I just see what looks like black lines.")

The bikes outside the Bike Shop, made again so they read as bikes from the road:
- They stood across the paving (along x), so from the road beside them (looking east) only their ends showed: a
  pixel-thin tyre and frame, the "black lines". Now each one runs along the kerb (along z), side on to the road.
- Chunkier: tyres 2 pixels wide with a pale steel rim, a hub and spokes inside; a frame of 1.3-pixel tubes (top
  tube, down tube, seat tube, stays, fork), a black saddle, wide handlebars with black grips, a chain ring and pedals.
- Two models: prop_bike (stood on a full block) and prop_bike_low (on a bottom slab, half a block lower).

Writes src/main/resources/assets/retromon/models/block/area/prop_bike.json and prop_bike_low.json (the textures
bike_red, bike_black and bike_steel are tools/looks50.py's, kept as they are).
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon", "models", "block", "area")
TEX = {"tyre": "retromon:block/area/bike_black", "frame": "retromon:block/area/bike_red", "steel": "retromon:block/area/bike_steel",
       "particle": "retromon:block/area/bike_red"}


def r(v):
    return round(v, 3)


def bike(g):
    """The bike's elements, its wheels on the ground at height g (16: on a full block; 8: on a bottom slab)."""
    els = []

    def box(x0, y0, z0, x1, y1, z1, tex):
        els.append({"from": [r(x0), r(y0), r(z0)], "to": [r(x1), r(y1), r(z1)],
                    "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}})

    def ring(zc, yc, r0, r1, x0, x1, tex, c=0.5):
        """A ring in the side (z, y) plane, in half-pixel cells, each row's runs one element."""
        n = int(math.ceil(r1 / c)) + 1
        for j in range(-n, n):
            yy = yc + (j + 0.5) * c
            run = None
            for i in range(-n, n + 1):
                zz = zc + (i + 0.5) * c
                d = math.hypot(zz - zc, yy - yc)
                inside = i < n and r0 <= d <= r1
                if inside and run is None: run = i
                if not inside and run is not None:
                    box(x0, yc + j * c, zc + run * c, x1, yc + (j + 1) * c, zc + i * c, tex)
                    run = None

    def tube(a, b, w, tex, steps):
        """A tube from a to b ((z, y) points) as a staircase of square-ended pieces, w across."""
        for k in range(steps):
            t = (k + 0.5) / steps
            z = a[0] + (b[0] - a[0]) * t
            y = a[1] + (b[1] - a[1]) * t
            hz = abs(b[0] - a[0]) / steps / 2 + w / 2
            hy = abs(b[1] - a[1]) / steps / 2 + w / 2
            box(7.35, y - hy, z - hz, 8.65, y + hy, z + hz, tex)

    R_OUT, R_TYRE, R_RIM = 3.4, 2.5, 1.9
    rear, front = (3.5, g + R_OUT), (12.5, g + R_OUT)
    for zc, yc in (rear, front):
        ring(zc, yc, R_TYRE, R_OUT, 7.0, 9.0, "tyre")           # the tyre, two pixels wide
        ring(zc, yc, R_RIM, R_TYRE - 0.01, 7.3, 8.7, "steel")    # the rim
        box(7.7, yc - 0.15, zc - R_RIM, 8.3, yc + 0.15, zc + R_RIM, "steel")   # spokes, across
        box(7.7, yc - R_RIM, zc - 0.15, 8.3, yc + R_RIM, zc + 0.15, "steel")   # and up and down
        box(7.2, yc - 0.5, zc - 0.5, 8.8, yc + 0.5, zc + 0.5, "steel")         # the hub

    bb = (7.9, g + 3.1)            # the bottom bracket
    seat = (7.0, g + 8.6)          # the top of the seat tube
    tube(bb, seat, 1.1, "frame", 6)                        # the seat tube
    box(7.4, g + 7.8, 6.9, 8.6, g + 8.8, 11.7, "frame")    # the top tube
    tube(bb, (11.5, g + 7.2), 1.1, "frame", 8)             # the down tube
    tube(rear, bb, 0.8, "frame", 6)                        # the chain stays
    tube(rear, (7.1, g + 8.2), 0.8, "frame", 7)            # the seat stays
    box(7.35, g + 6.6, 11.2, 8.65, g + 9.4, 12.3, "frame") # the head tube
    # The fork: a blade each side of the front tyre, joined at the crown.
    for x0, x1 in ((6.5, 7.0), (9.0, 9.5)):
        tube((12.0, g + 6.8), front, 0.6, "frame", 4)
        els[-4:] = [dict(e, **{"from": [x0] + e["from"][1:], "to": [x1] + e["to"][1:]}) for e in els[-4:]]
    box(6.5, g + 6.6, 11.4, 9.5, g + 7.3, 12.4, "frame")   # the crown
    box(7.65, g + 9.4, 11.3, 8.35, g + 10.4, 12.0, "steel")  # the stem
    box(4.6, g + 10.3, 11.0, 11.4, g + 10.9, 11.7, "steel")  # the handlebars
    for x0, x1 in ((4.3, 5.9), (10.1, 11.7)):                # the grips
        box(x0, g + 10.15, 10.85, x1, g + 11.05, 11.85, "tyre")
    box(7.65, g + 8.6, 6.65, 8.35, g + 9.2, 7.35, "steel")   # the seat post
    box(7.0, g + 9.2, 5.3, 9.0, g + 10.0, 8.5, "tyre")       # the saddle
    box(8.7, g + 2.1, 6.8, 9.1, g + 4.1, 8.9, "steel")       # the chain ring
    box(9.1, g + 1.3, 8.4, 10.4, g + 1.9, 9.6, "tyre")       # the pedals, one each side
    box(5.6, g + 4.3, 6.2, 6.9, g + 4.9, 7.4, "tyre")
    return els


def build():
    for name, g in (("prop_bike", 16.0), ("prop_bike_low", 8.0)):
        els = bike(g)
        json.dump({"ambientocclusion": False, "textures": TEX, "elements": els}, open(os.path.join(MODELS, name + ".json"), "w"), indent=1)
        print(name, len(els), "elements")


if __name__ == "__main__":
    build()
