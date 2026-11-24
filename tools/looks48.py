"""
0.48.0's looks (John's 17:42, 17:51 and 17:53 rounds), drawn for client/AreaLooks (textures/block/area, models/block/area):

  - Seafoam Islands' ice (its rock as faceted ice, its walls packed snow, a frosted floor, its sand, its icy steps), its
    boulders as clusters of ice shards; Rock Tunnel's rock, floor and craggy boulders (the cracked ones for ROCK SMASH),
  - and the rest of the round (see build()).

    (called from lookarea.build())
"""
import math
import os
import random

from PIL import Image

import lookarea as L

V = lambda n: os.path.join(L.VANILLA, "textures", n + ".png")
hexc, mix, save = L.hexc, L.mix, L.save
MODELS = os.path.join(L.HERE, "..", "src", "main", "resources", "assets", "retromon", "models", "block", "area")


def shade(c, k):
    return tuple(int(max(0, min(255, v + (255 - v) * k if k >= 0 else v * (1 + k)))) for v in c[:3]) + (255,)


def noise(size, seed, cells):
    """Smooth value noise over size by size that wraps (0..1)."""
    r = random.Random(seed)
    g = [[r.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            fx, fy = x / size * cells, y / size * cells
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b = g[y0 % cells][x0 % cells], g[y0 % cells][(x0 + 1) % cells]
            c, d = g[(y0 + 1) % cells][x0 % cells], g[(y0 + 1) % cells][(x0 + 1) % cells]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


def cells(size, n, seed):
    r = random.Random(seed)
    return [(r.random() * size, r.random() * size) for _ in range(n)]


def facets(size, n, seed, base, light, dark, edge=0.18):
    """Crystal facets: a Voronoi of flat-shaded faces, each lit by its own tilt, a bright rim where two meet."""
    pts = cells(size, n, seed)
    r = random.Random(seed + "t")
    tilt = [r.uniform(-1, 1) for _ in pts]
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            ds = []
            for i, (px, py) in enumerate(pts):
                for ox in (-size, 0, size):
                    for oy in (-size, 0, size):
                        ds.append(((x - px - ox) ** 2 + (y - py - oy) ** 2, i))
            ds.sort()
            (d0, i0), (d1, _i1) = ds[0], ds[1]
            k = tilt[i0] * 0.55 - 0.25 * (y / size)
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            if math.sqrt(d1) - math.sqrt(d0) < 1.0:
                c = mix(c, (255, 255, 255), edge)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    return im


# ---------- Seafoam Islands ----------
def seafoam():
    ice, ice_l, ice_d = hexc("#8FC6E8"), hexc("#D6F0FF"), hexc("#4E8DBE")
    save("sf_ice", facets(32, 9, "sfi", ice, ice_l, ice_d))
    save("sf_ice_top", facets(32, 7, "sft", hexc("#A9D6F0"), hexc("#EAF8FF"), hexc("#6FA6CC"), 0.25))
    snow, snow_l, snow_d = hexc("#E7EEF4"), hexc("#FFFFFF"), hexc("#B5C6D6")
    n1, n2 = noise(32, "sn1", 4), noise(32, "sn2", 8)
    im = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            k = (n1[y][x] - 0.5) * 0.8 + (n2[y][x] - 0.5) * 0.35 - 0.18 * (y / 31)
            c = mix(snow, snow_l, k) if k > 0 else mix(snow, snow_d, -k)
            # Bands of wind-packed snow, and a blue vein of ice through it.
            if (y + int(6 * n1[y][(x * 3) % 32])) % 11 == 0: c = mix(c, snow_d, 0.35)
            if abs(y - 20 - 4 * math.sin(x / 5.0)) < 0.8: c = mix(c, ice, 0.5)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("sf_snow", im)
    top = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            k = (n1[y][x] - 0.5) * 0.5 + (n2[y][x] - 0.5) * 0.3
            c = mix(snow, snow_l, k) if k > 0 else mix(snow, snow_d, -k)
            top.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("sf_snow_top", top)
    # The floor: pale frosted stone, glints of ice.
    fl, fl_l, fl_d = hexc("#CCD8E2"), hexc("#EEF5FA"), hexc("#9FB2C2")
    r = random.Random("sff")
    m1, m2 = noise(64, "sff1", 6), noise(64, "sff2", 14)
    im = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            k = (m1[y][x] - 0.5) * 0.7 + (m2[y][x] - 0.5) * 0.35
            c = mix(fl, fl_l, k) if k > 0 else mix(fl, fl_d, -k)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    for _ in range(60):
        x, y = r.randrange(64), r.randrange(64)
        im.putpixel((x, y), (255, 255, 255, 255))
    save("sf_floor", im)
    sd, sd_l, sd_d = hexc("#E6DDB8"), hexc("#F6EFD2"), hexc("#C2B78E")
    im = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            k = (m1[(y + 13) % 64][x] - 0.5) * 0.6 + (m2[y][(x + 7) % 64] - 0.5) * 0.4
            c = mix(sd, sd_l, k) if k > 0 else mix(sd, sd_d, -k)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("sf_sand", im)
    # The steps between its levels (polished diorite) glassy ice; the edges at the water (jungle stairs) icy rock.
    st = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.22 - 0.3 * (y / 15) + (0.12 if 4 <= (x + y) % 16 <= 6 else 0)
            if y == 0: k = 0.45
            st.putpixel((x, y), shade(hexc("#A3D2EE"), k))
    save("sf_step", st)
    save("sf_edge", facets(16, 4, "sfe", hexc("#9ABCD4"), hexc("#D9ECF8"), hexc("#5F86A6")))
    # The boulders' ice: a shard's face, pale at its edges and bright at its tip.
    sh = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 0.35 - 0.5 * (y / 15) + (0.25 if x in (0, 1) else -0.15 if x in (14, 15) else 0) + (0.12 if (x + 2 * y) % 9 == 0 else 0)
            sh.putpixel((x, y), shade(hexc("#86C7EE"), k))
    save("ice_shard", sh)


# ---------- Rock Tunnel ----------
def rock_tunnel():
    # FireRed's Rock Tunnel: warm brown-grey rock in rough slabs; a brown earth floor.
    rk, rk_l, rk_d = hexc("#8C7F70"), hexc("#AA9C8B"), hexc("#625649")
    save("rt_rock", facets(32, 10, "rtr", rk, rk_l, rk_d, 0.06))
    save("rt_rock_top", facets(32, 8, "rtt", hexc("#9A8D7E"), hexc("#B6A898"), hexc("#73675A"), 0.05))
    fl, fl_l, fl_d = hexc("#8D6A48"), hexc("#A8825C"), hexc("#694D33")
    n1, n2 = noise(64, "rtf1", 6), noise(64, "rtf2", 16)
    r = random.Random("rtf")
    im = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            k = (n1[y][x] - 0.5) * 0.6 + (n2[y][x] - 0.5) * 0.4
            c = mix(fl, fl_l, k) if k > 0 else mix(fl, fl_d, -k)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    for _ in range(90):
        x, y = r.randrange(64), r.randrange(64)
        im.putpixel((x, y), shade(fl_d, -0.2))
    save("rt_floor", im)
    save("rt_boulder", facets(16, 5, "rtb", hexc("#8E8273"), hexc("#B0A392"), hexc("#5F5446"), 0.08))
    cr = facets(16, 5, "rts", hexc("#7A7166"), hexc("#9B9183"), hexc("#4E463D"), 0.04)
    for x, y in ((3, 2), (4, 3), (5, 4), (5, 5), (6, 6), (7, 6), (8, 7), (8, 8), (9, 9), (11, 9), (12, 10), (6, 9), (5, 10), (4, 12)):
        cr.putpixel((x, y), (40, 34, 28, 255))
    save("rt_smash", cr)


def boulder_models():
    """The new boulders: Seafoam's clusters of ice shards (iboulder), Rock Tunnel's craggy rocks (rboulder) and its
    cracked ones you'd break with ROCK SMASH (sboulder)."""
    def shard(x, z, w, h, axis=None, angle=0, origin=None):
        e = {"from": [x - w / 2, 0, z - w / 2], "to": [x + w / 2, h, z + w / 2], "faces": {f: {"texture": "#rock", "uv": [0, 16 - min(16, h), 16, 16]} for f in ("north", "south", "east", "west")}}
        e["faces"]["up"] = {"texture": "#rock", "uv": [6, 0, 10, 4]}
        if axis:
            e["rotation"] = {"origin": origin or [x, 0, z], "axis": axis, "angle": angle}
        return e
    ice = {
        "iboulder_a": [shard(8, 8, 4.2, 15), shard(4.5, 6, 3.2, 10, "z", 22.5), shard(11.5, 9.5, 3.4, 11, "z", -22.5), shard(7, 12, 3, 8, "x", -22.5),
                       shard(9.5, 4, 2.8, 9, "x", 22.5)],
        "iboulder_b": [shard(7, 9, 4.6, 13, "x", -22.5), shard(11, 6, 3.4, 10, "z", -22.5), shard(4, 6, 2.8, 7, "z", 22.5), shard(10.5, 12, 3, 8)],
        "iboulder_small": [shard(8, 8, 3.6, 8), shard(5, 9, 2.6, 6, "z", 22.5), shard(11, 7, 2.6, 5, "z", -22.5)],
    }

    def crag(boxes):
        return [{"from": b[:3], "to": b[3:], "faces": {f: {"texture": "#rock"} for f in ("north", "south", "east", "west", "up", "down")}} for b in boxes]
    rocks = {
        "rboulder_a": crag([[1, 0, 2, 15, 7, 14], [2, 7, 3, 12, 11, 12], [4, 11, 4, 10, 13, 9], [10, 0, 0.5, 15.5, 5, 6]]),
        "rboulder_b": crag([[2, 0, 1, 14, 6, 15], [3, 6, 4, 13, 10, 13], [5, 10, 6, 9, 12, 11], [0.5, 0, 9, 5, 4, 15.5]]),
        "rboulder_small": crag([[2, 0, 2, 14, 4, 14], [4, 4, 4, 11, 6, 11]]),
    }
    for name, els in list(ice.items()):
        L.write_json(os.path.join(MODELS, name + ".json"), {"parent": "block/block", "ambientocclusion": False,
                                                            "textures": {"rock": "retromon:block/area/ice_shard", "particle": "retromon:block/area/ice_shard"}, "elements": els})
    for name, els in rocks.items():
        L.write_json(os.path.join(MODELS, name + ".json"), {"parent": "block/block", "ambientocclusion": True,
                                                            "textures": {"rock": "retromon:block/area/rt_boulder", "particle": "retromon:block/area/rt_boulder"}, "elements": els})
        sname = name.replace("rboulder", "sboulder")
        L.write_json(os.path.join(MODELS, sname + ".json"), {"parent": "block/block", "ambientocclusion": True,
                                                             "textures": {"rock": "retromon:block/area/rt_smash", "particle": "retromon:block/area/rt_smash"}, "elements": els})


def build():
    seafoam()
    rock_tunnel()
    boulder_models()
    for f in EXTRA:
        f()


import looks48b
import looks49
import looks50
import looks51
import looks52
import looks53
import looks54
import looks56
import looks57
import looks59
import looks594
import looks60
import looks61
EXTRA = [looks48b.build, looks49.build, looks50.build, looks51.build, looks52.build, looks53.build, looks54.build, looks56.build, looks57.build, looks59.build, looks594.build,
         looks60.build, looks61.build]
