#!/usr/bin/env python3
"""
(0.64, John's 07:10) A battle fought on the spot, in John's own build: where it's fought, which way, where the camera
stands, and what's cleared out of the way (on your screen only). The game does the same, block for block
(client/BattleSpot.java); this is the same rules for the Interior Viewer's previews (tools/spots.py).

  "take a portion of the actual schematic ... it always looks like my schematic, but you've maybe cleared the grass
   area ... Obviously camera angles not being obscured by stuff ... a distant horizon where you just clear it down"

The rules:
- The battle's laid out as the stages were: you (the trainer) behind yours, theirs across the field with their trainer
  behind, the camera behind yours and a little above, looking across. It's laid along one of eight ways (north,
  north-east, east...): the way to the trainer who's battling you (or the way you're facing, for a wild one) unless
  another is clearly more open.
- Its field and the camera's view are a corridor of the world: 10 blocks across, from behind their trainer to behind
  the camera, 6 blocks up from the ground.
- Never cut: anything solid (the ground, rock, buildings, tree trunks, the cave's walls). Where something solid would
  be in the field or the camera's way, the battle's moved to the nearest spot that's open (up to 6 blocks away outside,
  10 in a cave or a forest), or the camera's brought in closer.
- Cleared from the corridor: grass, flowers, bushes and the like; leaves; fences, walls, signs, lamp posts and the like.
- Everyone else (people, wandering POKéMON) near the field steps out of the picture.
"""
import math

_D = math.sqrt(0.5)
# The way from you to them: north, north-east, east ... north-west (the diagonals for the caves' slanting tunnels).
DIRS = [(0, -1), (_D, -_D), (1, 0), (_D, _D), (0, 1), (-_D, _D), (-1, 0), (-_D, -_D)]


def turn_cost(d, prefer):
    """How much a way that isn't the one you'd face costs (a diagonal a little more than straight)."""
    diag = 1.5 if d % 2 else 0.0
    if prefer is None: return diag
    k = min((d - prefer) % 8, (prefer - d) % 8)
    return diag + (0.0, 2.5, 4.0, 6.0, 8.0)[k]

AIR, FLUID, FLAT, PLANT, LEAVES, DECO, SOLID = range(7)

_AIR = {"air", "cave_air", "void_air", "light", "structure_void", "barrier"}
_FLUID = {"water", "lava", "bubble_column"}
_FLAT_END = ("_carpet", "_pressure_plate")
_FLAT = {"snow", "rail", "powered_rail", "detector_rail", "activator_rail", "tripwire", "redstone_wire", "lily_pad", "leaf_litter",
         "pink_petals", "wildflowers", "moss_carpet", "pale_moss_carpet"}
_PLANT = {"short_grass", "grass", "tall_grass", "fern", "large_fern", "dead_bush", "bush", "firefly_bush", "short_dry_grass", "tall_dry_grass",
          "sweet_berry_bush", "sugar_cane", "brown_mushroom", "red_mushroom", "vine", "glow_lichen", "hanging_roots", "dandelion", "poppy",
          "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
          "lily_of_the_valley", "wither_rose", "sunflower", "lilac", "rose_bush", "peony", "torchflower", "pitcher_plant", "open_eyeblossom",
          "closed_eyeblossom", "cactus_flower", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "cobweb", "nether_sprouts", "crimson_roots",
          "warped_roots", "crimson_fungus", "warped_fungus", "spore_blossom", "small_dripleaf", "cave_vines", "cave_vines_plant",
          "twisting_vines", "twisting_vines_plant", "weeping_vines", "weeping_vines_plant", "pale_hanging_moss", "big_dripleaf_stem"}
_DECO_END = ("_fence", "_fence_gate", "_wall", "_pane", "_sign", "_wall_sign", "_hanging_sign", "_wall_hanging_sign", "_banner", "_wall_banner",
             "_button", "_candle", "_head", "_wall_head", "_skull", "_wall_skull", "_lantern", "_torch", "_wall_torch", "_shelf", "_chain")
_DECO = {"lantern", "soul_lantern", "torch", "wall_torch", "soul_torch", "soul_wall_torch", "redstone_torch", "redstone_wall_torch", "chain",
         "iron_chain", "iron_bars", "flower_pot", "end_rod", "lightning_rod", "lever", "scaffolding", "bell", "candle", "glass_pane",
         "copper_bars", "tripwire_hook", "decorated_pot"}


def base(state):
    """A block state's block, without "minecraft:" and its properties."""
    s = state.split("[", 1)[0]
    return s[10:] if s.startswith("minecraft:") else s


def kind(state):
    b = base(state)
    if b in _AIR: return AIR
    if b in _FLUID: return FLUID
    if b in _FLAT or b.endswith(_FLAT_END): return FLAT
    if b in _PLANT or (b.endswith("_sapling") and not b.startswith("potted_")): return PLANT
    if b.endswith("_leaves"): return LEAVES
    if b in _DECO or b.startswith("potted_") or b.endswith(_DECO_END): return DECO
    return SOLID


def top_of(state):
    """How high a solid block's top is in its block (a bottom slab's at a half, a snow layer's by its layers)."""
    b = base(state)
    props = state[state.index("[") + 1:-1].split(",") if "[" in state else []
    p = dict(kv.split("=", 1) for kv in props if "=" in kv)
    if b.endswith("_slab") and p.get("type") == "bottom": return 0.5
    if b == "snow": return int(p.get("layers", "1")) / 8.0
    return 1.0


# The layout, along the battle (right: to your right; back: towards the camera), from the middle of the field, as the
# stages had it (client/BattleScene: far 6 for a POKéMON up to a block and a half tall, more for taller).
def layout(h=1.0):
    far = 6.0 + max(0.0, h - 1.5) * 1.3
    mine = (-0.6, 2.2)
    enemy = (1.8, 2.2 - far)
    hero = (mine[0] - 2.0, mine[1] + 1.5)
    foe = (enemy[0] + 2.1, enemy[1] - 1.8 - max(0.0, h - 1.5) * 0.4)
    return {"far": far, "mine": mine, "enemy": enemy, "hero": hero, "foe": foe, "foe_h": max(0.0, h - 1.5) * 0.4,
            "cam_up": 2.2 + max(0.0, h - 2) * 0.4, "cam_right": -1.4, "cam_back": 9.8 + max(0.0, h - 2) * 0.9}


CAM_BACKS = (0.0, -1.4, -2.8, -4.0)   # how much closer the camera may come
ACROSS = (-4.5, 5.5)                  # the corridor across (right), from the field's middle
UP = 6                                # its height above the ground, in blocks (what's cleared)
COST_UP = 4                           # how high something solid gets in the way (a cave's roof over it doesn't)


FOE_OFFSETS = ((2.1, -1.8), (1.2, -1.6), (0.3, -1.6), (2.8, -0.6), (-0.8, -1.6))  # their trainer, from theirs


def find(get, px, py, pz, prefer=None, h=1.0, reach=6, water=False, trainer=True):
    """
    get(x, y, z): the block state there (a string). (px, py, pz): your feet. prefer: 0..7 (DIRS) or None. reach: how
    far the battle may move (it looks twice as far, up to 16, if there's nowhere open that near). trainer: whether
    there's a trainer to stand behind theirs.
    Returns the battle as it's laid out: its way, the field's middle, everyone's spots, the camera, what to clear.
    """
    L = layout(h)
    W = _World(get)
    first = _search(W, L, px, py, pz, prefer, reach, water, trainer)
    if first["bad"] and reach < 16:
        wider = _search(W, L, px, py, pz, prefer, min(16, reach * 2), water, trainer)
        if wider["cost"] < first["cost"]: first = wider
    return _lay_out(W, L, first, h, trainer)


class _World:
    """The blocks, read once each: their kind, the ground's top at a column, whether there's room to stand."""

    def __init__(self, get):
        self.get, self.k, self.s = get, {}, {}

    def K(self, x, y, z):
        key = (x, y, z)
        v = self.k.get(key)
        if v is None:
            st = self.get(x, y, z); self.s[key] = st
            v = self.k[key] = kind(st)
        return v

    def surface(self, x, z, yref):
        """The ground's top at a column near a height (None if there's none close), and whether it's water."""
        y = int(math.floor(yref)) + 2
        while y >= int(math.floor(yref)) - 4:
            k = self.K(x, y, z)
            if k == SOLID: return y + top_of(self.s[(x, y, z)]), False
            if k == FLUID: return y + 0.8, True
            y -= 1
        return None, False

    def room(self, x, z, top):
        """Two blocks clear over a spot (nothing solid: plants and the like are cleared)."""
        y = int(math.floor(top + 0.01))
        return self.K(x, y, z) != SOLID and self.K(x, y + 1, z) != SOLID

    def stand(self, wx, wz, ay):
        """The ground's top where someone stands (near the field's height), or None if they can't."""
        x, z = int(math.floor(wx)), int(math.floor(wz))
        sy, wet = self.surface(x, z, ay)
        if sy is None or sy < ay - 1.6 or sy > ay + 1.1 or not self.room(x, z, sy): return None, wet
        return sy, wet


def _frame(L, d, ax, az):
    fx, fz = DIRS[d]
    rx, rz = -fz, fx
    # The field's middle, from where you stand (you're at hero).
    cx = ax + 0.5 - rx * L["hero"][0] + fx * L["hero"][1]
    cz = az + 0.5 - rz * L["hero"][0] + fz * L["hero"][1]
    return fx, fz, rx, rz, cx, cz


def _foe_spot(W, L, at, ay):
    """Their trainer's spot: behind theirs (the first of FOE_OFFSETS they can stand on), or None."""
    ex, eb = L["enemy"]
    for orr, ob in FOE_OFFSETS:
        wx, wz = at(ex + orr, eb + ob - L["foe_h"])
        sy, wet = W.stand(wx, wz, ay)
        if sy is not None: return wx, sy, wz, wet
    return None


def _search(W, L, px, py, pz, prefer, reach, water, trainer):
    bx, bz = int(math.floor(px)), int(math.floor(pz))
    y0, _ = W.surface(bx, bz, py)
    if y0 is None: y0 = py
    best = None
    for dz in range(-reach, reach + 1):
        for dx in range(-reach, reach + 1):
            dist = math.hypot(dx, dz)
            if dist > reach + 0.01: continue
            ax, az = bx + dx, bz + dz
            ay, wet = W.surface(ax, az, y0)
            if ay is None or abs(ay - y0) > 1.5 or not W.room(ax, az, ay): continue
            if wet and not water: continue
            for d in range(8):
                fx, fz, rx, rz, cx, cz = _frame(L, d, ax, az)
                pen = turn_cost(d, prefer)

                def at(right, back):
                    return cx + rx * right - fx * back, cz + rz * right - fz * back
                # The standing spots: yours and theirs (they can stand in water: a POKéMON there floats), their trainer.
                spot_cost, bad = 0.0, False
                for who in ("mine", "enemy"):
                    sy, swet = W.stand(*at(*L[who]), ay)
                    if sy is None: spot_cost += 60; bad = True
                if trainer:
                    f = _foe_spot(W, L, at, ay)
                    if f is None: spot_cost += 60; bad = True
                    elif f[3] and not water: spot_cost += 6
                yi = int(math.floor(ay - 0.01))
                # The field itself on the ground at its height (a bridge's deck, not the water either side of it).
                ground = 0.0
                for (wx, wz, right, back) in _cells(cx, cz, rx, rz, fx, fz, L["enemy"][1] - 1.0, L["mine"][1] + 1.0):
                    if not -2.5 <= right <= 3.5: continue
                    gy, gwet = W.surface(wx, wz, ay)
                    if gy is None or abs(gy - ay) > 1.6: ground += 1.5
                    elif gwet and not water: ground += 0.8
                for cb in CAM_BACKS:
                    cam_back = L["cam_back"] + cb
                    cost = pen + dist * 1.2 + spot_cost + ground + (-cb) * 3.0
                    if best is not None and cost >= best["cost"]: continue
                    camx, camz = at(L["cam_right"], cam_back)
                    camy = ay + L["cam_up"]
                    if W.K(int(math.floor(camx)), int(math.floor(camy)), int(math.floor(camz))) == SOLID or \
                            W.K(int(math.floor(camx)), int(math.floor(camy)) + 1, int(math.floor(camz))) == SOLID:
                        cost += 200
                    # Nothing solid between the camera and any of them (or what it looks at).
                    eye = (camx, camy, camz)
                    blocked = 0
                    for who in ("mine", "enemy") + (("foe",) if trainer else ()):
                        tx, tz = at(*L[who])
                        blocked += _blocked(W, eye, (tx, ay + 0.9, tz))
                    cost += 40 * blocked
                    core_solid = 0
                    for (wx, wz, right, back) in _cells(cx, cz, rx, rz, fx, fz, L["foe"][1] - 2.0, cam_back + 1.2):
                        core = -2.5 <= right <= 3.5 and L["enemy"][1] - 1.0 <= back <= L["mine"][1] + 1.0
                        for dy in range(1, COST_UP + 1):
                            k = W.K(wx, yi + dy, wz)
                            if k == SOLID:
                                if core and dy <= 2: cost += 10; core_solid += 1
                                else: cost += 2
                            elif k == DECO: cost += 1.5 if core else 1.0
                            elif k == LEAVES: cost += 0.4 if core else 0.3
                        if best is not None and cost >= best["cost"]: break
                    if best is None or cost < best["cost"]:
                        best = {"cost": cost, "d": d, "ax": ax, "az": az, "ay": ay, "cam_back": cam_back, "bad": bad or core_solid > 0 or blocked > 0,
                                "moved": (ax - bx, az - bz)}
    return best


def _lay_out(W, L, b, h, trainer):
    d, ax, az, ay, cam_back = b["d"], b["ax"], b["az"], b["ay"], b["cam_back"]
    fx, fz, rx, rz, cx, cz = _frame(L, d, ax, az)

    def at(right, back):
        return cx + rx * right - fx * back, cz + rz * right - fz * back
    spots = {}
    for who in ("mine", "enemy", "hero"):
        wx, wz = at(*L[who])
        sy, swet = W.surface(int(math.floor(wx)), int(math.floor(wz)), ay)
        spots[who] = (wx, (sy if sy is not None else ay), wz, swet)
    spots["hero"] = (ax + 0.5, ay, az + 0.5, False)
    if trainer:
        f = _foe_spot(W, L, at, ay)
        if f is None:
            wx, wz = at(*L["foe"]); f = (wx, ay, wz, False)
        spots["foe"] = f
    camx, camz = at(L["cam_right"], cam_back)
    cam = (camx, ay + L["cam_up"], camz)
    mx = (spots["mine"][0] + spots["enemy"][0]) / 2; mz = (spots["mine"][2] + spots["enemy"][2]) / 2
    my = (spots["mine"][1] + spots["enemy"][1]) / 2
    look = (mx, my + 0.8 + max(0.0, h - 1) * 0.15, mz)
    # What's cleared: every plant, leaf and fence (and the like) in the corridor, from two below the ground up to UP
    # (and a tall plant's top half just over it).
    yi = int(math.floor(ay - 0.01))
    clears = []
    for (wx, wz, right, back) in _cells(cx, cz, rx, rz, fx, fz, L["foe"][1] - 2.0, cam_back + 1.2):
        for dy in range(-2, UP + 2):
            k = W.K(wx, yi + dy, wz)
            if k in (PLANT, LEAVES, DECO) and (dy <= UP or k == PLANT): clears.append((wx, yi + dy, wz))
    # (For checking: what the chosen spot's cost is made of.)
    why = {"solid_core": 0, "solid_view": 0, "deco": 0, "leaves": 0,
           "blocked": sum(_blocked(W, cam, (spots[w][0], spots[w][1] + 0.9, spots[w][2])) for w in spots if w != "hero")}
    for (wx, wz, right, back) in _cells(cx, cz, rx, rz, fx, fz, L["foe"][1] - 2.0, cam_back + 1.2):
        core = -2.5 <= right <= 3.5 and L["enemy"][1] - 1.0 <= back <= L["mine"][1] + 1.0
        for dy in range(1, COST_UP + 1):
            k = W.K(wx, yi + dy, wz)
            if k == SOLID: why["solid_core" if core and dy <= 2 else "solid_view"] += 1
            elif k == DECO: why["deco"] += 1
            elif k == LEAVES: why["leaves"] += 1
    return {"why": why, "dir": d, "f": (fx, fz), "r": (rx, rz), "centre": (cx, ay, cz), "spots": spots, "cam": cam, "look": look, "clears": clears,
            "cost": round(b["cost"], 2), "moved": b["moved"], "cam_back": cam_back, "y": ay}


def _blocked(W, a, b):
    """How many solid blocks a line from a to b passes through (looked at every quarter block)."""
    n = max(1, int(math.ceil(math.dist(a, b) / 0.25)))
    seen = set()
    for i in range(1, n):
        t = i / n
        c = (int(math.floor(a[0] + (b[0] - a[0]) * t)), int(math.floor(a[1] + (b[1] - a[1]) * t)), int(math.floor(a[2] + (b[2] - a[2]) * t)))
        if c not in seen and W.K(*c) == SOLID: seen.add(c)
    return len(seen)


def _cells(cx, cz, rx, rz, fx, fz, b0, b1):
    """The blocks whose middles are in the corridor (across ACROSS, along back b0..b1): (x, z, right, back)."""
    out = []
    xs = [cx + rx * a - fx * b for a in ACROSS for b in (b0, b1)]
    zs = [cz + rz * a - fz * b for a in ACROSS for b in (b0, b1)]
    for z in range(int(math.floor(min(zs))) - 1, int(math.floor(max(zs))) + 2):
        for x in range(int(math.floor(min(xs))) - 1, int(math.floor(max(xs))) + 2):
            ox, oz = x + 0.5 - cx, z + 0.5 - cz
            right = ox * rx + oz * rz
            back = -(ox * fx + oz * fz)
            if ACROSS[0] <= right <= ACROSS[1] and b0 <= back <= b1: out.append((x, z, right, back))
    return out
