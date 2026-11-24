"""(0.61) John's 18:10 and 18:19: the caves' second textures (sand) drawn as connected surfaces, as the ice is (one
picture over four blocks, soft drifts and specks, no edge round each block); MT. SILVER's looks (Crystal's SILVER
CAVE: blue-grey rock in ledges, its floor grey, the snow at its top); its boulders."""
import math, os, random, sys
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import lookarea as L  # noqa: E402
from looks48 import facets, noise, shade  # noqa: E402
from looks60 import speckled, cliff  # noqa: E402

hexc, mix, save = L.hexc, L.mix, L.save
MODELS = L.MODELS


def dunes(name, size, seed, base, light, dark, specks, speck_c):
    """Sand as one surface over several blocks: soft ripples drifting across it (lit on one side, shaded on the other),
    soft blotches, grains of a lighter and a darker colour; it tiles, so neighbouring blocks run on into each other."""
    n1, n2 = noise(size, seed + "1", 4), noise(size, seed + "2", 10)
    r = random.Random(seed)
    im = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            # Ripples: a wave across at a slant, bent by the blotches (whole waves over the picture, so it tiles).
            ph = (x + 2 * y) / size * 2 * math.pi * 3 + n1[y][x] * 2.2
            k = 0.22 * math.sin(ph) + (n1[y][x] - 0.5) * 0.35 + (n2[y][x] - 0.5) * 0.2
            c = mix(base, light, k) if k > 0 else mix(base, dark, -k)
            im.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    for _ in range(specks):
        x, y = r.randrange(size), r.randrange(size)
        im.putpixel((x, y), speck_c[r.randrange(len(speck_c))])
    save(name, im)


def sands():
    # Seafoam's sand (end stone): warm pale sand, rippled.
    dunes("sf_sand", 64, "sfs61", hexc("#E6D6A4"), hexc("#F4E9C4"), hexc("#CBB882"), 260, [hexc("#FFF6DA"), hexc("#B9A673"), hexc("#D9C792")])
    # Victory Road's sand: warmer yellow.
    dunes("vr_sand", 64, "vrs61", hexc("#D6BD6A"), hexc("#EAD58A"), hexc("#B59C4E"), 240, [hexc("#F2E3A6"), hexc("#A88E42"), hexc("#C8AE5E")])


def silver():
    # The floor (deepslate): Crystal's cave floor, grey with a blue cast, speckled; its patches (cobbled deepslate) a
    # little darker; the raised ground (tuff) a little paler.
    speckled("ms_floor", 64, "msf", hexc("#868B98"), hexc("#A0A5B1"), hexc("#6C717E"), 230, [hexc("#5C616D"), hexc("#B4B9C4"), hexc("#7A7F8B")])
    speckled("ms_patch", 64, "msp", hexc("#757A88"), hexc("#8D92A0"), hexc("#5E6370"), 200, [hexc("#4E5360"), hexc("#A0A5B1")])
    speckled("ms_ground", 64, "msg", hexc("#979CA8"), hexc("#B0B5C0"), hexc("#7D828E"), 220, [hexc("#6C717D"), hexc("#C4C8D1")])
    # The walls: blue-grey rock in ledges; their tops (calcite) pale frosted rock.
    cliff("ms_cliff", 32, "msc", hexc("#5E6676"), hexc("#7A8394"), hexc("#3E4554"))
    speckled("ms_top", 64, "mst", hexc("#C9CED7"), hexc("#E0E4EA"), hexc("#A9AEB8"), 160, [hexc("#F2F4F7"), hexc("#9AA0AB")], big=5, small=12)
    # The ledges (andesite) and steps (polished tuff): pale stone, a lit edge.
    st = Image.new("RGBA", (16, 16))
    n = noise(16, "msst", 4)
    for y in range(16):
        for x in range(16):
            k = (n[y][x] - 0.5) * 0.3 + (0.25 if y < 2 else -0.25 if y > 13 else 0)
            c = mix(hexc("#A7ADB8"), hexc("#C6CBD4"), k) if k > 0 else mix(hexc("#A7ADB8"), hexc("#808692"), -k)
            st.putpixel((x, y), tuple(int(v) for v in c[:3]) + (255,))
    save("ms_step", st)
    # The rock at the water's edge (dripstone, jungle stairs): darker wet stone.
    save("ms_edge", facets(16, 5, "mse", hexc("#5A6070"), hexc("#757C8C"), hexc("#3C414E"), 0.05))
    # Room 3's rock (stone) as the walls, frosted; the snow as Seafoam's.
    cliff("ms_cliff_ice", 32, "msci", hexc("#7C8696"), hexc("#9AA4B4"), hexc("#566070"))
    # The boulders: grey-blue rock.
    save("ms_boulder", facets(16, 5, "msb", hexc("#7E8696"), hexc("#A2AAB8"), hexc("#565D6B"), 0.08))


def boulder_models():
    def boxes(bs):
        return [{"from": b[:3], "to": b[3:], "faces": {f: {"texture": "#rock"} for f in ("north", "south", "east", "west", "up", "down")}} for b in bs]
    shapes = {
        "_a": boxes([[1.5, 0, 2, 14.5, 6, 14], [2.5, 6, 3, 13.5, 10, 13], [4, 10, 4.5, 12, 12.5, 11.5], [0.5, 0, 4, 2, 4, 12], [14, 0, 4, 15.5, 4, 12]]),
        "_b": boxes([[2, 0, 1.5, 14, 5, 14.5], [3, 5, 2.5, 13, 9, 13.5], [5, 9, 4, 11, 11, 11], [4, 0, 0.5, 12, 3.5, 2], [4, 0, 14, 12, 3.5, 15.5]]),
        "_small": boxes([[3, 0, 3, 13, 4, 13], [4.5, 4, 4.5, 11.5, 6.5, 11.5]]),
    }
    for suf, els in shapes.items():
        L.write_json(os.path.join(MODELS, "xboulder" + suf + ".json"), {"parent": "block/block", "ambientocclusion": True,
                                                                        "textures": {"rock": "retromon:block/area/ms_boulder", "particle": "retromon:block/area/ms_boulder"},
                                                                        "elements": els})


def build():
    sands()
    silver()
    boulder_models()


if __name__ == "__main__":
    build()
