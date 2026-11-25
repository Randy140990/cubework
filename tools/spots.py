#!/usr/bin/env python3
"""
(0.64, John's 07:10) The Interior Viewer's previews of battles fought on the spot: real places out of John's Kanto world
and his caves' and forests' block rooms, each laid out as the game lays a battle there (tools/battlespot.py, the
game's client/BattleSpot.java), with what's cleared taken away, seen from the battle's camera.

    python3 -I tools/spots.py <John's world folder>

Writes tools/interior/spots_data.js: for each preview, its blocks round the battle (only the ones you could see), the
blocks' looks (the mod's own textures where a look of the mod's covers them, the Retromon Look pack's where it has
the block, otherwise a plain picture in the block's colours), who stands where, and the camera.
"""
import base64, collections, io, json, math, os, re, sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import battlespot as BS  # noqa: E402
import spotworld  # noqa: E402

ROOT = os.path.join(HERE, "..")
RES = os.path.join(ROOT, "src", "main", "resources")
DATA = os.path.join(RES, "retromon", "data")
INTER = os.path.join(RES, "retromon", "interiors")
LOOK = os.path.join(RES, "resourcepacks", "retromon_look", "assets", "minecraft", "textures", "block")
AREA = os.path.join(RES, "assets", "retromon", "textures", "block", "area")
OUT = os.path.join(HERE, "interior", "spots_data.js")

# The places on John's map (game/Game.DEFAULT_ZONES): x1 z1 x2 z2.
ZONES = {"route_1": (-845, 590, -745, 654), "route_22": (-900, 495, -831, 565), "route_3": (-740, 285, -580, 332), "cerulean_city": (-401, 220, -311, 305),
         "route_24": (-372, 140, -332, 219), "route_25": (-331, 125, -160, 175), "route_10": (-165, 296, -110, 345), "route_16": (-680, 388, -560, 444),
         "route_17": (-665, 445, -615, 757), "route_18": (-665, 758, -561, 805), "route_12": (-142, 448, -100, 720)}
FACING = {"down": (0, 1), "up": (0, -1), "left": (-1, 0), "right": (1, 0)}


def table(name):
    out = []
    for line in open(os.path.join(DATA, name), encoding="utf-8"):
        if line.startswith("#") or not line.strip(): continue
        out.append(line.rstrip("\n").split("\t"))
    return out


SIZES = {r[0]: (int(r[1]), int(r[2])) for r in table("sizes.tsv")}
TRAINERS = {r[0]: r for r in table("trainers.tsv")}

# ---------- the previews ----------
# (id, name, where: ("map", place, trainer key | (x, z)), or ("room", json, room, trainer key | (x, z, facing)), note)
PREVIEWS = [
    ("spot_route_1", "Route 1 (wild, in the grass)", ("map", "route_1", "grass"), "A wild POKéMON in Route 1's grass."),
    ("spot_route_22", "Route 22 (your rival)", ("map", "route_22", (-866, 535)), "Your rival on Route 22."),
    ("spot_route_3", "Route 3", ("map", "route_3", "route_3_0"), "BUG CATCHER COLTON on Route 3."),
    ("spot_route_24", "Route 24 (Nugget Bridge)", ("map", "route_24", "route_24_2"), "A trainer on NUGGET BRIDGE."),
    ("spot_route_25", "Route 25", ("map", "route_25", "route_25_3"), "A trainer on Route 25."),
    ("spot_cerulean_city", "Cerulean City", ("map", "cerulean_city", "cerulean_city_0"), "The ROCKET in CERULEAN CITY."),
    ("spot_route_10", "Route 10", ("map", "route_10", "route_10_0"), "POKéMANIAC MARK on Route 10."),
    ("spot_route_16", "Route 16 (Cycling Road)", ("map", "route_16", "route_16_2"), "A CUE BALL on the CYCLING ROAD (Route 16)."),
    ("spot_route_17", "Route 17 (Cycling Road)", ("map", "route_17", "route_17_0"), "A CUE BALL on the CYCLING ROAD by the sea (Route 17)."),
    ("spot_route_18", "Route 18", ("map", "route_18", "route_18_0"), "A BIRD KEEPER on Route 18."),
    ("spot_mt_moon", "Mt. Moon", ("room", "mt_moon", "mt_moon_1f", "first"), "A trainer in MT. MOON."),
    ("spot_rock_tunnel", "Rock Tunnel", ("room", "rock_tunnel", "rock_tunnel_1f", "first"), "A trainer in ROCK TUNNEL."),
    ("spot_digletts_cave", "Diglett's Cave (wild)", ("room", "digletts_cave", "digletts_b1f", (98, 88)), "A wild DIGLETT, in the tunnel."),
    ("spot_seafoam", "Seafoam Islands (wild)", ("room", "seafoam_islands", "seafoam_islands_1f", "spawn"), "A wild POKéMON in the SEAFOAM ISLANDS."),
    ("spot_victory_road", "Victory Road", ("room", "victory_road", "victory_road_2f", "first"), "A trainer on VICTORY ROAD."),
    ("spot_cerulean_cave", "Cerulean Cave (wild)", ("room", "cerulean_cave", "cerulean_cave_1f", (86, 38)), "A wild POKéMON in CERULEAN CAVE."),
    ("spot_mt_silver", "Mt. Silver (Red)", ("room", "mt_silver", "silver_cave_room_3", "first"), "RED, at the top of MT. SILVER."),
    ("spot_viridian_forest", "Viridian Forest", ("room", "viridian_forest", "viridian_forest", "first"), "A BUG CATCHER in VIRIDIAN FOREST."),
    ("spot_ilex_forest", "Ilex Forest (wild)", ("room", "ilex_forest", "ilex_forest", "spawn"), "A wild POKéMON in ILEX FOREST."),
    ("spot_safari_zone", "Safari Zone (wild)", ("room", "safari_zone", "safari_zone", "spawn"), "A wild POKéMON in the SAFARI ZONE."),
]

# The preview's box round the battle (right across, back along: towards the camera), from the field's middle.
BOX_RIGHT, BOX_BACK, BOX_DOWN, BOX_UP = (-26, 28), (-46, 14), 8, 22


def world_of(place, txy):
    x1, z1, x2, z2 = ZONES[place]
    W, H = SIZES[place]
    return x1 + int(math.floor((txy[0] + 0.5) / W * (x2 - x1 + 1))), z1 + int(math.floor((txy[1] + 0.5) / H * (z2 - z1 + 1)))


class MapGet:
    """John's world, a box at a time (read once, round a point)."""

    def __init__(self, world, x, z, y0=10, y1=80, r=80):
        self.x0, self.z0, self.y0 = x - r, z - r, y0
        self.names, self.ids = world.box(x - r, x + r, y0, y1, z - r, z + r)

    def __call__(self, x, y, z):
        i, j, k = y - self.y0, z - self.z0, x - self.x0
        if 0 <= i < self.ids.shape[0] and 0 <= j < self.ids.shape[1] and 0 <= k < self.ids.shape[2]: return self.names[self.ids[i, j, k]]
        return "minecraft:air"


class RoomGet:
    def __init__(self, path):
        self.names, self.ids, self.W, self.H, self.L = spotworld.binroom(path)
        self.y0 = 0

    def __call__(self, x, y, z):
        if 0 <= y < self.H and 0 <= z < self.L and 0 <= x < self.W: return self.names[self.ids[y, z, x]]
        return "minecraft:stone" if y < 2 else "minecraft:air"


def standable(get, x, z, ytop, ybot):
    """The highest spot on a column you could stand on (two clear above), between two heights."""
    for y in range(ytop, ybot - 1, -1):
        if BS.kind(get(x, y, z)) == BS.SOLID and BS.kind(get(x, y + 1, z)) != BS.SOLID and BS.kind(get(x, y + 2, z)) != BS.SOLID:
            return y + BS.top_of(get(x, y, z))
    return None


def near_stand(get, x, z, ytop, ybot, r=8):
    best = None
    for dz in range(-r, r + 1):
        for dx in range(-r, r + 1):
            y = standable(get, x + dx, z + dz, ytop, ybot)
            if y is None: continue
            d = math.hypot(dx, dz)
            if best is None or d < best[0]: best = (d, x + dx, z + dz, y)
    return best


# ---------- what each block looks like ----------
def rules():
    """The mod's area looks (texture swaps only), with their boxes."""
    out = []
    for r in table("area_looks.tsv"):
        if len(r) < 9 or r[0] != "swap": continue
        blocks = set(r[1].split(","))
        x0, z0, x1, z1 = int(r[2]), int(r[3]), int(r[4]), int(r[5])
        y0 = None if r[6] == "-" else int(r[6]); y1 = None if r[7] == "-" else int(r[7])
        inter = len(r) > 10 and r[10] == "interiors"
        swaps = {}
        for s in r[8].split(","):
            if "=" not in s: continue
            a, b = s.split("=", 1)
            per = 0
            m = re.search(r"@(\d+)", b)
            if m: per = int(m.group(1))
            b = re.sub(r"@\d+", "", b).replace("!", "")
            top, side = (b.split("|") + [None])[:2]
            swaps[a.replace("block/", "")] = (top, side, per)
        out.append({"blocks": blocks, "box": (x0, z0, x1, z1, y0, y1), "swaps": swaps, "area": (x1 - x0 + 1) * (z1 - z0 + 1), "inter": inter})
    return out


WOODS = ("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "bamboo", "crimson", "warped")


def textures_of(b):
    """A block's textures as Minecraft names them: (top, side, bottom)."""
    if b == "grass_block": return "grass_block_top", "grass_block_side", "dirt"
    if b == "dirt_path": return "dirt_path_top", "dirt_path_side", "dirt"
    if b in ("podzol", "mycelium"): return b + "_top", b + "_side", "dirt"
    if b.endswith("_log") or b.endswith("_stem"): return b + "_top", b, b + "_top"
    if b.endswith("_wood") or b.endswith("_hyphae"):
        n = b.replace("_wood", "_log").replace("_hyphae", "_stem"); return n, n, n
    if b in ("brown_mushroom_block", "red_mushroom_block"): return b, b, "mushroom_block_inside"
    if b in ("sandstone", "red_sandstone"): return b + "_top", b, b + "_bottom"
    if b in ("smooth_sandstone",): return "sandstone_top", "sandstone_top", "sandstone_top"
    if b in ("quartz_block",): return "quartz_block_top", "quartz_block_side", "quartz_block_bottom"
    for suf in ("_stairs", "_slab", "_fence_gate", "_fence", "_wall", "_trapdoor", "_door", "_button", "_pressure_plate", "_shelf"):
        if b.endswith(suf):
            n = b[:-len(suf)]
            if n in WOODS: n = n + "_planks"
            elif n.endswith("_brick"): n = n + "s"
            elif n.endswith("_tile"): n = n + "s"
            elif n == "smooth_stone": return "smooth_stone", "smooth_stone_slab_side", "smooth_stone"
            elif n == "quartz": return "quartz_block_top", "quartz_block_side", "quartz_block_bottom"
            elif n in ("sandstone", "red_sandstone"): return n + "_top", n, n + "_bottom"
            elif n == "petrified_oak": n = "oak_planks"
            return n, n, n
    if b.endswith("_carpet"): n = b.replace("_carpet", "_wool"); return n, n, n
    if b == "moss_carpet": return "moss_block", "moss_block", "moss_block"
    return b, b, b


# Colours for the pictures the look pack hasn't (approximately Minecraft's own; a plain picture with a little grain).
COLOURS = {
    "white": "#E9ECEC", "orange": "#F07613", "magenta": "#BD44B3", "light_blue": "#3AAFD9", "yellow": "#F8C627", "lime": "#70B919", "pink": "#ED8DAC",
    "gray": "#3E4447", "light_gray": "#8E8E86", "cyan": "#158991", "purple": "#792AAC", "blue": "#35399D", "brown": "#724728", "green": "#546D1B",
    "red": "#A12722", "black": "#141519",
}
WOOD_COL = {"oak": "#A2834F", "spruce": "#735531", "birch": "#C0AF79", "jungle": "#A07351", "acacia": "#A85A32", "dark_oak": "#42291A", "mangrove": "#763331",
            "cherry": "#E2B2AD", "pale_oak": "#E3D8D3", "bamboo": "#C2AE52", "crimson": "#653047", "warped": "#2B6963"}
NAMED = {"stone": "#7E7E7E", "andesite": "#888889", "polished_andesite": "#848686", "diorite": "#BCBCBC", "polished_diorite": "#C1C1C3", "granite": "#956756",
         "polished_granite": "#9A6A59", "tuff": "#6C6D67", "polished_tuff": "#626863", "deepslate": "#505052", "polished_deepslate": "#484849",
         "deepslate_tiles": "#373737", "deepslate_bricks": "#464646", "cobblestone": "#7A7A7A", "mossy_cobblestone": "#6E7660", "stone_bricks": "#7A797A",
         "smooth_stone": "#9E9E9E", "smooth_stone_slab_side": "#A8A8A8", "bricks": "#976253", "mud_bricks": "#89684F", "packed_mud": "#8E6B50", "mud": "#3C393D",
         "prismarine_bricks": "#63AB9E", "prismarine": "#639C97", "dark_prismarine": "#33584B", "calcite": "#DFE0DC", "dripstone_block": "#866B5C",
         "sand": "#DBCFA3", "red_sand": "#BE6721", "gravel": "#847F7F", "clay": "#A0A6B3", "terracotta": "#985E43", "glass": "#C0DEE6",
         "quartz_block_top": "#ECE6DF", "quartz_block_side": "#EBE5DE", "quartz_block_bottom": "#ECE6DF", "sandstone_top": "#E0D6AA", "sandstone": "#D8CB9B",
         "sandstone_bottom": "#D8CB9B", "red_sandstone_top": "#B5621F", "red_sandstone": "#B5622A", "red_sandstone_bottom": "#B5622A",
         "chiseled_red_sandstone": "#B05D24", "sea_lantern": "#ACC7BE", "glowstone": "#ABDE7B", "redstone_lamp": "#5F3620", "daylight_detector": "#837563",
         "snow_block": "#F9FEFE", "snow": "#F9FEFE", "ice": "#91B7FD", "packed_ice": "#8DB4FA", "blue_ice": "#74A8FD", "obsidian": "#0F0A18",
         "hay_block": "#A68B0C", "bookshelf": "#75603D", "iron_block": "#DCDCDC", "gold_block": "#F6D03D", "copper_block": "#C06C50",
         "waxed_copper_grate": "#C06C50", "waxed_exposed_copper_grate": "#A1806B", "water": "#3F76E4", "lava": "#CF5B13", "coarse_dirt": "#77553B",
         "rooted_dirt": "#90674D", "moss_block": "#596E2D", "mushroom_block_inside": "#C9AA78", "brown_mushroom_block": "#956F52",
         "red_mushroom_block": "#C82E2D", "end_stone": "#DBDE9E", "dirt_path_top": "#94793F", "iron_chain": "#3A4150", "iron_bars": "#7A7A7A",
         "lantern": "#5E5340", "waxed_copper_lantern": "#A7684E"}
GRASS_TINT = (134, 217, 104)    # the grass's green as the viewer's has it (client/Colours brightens the biome's)
LEAF_TINT = {"birch_leaves": (205, 255, 105)}  # (client/Colours: Kanto's birch leaves)
LEAF_TINT_DEFAULT = (120, 190, 70)


def colour_of(tex):
    if tex in NAMED: return NAMED[tex]
    for w, c in WOOD_COL.items():
        if tex.startswith(w + "_") or tex.startswith("stripped_" + w + "_"): return c if "stripped" not in tex else c
    for k in sorted(COLOURS, key=len, reverse=True):
        if tex.startswith(k + "_"): return COLOURS[k]
    if tex.endswith("_leaves"): return "#4E7F2A"
    return "#8A8A8A"


def painted(tex):
    """A plain 16 by 16 picture of a block in its colour, with a little grain (and a pattern for planks, bricks, logs)."""
    c = colour_of(tex)
    r0, g0, b0 = int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16)
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    rng = np.random.default_rng(abs(hash(tex)) % (2 ** 32))
    glass = "glass" in tex
    for y in range(16):
        for x in range(16):
            k = 1 + (rng.random() - 0.5) * 0.12
            if tex.endswith("_planks") and y % 4 == 3: k *= 0.82
            if (tex.endswith("bricks") or tex in ("bricks",)) and (y % 4 == 3 or (x + (y // 4) * 4) % 8 == 7): k *= 0.78
            if tex.endswith("_log") or tex.endswith("_stem"): k *= 0.86 if x % 4 == 0 else 1.0
            if tex.endswith("_log_top") and (max(abs(x - 7.5), abs(y - 7.5)) in (2.5, 5.5)): k *= 0.8
            a = 255
            if glass:
                a = 90 if 1 <= x <= 14 and 1 <= y <= 14 else 220
            px[x, y] = (max(0, min(255, int(r0 * k))), max(0, min(255, int(g0 * k))), max(0, min(255, int(b0 * k))), a)
    return im


LOOK_PACK = {os.path.splitext(f)[0] for f in os.listdir(LOOK) if f.endswith(".png")}
AREA_TEX = {os.path.splitext(f)[0] for f in os.listdir(AREA) if f.endswith(".png")}


class Looks:
    def __init__(self):
        self.rules = rules()
        self.tex = {}      # picture name -> PNG (the ones the viewer doesn't have already)

    def pic(self, tex, tint=None):
        """The viewer's material for one of Minecraft's pictures: the look pack's (tinted as the game tints it), or a plain one."""
        if tex in LOOK_PACK:
            name = "lk_" + tex + ("_t" if tint else "")
            if name not in self.tex:
                im = Image.open(os.path.join(LOOK, tex + ".png")).convert("RGBA")
                if im.size[0] != im.size[1]: im = im.crop((0, 0, im.size[0], im.size[0]))
                if im.size[0] > 16: im = im.resize((16, 16), Image.NEAREST)
                if tint:
                    a = np.array(im).astype(float); a[..., :3] *= np.array(tint) / 255.0; im = Image.fromarray(a.clip(0, 255).astype(np.uint8))
                if tex == "grass_block_side":
                    # The grass's green over the side's top, as Minecraft lays its overlay.
                    ov = Image.open(os.path.join(LOOK, "grass_block_side_overlay.png")).convert("RGBA")
                    o = np.array(ov).astype(float); o[..., :3] *= np.array(GRASS_TINT) / 255.0
                    im = Image.alpha_composite(im, Image.fromarray(o.clip(0, 255).astype(np.uint8)))
                self.tex[name] = im
            return name
        name = "mc_" + tex + ("_t" if tint else "")
        if name not in self.tex:
            im = painted(tex)
            if tint and tex.endswith("_leaves"):
                a = np.array(im).astype(float); a[..., :3] = np.array(tint) * (0.55 + 0.25 * np.random.default_rng(3).random((16, 16, 1))); im = Image.fromarray(a.clip(0, 255).astype(np.uint8))
            self.tex[name] = im
        return name

    def rule_at(self, b, x, y, z, inter):
        best = None
        for r in self.rules:
            if r["inter"] != inter or b not in r["blocks"]: continue
            x0, z0, x1, z1, y0, y1 = r["box"]
            if not (x0 <= x <= x1 and z0 <= z <= z1): continue
            if y0 is not None and not (y0 <= y <= y1): continue
            if best is None or r["area"] < best["area"]: best = r
        return best

    def faces(self, state, rule):
        """(top, side, bottom) materials for a block (with the rule that covers it, or None)."""
        b = BS.base(state)
        t, s, bo = textures_of(b)
        out = []
        for i, tex in enumerate((t, s, bo)):
            if rule and tex in rule["swaps"]:
                top, side, per = rule["swaps"][tex]
                pick = side if (i == 1 and side) else top
                if pick in AREA_TEX:
                    out.append("area_" + pick + ("@%d" % per if per else "")); continue
            tint = None
            if tex in ("grass_block_top",) or b in ("short_grass", "tall_grass", "fern", "large_fern", "bush", "moss_block", "moss_carpet", "green_concrete_powder"):
                tint = GRASS_TINT if tex in ("grass_block_top", "short_grass", "tall_grass_bottom", "tall_grass_top", "fern", "large_fern_bottom", "large_fern_top", "moss_block", "green_concrete_powder", "bush") else None
            if b.endswith("_leaves") and b not in ("azalea_leaves", "flowering_azalea_leaves", "cherry_leaves"): tint = LEAF_TINT.get(b, LEAF_TINT_DEFAULT)
            out.append(self.pic(tex, tint))
        return out


def shape_of(state):
    b = BS.base(state)
    p = dict(kv.split("=", 1) for kv in state[state.index("[") + 1:-1].split(",")) if "[" in state else {}
    k = BS.kind(state)
    if k == BS.FLUID: return "water" if b != "lava" else "lava", p
    if k == BS.PLANT: return ("tall" if b in ("tall_grass", "large_fern", "sunflower", "lilac", "rose_bush", "peony", "tall_seagrass", "tall_dry_grass") else "cross"), p
    if k == BS.FLAT: return "carpet", p
    if k == BS.LEAVES: return "leaves", p
    if b.endswith("_slab"): return ("slab_" + p.get("type", "bottom")), p
    if b.endswith("_stairs"): return "stairs", p
    if b.endswith("_fence") or b.endswith("_wall") and k == BS.DECO: return "post", p
    if b.endswith("_fence_gate"): return "gate", p
    if b.endswith("_pane") or b in ("iron_bars", "glass_pane"): return "pane", p
    if b.endswith("_trapdoor"): return "trapdoor", p
    if b.endswith("_door"): return "door", p
    if k == BS.DECO: return "small", p
    return "cube", p


# ---------- one preview ----------
def preview(pid, name, where, note, world, looks):
    if where[0] == "map":
        place, who = where[1], where[2]
        if isinstance(who, tuple): tx, tz = who; facing = None
        elif who == "grass":
            x1, z1, x2, z2 = ZONES[place]; tx, tz = (x1 + x2) // 2, (z1 + z2) // 2; facing = None
        else:
            row = TRAINERS[who]; tx, tz = world_of(place, (int(row[12]), int(row[13]))); facing = row[4] if row[4] in FACING else "down"
        get = MapGet(world, tx, tz)
        # The street's height round there (the commonest ground), so the spot's on it, not on a roof.
        hs = collections.Counter()
        for dz in range(-24, 25, 4):
            for dx in range(-24, 25, 4):
                y = standable(get, tx + dx, tz + dz, 70, 20)
                if y is not None: hs[int(y)] += 1
        street = hs.most_common(1)[0][0]
        found = near_stand(get, tx, tz, street + 2, street - 3, 10) if place.endswith("_city") or place.endswith("_town") else near_stand(get, tx, tz, 70, 20, 8)
        if who == "grass":
            # Its nearest tall grass.
            best = None
            for dz in range(-30, 31):
                for dx in range(-30, 31):
                    for y in range(60, 20, -1):
                        if BS.base(get(tx + dx, y, tz + dz)) == "tall_grass" and BS.kind(get(tx + dx, y - 1, tz + dz)) == BS.SOLID:
                            d = math.hypot(dx, dz)
                            if best is None or d < best[0]: best = (d, tx + dx, tz + dz, y)
                            break
            _, tx, tz, ty = best; found = (0, tx, tz, ty)
        _, tx, tz, ty = found
        inter = False
        origin = (0, 0, 0)
    else:
        jname, rid, who = where[1], where[2], where[3]
        room = json.load(open(os.path.join(INTER, jname + ".json")))["rooms"][rid]
        get = RoomGet(os.path.join(INTER, room["schem"]))
        floor = -room["base"]
        if who == "first":
            p = [q for q in room["people"] if q.get("role") == "trainer"][0]
            tx, tz = int(math.floor(p["x"])), int(math.floor(p["z"]))
            ty = floor + (p.get("y") or 0)
            facing = p.get("facing")
            if not facing:
                yaw = p.get("yaw", 0.0) % 360
                facing = {0: "down", 90: "left", 180: "up", 270: "right"}[int(round(yaw / 90.0)) % 4 * 90]
        elif isinstance(who, tuple):
            tx, tz = who; ty = floor; facing = None
        else:
            tx, tz = int(math.floor(room["spawn"]["x"])), int(math.floor(room["spawn"]["z"])); facing = None
            ty = floor + (room["spawn"].get("y") or 0)
        inter = True
        origin = tuple(room["origin"])
    # Where you stand: a few blocks in front of the trainer (they come to you), facing them.
    if facing:
        fx, fz = FACING[facing]
        px, pz = tx + fx * 4, tz + fz * 4
        prefer = BS.DIRS.index((-fx, -fz))
    else:
        px, pz = tx, tz
        prefer = 0
    st = near_stand(get, px, pz, int(ty) + 4, int(ty) - 5, 6)
    if st: _, px, pz, py = st
    else: py = ty
    reach = 10 if inter else 6
    res = BS.find(get, px + 0.5, py, pz + 0.5, prefer=prefer, reach=reach, trainer=bool(facing))
    cleared = set(res["clears"])
    # The box round it.
    cx, cy, cz = res["centre"]
    fx, fz = res["f"]; rx, rz = res["r"]
    corners = [(cx + rx * a - fx * b, cz + rz * a - fz * b) for a in BOX_RIGHT for b in BOX_BACK]
    x0, x1 = int(math.floor(min(c[0] for c in corners))), int(math.floor(max(c[0] for c in corners)))
    z0, z1 = int(math.floor(min(c[1] for c in corners))), int(math.floor(max(c[1] for c in corners)))
    y0, y1 = int(math.floor(cy)) - BOX_DOWN, int(math.floor(cy)) + BOX_UP
    if isinstance(get, MapGet) and (x0 < get.x0 or z0 < get.z0 or x1 > get.x0 + get.ids.shape[2] - 1 or z1 > get.z0 + get.ids.shape[1] - 1):
        get = MapGet(world, (x0 + x1) // 2, (z0 + z1) // 2, r=max(x1 - x0, z1 - z0) // 2 + 2)

    def S(x, y, z):
        return "minecraft:air" if (x, y, z) in cleared else get(x, y, z)
    pal, palix, cells = [], {}, []
    shapes = {}

    def opaque(st, me):
        if st not in shapes: shapes[st] = shape_of(st)[0]
        if BS.kind(st) == BS.SOLID and shapes[st] == "cube": return True
        # (Leaves against leaves: the face between them left out, as the game's fast leaves.)
        return me == BS.LEAVES and BS.kind(st) == BS.LEAVES
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                st = S(x, y, z)
                k = BS.kind(st)
                if k == BS.AIR: continue
                # Faces you could see: next to something that isn't a whole opaque block.
                mask = 0
                for i, (dx, dy, dz) in enumerate(((0, 1, 0), (0, -1, 0), (0, 0, -1), (0, 0, 1), (-1, 0, 0), (1, 0, 0))):
                    n = S(x + dx, y + dy, z + dz)
                    if not opaque(n, k): mask |= 1 << i
                if k == BS.FLUID:
                    above = S(x, y + 1, z)
                    mask = 1 if BS.kind(above) != BS.FLUID else 0
                if not mask: continue
                b = BS.base(st)
                rule = looks.rule_at(b, x + origin[0], y, z + origin[2], inter)
                key = (st, id(rule) if rule else 0)
                if key not in palix:
                    shp, props = shape_of(st)
                    top, side, bottom = looks.faces(st, rule)
                    palix[key] = len(pal)
                    pal.append({"s": shp, "t": top, "d": side, "b": bottom, "p": {k2: v for k2, v in props.items() if k2 in ("facing", "half", "type", "shape", "open", "axis")}, "n": b})
                cells.append((x - x0, y - y0, z - z0, palix[key], mask))
    arr = np.array(cells, dtype=np.int32)
    blob = np.zeros((len(cells), 6), np.uint8)
    blob[:, 0] = arr[:, 0]; blob[:, 1] = arr[:, 1]; blob[:, 2] = arr[:, 2]
    blob[:, 3] = arr[:, 3] & 255; blob[:, 4] = arr[:, 3] >> 8; blob[:, 5] = arr[:, 4]
    sp = res["spots"]

    def loc(p):
        return [round(p[0] - x0, 3), round(p[1] - y0, 3), round(p[2] - z0, 3)]
    out = {"id": pid, "name": name, "note": note, "W": x1 - x0 + 1, "H": y1 - y0 + 1, "D": z1 - z0 + 1, "pal": pal,
           "cells": base64.b64encode(blob.tobytes()).decode(), "n": len(cells),
           "hero": loc(sp["hero"][:3]), "mine": loc(sp["mine"][:3]), "enemy": loc(sp["enemy"][:3]), "foe": loc(sp["foe"][:3]) if facing else None,
           "wet": {k: bool(v[3]) for k, v in sp.items()},
           "cam": loc(res["cam"]), "look": loc(res["look"]), "dir": res["dir"], "camBack": res["cam_back"], "why": res["why"], "moved": list(res["moved"]), "cost": res["cost"], "cleared": len(cleared),
           "clearedKinds": dict(collections.Counter(BS.base(get(*c)) for c in cleared).most_common(12)),
           "where": [px + (origin[0] if inter else 0), pz + (origin[2] if inter else 0)], "inter": inter}
    if facing:
        row = TRAINERS.get(where[2]) if where[0] == "map" else None
        out["foeSkin"] = row[3] if row else None
    return out


def main():
    world = spotworld.World(sys.argv[1])
    looks = Looks()
    out = []
    only = set(sys.argv[2:])
    for pid, name, where, note in PREVIEWS:
        if only and pid not in only: continue
        p = preview(pid, name, where, note, world, looks)
        print("%-22s dir %d moved %-8s camback %4.1f cost %7.1f cleared %4d %s cells %d %s" % (pid, p["dir"], p["moved"], p["camBack"], p["cost"], p["cleared"], p["clearedKinds"], p["n"], p["why"]))
        out.append(p)
    tex = {}
    for k, im in looks.tex.items():
        tex[k] = {"w": im.size[0], "h": im.size[1], "d": base64.b64encode(im.tobytes()).decode()}
    js = ("// Written by tools/spots.py from John's world and block rooms: battles on the spot, as the game lays them out\n"
          "// (tools/battlespot.py, client/BattleSpot.java).\nconst SPOTS = " + json.dumps(out, separators=(",", ":")) + ";\nconst SPOTTEX = " + json.dumps(tex, separators=(",", ":")) + ";\n")
    open(OUT, "w").write(js)
    print("wrote", OUT, len(js))


if __name__ == "__main__":
    main()
