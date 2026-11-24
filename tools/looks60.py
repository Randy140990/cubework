"""
0.60's looks (John's 16:36: Cerulean Cave and Victory Road, his schematics), drawn for client/AreaLooks
(textures/block/area, models/block/area), as the other caves' (tools/looks48.py):

  - Cerulean Cave as FireRed's: a pale grey speckled stone floor, its cliffs green and mossy (John's mossy cobblestone
    and moss), pale stone steps, the red-brown rock along the water's edge, its rocks grey boulders (cboulder);
  - Victory Road as FireRed's: an olive-grey earth floor, yellow sand, red-brown rock walls with pale tops, stone
    steps, the round switches in the floor, its rocks warm granite boulders (vboulder).

    python3 -I tools/looks60.py     (also from tools/looks48.py's build())
"""
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lookarea as L  # noqa: E402
from looks48 import facets, noise, shade  # noqa: E402

hexc, mix, save = L.hexc, L.mix, L.save
MODELS = L.MODELS


def speckled(name, size, seed, base, light, dark, specks, speck_c, big=6, small=16, weight=(0.6, 0.4)):
    """A floor: smooth noise in its colours, and specks over it."""
    n1, n2 = noise(size, seed + "1", big), noise(size, seed + "2", small)
    r = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            k = (n1[y][x] - 0.5) * weight[0] + (n2[y][x] - 0.5) * weight[1]
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    for _ in range(specks):
        x, y = r.randrange(size), r.randrange(size)
        c = speck_c[r.randrange(len(speck_c))]
        im.putpixel((x, y), c)
        if r.random() < 0.3: im.putpixel(((x + 1) % size, y), c)
    save(name, im)


def cliff(name, size, seed, base, light, dark, moss=None, ledges=5):
    """A cliff's face: rough rock in rows of ledges, lit from above, and (moss) green creeping down from the top."""
    n1, n2 = noise(size, seed + "1", 4), noise(size, seed + "2", 12)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            k = (n1[y][x] - 0.5) * 0.55 + (n2[y][x] - 0.5) * 0.45
            # Each ledge lit along its top and in shadow under its lip.
            band = (y + int(3 * n1[(y * 2) % size][x])) % (size // ledges)
            if band == 0: k += 0.35
            elif band == size // ledges - 1: k -= 0.4
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            if moss is not None:
                m = n2[y][(x * 5) % size] * 1.2 - y / size * 1.6
                if m > 0.15: c = mix(c, moss, min(1.0, (m - 0.15) * 3))
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save(name, im)


def cerulean():
    # The floor: FireRed's pale grey stone, speckled darker and lighter.
    speckled("cc_floor", 64, "ccf", hexc("#A9AEB8"), hexc("#C3C8D0"), hexc("#8E939E"), 220,
             [hexc("#7C818C"), hexc("#D8DCE2"), hexc("#969BA6")])
    # The cliffs: green-grey rock with moss down from the top; their tops thick moss, as FireRed's green ledges.
    cliff("cc_cliff", 32, "ccc", hexc("#5F7464"), hexc("#7E9583"), hexc("#3F5045"), moss=hexc("#4F8A45"))
    speckled("cc_moss_top", 32, "ccm", hexc("#5E9A4E"), hexc("#7DB866"), hexc("#3F7236"), 50,
             [hexc("#8CC872"), hexc("#356030")], big=4, small=10)
    # The steps: pale stone slabs with a lit edge.
    st = Image.new("RGBA", (16, 16))
    n = noise(16, "ccs", 4)
    for y in range(16):
        for x in range(16):
            k = (n[y][x] - 0.5) * 0.3 + (0.25 if y < 2 else -0.25 if y > 13 else 0)
            c = mix(hexc("#C4C8CE"), hexc("#E2E5E9"), k) if k > 0 else mix(hexc("#C4C8CE"), hexc("#9DA2AA"), -k)
            if x in (0, 15): c = shade(c, -0.12)
            st.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("cc_step", st)
    # The water's edge: FireRed's red-brown rock rim.
    save("cc_edge", facets(16, 5, "cce", hexc("#9A6A52"), hexc("#B98A6E"), hexc("#6E4A38"), 0.05))
    # The boulders: rounded grey rock.
    save("cc_boulder", facets(16, 5, "ccb", hexc("#9097A1"), hexc("#B4BAC3"), hexc("#626973"), 0.08))


def victory():
    # The floor: FireRed's olive-grey earth, a little mottled.
    speckled("vr_floor", 64, "vrf", hexc("#8D8A70"), hexc("#A5A287"), hexc("#6F6C55"), 180,
             [hexc("#5E5B47"), hexc("#B3B095"), hexc("#7A775F")])
    # The sand: warm yellow.
    speckled("vr_sand", 32, "vrs", hexc("#D6BD6A"), hexc("#EAD58A"), hexc("#B59C4E"), 60,
             [hexc("#F2E3A6"), hexc("#A88E42")], big=4, small=12, weight=(0.4, 0.3))
    # The walls: red-brown rock in ledges; their tops the mountain's pale rock.
    cliff("vr_cliff", 32, "vrc", hexc("#9A5C46"), hexc("#B87A60"), hexc("#6A3C2C"))
    save("vr_top", facets(32, 9, "vrt", hexc("#D3BBA4"), hexc("#E8D4C0"), hexc("#AE9480"), 0.05))
    # The steps cut in the rock.
    st = Image.new("RGBA", (16, 16))
    n = noise(16, "vrst", 4)
    for y in range(16):
        for x in range(16):
            k = (n[y][x] - 0.5) * 0.3 + (0.3 if y < 2 else -0.3 if y > 13 else 0)
            c = mix(hexc("#B49A80"), hexc("#CDB59C"), k) if k > 0 else mix(hexc("#B49A80"), hexc("#8C735C"), -k)
            st.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("vr_step", st)
    # The switch in the floor (FireRed's, that a boulder presses): a round grey plate, a ring and a cross. Laid over
    # its 2 by 2 blocks (@2).
    S = 32
    sw = Image.new("RGBA", (S, S))
    fl = noise(S, "vrsw", 6)
    for y in range(S):
        for x in range(S):
            k = (fl[y][x] - 0.5) * 0.4
            c = mix(hexc("#8D8A70"), hexc("#A5A287"), k) if k > 0 else mix(hexc("#8D8A70"), hexc("#6F6C55"), -k)
            d = math.hypot(x + 0.5 - S / 2, y + 0.5 - S / 2)
            if d < 13:
                c = mix(hexc("#9EA3A8"), hexc("#C8CCD0"), max(0.0, 0.5 - (x + y) / (2 * S)))
                if 10 <= d < 11.4: c = hexc("#5D6268")
                if (abs(x + 0.5 - S / 2) < 1.2 or abs(y + 0.5 - S / 2) < 1.2) and d < 8: c = hexc("#6C7177")
            elif d < 14: c = hexc("#4E4B3C")
            sw.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("vr_switch", sw)
    # The boulders: warm granite.
    save("vr_boulder", facets(16, 5, "vrb", hexc("#9C7E6C"), hexc("#BC9E8A"), hexc("#6A5244"), 0.07))


def boulder_models():
    """Cerulean Cave's grey boulders (cboulder) and Victory Road's granite ones (vboulder): rounded mounds of a few
    boxes, as Rock Tunnel's craggy ones (rboulder) but rounder."""
    def boxes(bs):
        return [{"from": b[:3], "to": b[3:], "faces": {f: {"texture": "#rock"} for f in ("north", "south", "east", "west", "up", "down")}} for b in bs]
    shapes = {
        "_a": boxes([[1.5, 0, 2, 14.5, 6, 14], [2.5, 6, 3, 13.5, 10, 13], [4, 10, 4.5, 12, 12.5, 11.5], [0.5, 0, 4, 2, 4, 12], [14, 0, 4, 15.5, 4, 12]]),
        "_b": boxes([[2, 0, 1.5, 14, 5, 14.5], [3, 5, 2.5, 13, 9, 13.5], [5, 9, 4, 11, 11, 11], [4, 0, 0.5, 12, 3.5, 2], [4, 0, 14, 12, 3.5, 15.5]]),
        "_small": boxes([[3, 0, 3, 13, 4, 13], [4.5, 4, 4.5, 11.5, 6.5, 11.5]]),
    }
    for pre, tex in (("cboulder", "cc_boulder"), ("vboulder", "vr_boulder")):
        for suf, els in shapes.items():
            L.write_json(os.path.join(MODELS, pre + suf + ".json"), {"parent": "block/block", "ambientocclusion": True,
                                                                     "textures": {"rock": "retromon:block/area/" + tex, "particle": "retromon:block/area/" + tex},
                                                                     "elements": els})


def build():
    cerulean()
    victory()
    boulder_models()


if __name__ == "__main__":
    build()
