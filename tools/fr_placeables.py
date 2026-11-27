#!/usr/bin/env python3
"""
(0.72, John's 27 Nov 18:24 and 18:56: "the placement of absolutely everything. Items across the map, all the caves ...
I'll need a way where I can touch a pixel on the map and place them"; "on the next pass, I want obviously everything
in there") What the PLACER (OPTION, in game) lists for John to put down, from FireRed's own maps (pret's pokefirered):
every trainer, item ball and hidden item outside and in the caves, each with where FireRed has it on its own grid.

Writes src/main/resources/retromon/data/placeables.tsv:

    id  place  kind  key  label  detail  gx  gy  facing  sight

  kind: trainer (key: the mod's trainer, trainers.tsv's own where FireRed's is the same trainer, else one of
        fr_trainers.tsv's, written here with FireRed's team and words), item (a POKé BALL on the ground), hidden (found
        with A, or the ITEMFINDER); key, for an item, is its field item's key (field_items.tsv's where it's the
        same one, else a new one with the item in the detail column: "item=<id>").

and src/main/resources/retromon/data/fr_trainers.tsv (trainers.tsv's columns) for FireRed's trainers the mod hadn't.

    python3 tools/fr_placeables.py [/home/claude/pret/pokefirered]
"""
import json
import os
import re
import sys
from collections import Counter, defaultdict

FR = sys.argv[1] if len(sys.argv) > 1 else "/home/claude/pret/pokefirered"
HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")


def table(name):
    out = []
    for l in open(os.path.join(DATA, name), encoding="utf-8"):
        if l.startswith("#") or not l.strip(): continue
        out.append(l.rstrip("\n").split("\t"))
    return out


PLACES = {r[0] for r in table("places.tsv")}
TRAINERS = table("trainers.tsv")
FIELD = table("field_items.tsv")
TMS = {r[2]: r[0] for r in table("tms.tsv")}  # move -> tm_ id
ITEM_IDS = {r[0] for r in table("items.tsv")} | {r[0] for r in table("tms.tsv")}
SPECIES = {r[1] for r in table("species.tsv")}

# FireRed's maps outside and the caves, and the place each is in the mod.
MAPS = {}
for name in sorted(os.listdir(os.path.join(FR, "data", "maps"))):
    m = re.match(r"^(PalletTown|ViridianCity|PewterCity|CeruleanCity|VermilionCity|LavenderTown|CeladonCity|FuchsiaCity|SaffronCity|CinnabarIsland)$", name)
    if m:
        MAPS[name] = re.sub(r"(?<!^)([A-Z])", r"_\1", name).lower()
        continue
    m = re.match(r"^Route(\d+)(_North|_South)?$", name)
    if m:
        MAPS[name] = "route_" + m.group(1)
        continue
    m = re.match(r"^(ViridianForest|MtMoon|RockTunnel|SeafoamIslands|CeruleanCave|VictoryRoad|SafariZone|PowerPlant|DiglettsCave)(_([A-Z0-9a-z]+))?$", name)
    if m:
        base = {"ViridianForest": "viridian_forest", "MtMoon": "mt_moon", "RockTunnel": "rock_tunnel", "SeafoamIslands": "seafoam_islands",
                "CeruleanCave": "cerulean_cave", "VictoryRoad": "victory_road", "SafariZone": "safari_zone", "PowerPlant": "power_plant",
                "DiglettsCave": "digletts_cave"}[m.group(1)]
        sub = m.group(3)
        if sub and "RestHouse" in name: continue
        if base == "digletts_cave": MAPS[name] = "digletts_cave"
        elif sub: MAPS[name] = base + "_" + sub.lower()
        else: MAPS[name] = base
        continue
    if name == "UndergroundPath_NorthSouthTunnel": MAPS[name] = "underground_path_ns"
    if name == "UndergroundPath_EastWestTunnel": MAPS[name] = "underground_path_we"

SCRIPTS = {}
for root in (os.path.join(FR, "data", "scripts"), os.path.join(FR, "data", "maps")):
    for dp, dn, fn in os.walk(root):
        for f in fn:
            if not f.endswith(".inc") or f == "text.inc": continue
            cur = None
            for line in open(os.path.join(dp, f), encoding="utf-8", errors="replace"):
                m = re.match(r"^(\w+)::", line)
                if m:
                    cur = m.group(1)
                    SCRIPTS[cur] = []
                elif cur: SCRIPTS[cur].append(line.strip())

TEXT = {}
for dp, dn, fn in os.walk(os.path.join(FR, "data")):
    for f in fn:
        if f != "text.inc" and not f.endswith("text.inc"): continue
        cur = None
        for line in open(os.path.join(dp, f), encoding="utf-8", errors="replace"):
            m = re.match(r"^(\w+)::", line)
            if m:
                cur = m.group(1)
                TEXT[cur] = ""
                continue
            m = re.match(r'^\s*\.string "(.*)"', line)
            if m and cur: TEXT[cur] += m.group(1)


def say(label):
    """A text's words, as our lines: one per box (\\p), the line breaks as spaces."""
    t = TEXT.get(label, "")
    if not t: return []
    t = t.replace("$", "").replace("{PLAYER}", "{player}").replace("{RIVAL}", "{rival}").replace("{STR_VAR_1}", "").replace("POKé", "POKé")
    t = re.sub(r"\{[A-Z_0-9]+\}", "", t)
    out = []
    for box in t.split("\\p"):
        box = box.replace("\\n", " ").replace("\\l", " ").strip()
        if box: out.append(re.sub(r"\s+", " ", box))
    return out


# FireRed's trainers: class, name, party.
TR = {}
src = open(os.path.join(FR, "src", "data", "trainers.h"), encoding="utf-8").read()
for m in re.finditer(r"\[(TRAINER_\w+)\]\s*=\s*\{(.*?)\n    \},", src, re.S):
    body = m.group(2)
    cls = re.search(r"\.trainerClass = TRAINER_CLASS_(\w+)", body)
    nm = re.search(r'\.trainerName = _\("([^"]*)"\)', body)
    party = re.search(r"\.party = \w+\((sParty_\w+)\)", body)
    TR[m.group(1)] = (cls.group(1) if cls else "", nm.group(1) if nm else "", party.group(1) if party else "")
PARTY = {}
src = open(os.path.join(FR, "src", "data", "trainer_parties.h"), encoding="utf-8").read()
for m in re.finditer(r"(sParty_\w+)\[\]\s*=\s*\{(.*?)\n\};", src, re.S):
    mons = []
    for mm in re.finditer(r"\.lvl = (\d+),\s*\.species = SPECIES_(\w+)", m.group(2)):
        mons.append((mm.group(2).lower(), int(mm.group(1))))
    PARTY[m.group(1)] = mons
CLASS_NAME = {"BUG_CATCHER": "BUG CATCHER", "COOLTRAINER": "COOLTRAINER", "SUPER_NERD": "SUPER NERD", "TEAM_ROCKET": "TEAM ROCKET GRUNT",
              "PKMN_PROF": "PROF.", "CRUSH_GIRL": "CRUSH GIRL", "TUBER": "TUBER", "PKMN_BREEDER": "PKMN BREEDER", "RUIN_MANIAC": "RUIN MANIAC",
              "LADY": "LADY", "PAINTER": "PAINTER", "TWINS": "TWINS", "YOUNG_COUPLE": "YOUNG COUPLE", "SIS_AND_BRO": "SIS AND BRO", "CAMPER": "CAMPER",
              "PICNICKER": "PICNICKER", "BEAUTY": "BEAUTY", "GENTLEMAN": "GENTLEMAN", "ENGINEER": "ENGINEER", "BURGLAR": "BURGLAR", "BIRD_KEEPER": "BIRD KEEPER",
              "BLACK_BELT": "BLACK BELT", "CUE_BALL": "CUE BALL", "PSYCHIC": "PSYCHIC", "ROCKER": "ROCKER", "JUGGLER": "JUGGLER", "TAMER": "TAMER",
              "CHANNELER": "CHANNELER", "SWIMMER_M": "SWIMMER", "SWIMMER_F": "SWIMMER", "SCIENTIST": "SCIENTIST", "SAILOR": "SAILOR",
              "FISHERMAN": "FISHERMAN", "GAMBLER": "GAMER", "POKEMANIAC": "POKéMANIAC", "HIKER": "HIKER", "BIKER": "BIKER", "LASS": "LASS",
              "YOUNGSTER": "YOUNGSTER", "AROMA_LADY": "AROMA LADY"}
FACING = {"UP": "up", "DOWN": "down", "LEFT": "left", "RIGHT": "right"}

# The mod's sprite and prize for each trainer class (from its own trainers), for FireRed's new ones.
SPRITE, MONEY = {}, {}
by_class = defaultdict(Counter)
for r in TRAINERS:
    if len(r) < 8: continue
    cls = r[2].rsplit(" ", 1)[0] if " " in r[2] else r[2]
    by_class[cls][(r[3], r[6])] += 1
for cls, c in by_class.items():
    (sp, money), _ = c.most_common(1)[0]
    SPRITE[cls], MONEY[cls] = sp, money


def facing_of(mv):
    m = re.search(r"(UP|DOWN|LEFT|RIGHT)", mv)
    return FACING[m.group(1)] if m else "none"


def item_of(script):
    for l in SCRIPTS.get(script, []):
        m = re.match(r"finditem (ITEM_\w+)(?:,\s*(\d+))?", l)
        if m: return m.group(1), int(m.group(2) or 1)
    return None, 0


ALIAS = {}
for l in open(os.path.join(FR, "include", "constants", "items.h"), encoding="utf-8"):
    m = re.match(r"#define (ITEM_(?:TM|HM)\d+)_(\w+) (ITEM_(?:TM|HM)\d+)", l)
    if m: ALIAS[m.group(3)] = m.group(2).lower()


ITEM_NAME = {r[0]: r[1] for r in table("items.tsv") if len(r) > 1}
for r in table("tms.tsv"):
    if len(r) > 4: ITEM_NAME[r[0]] = "%s %s" % (r[3] if r[3] not in ("-", "") else r[4], r[2].replace("_", " ").upper())


def item_label(i):
    """An item as the game names it: ULTRA BALL, TM28 DIG."""
    return ITEM_NAME.get(i, i.replace("_", " ").upper())


def our_item(it):
    """FireRed's item as one of ours (a TM by its move), or None."""
    if it in ALIAS:
        mv = ALIAS[it]
        return TMS.get(mv) or TMS.get(mv.replace("_", "")) or None
    i = it[5:].lower()
    fix = {"paralyze_heal": "parlyz_heal", "pokeball": "poke_ball", "poke_ball": "poke_ball", "ss_ticket": "s_s_ticket", "elixir": "elixer",
           "max_elixir": "max_elixer", "thunder_stone": "thunderstone", "tiny_mushroom": "tinymushroom",
           # FireRed's berries as Gold and Silver's that do the same.
           "cheri_berry": "przcureberry", "chesto_berry": "mint_berry", "pecha_berry": "psncureberry", "rawst_berry": "ice_berry",
           "aspear_berry": "burnt_berry", "lum_berry": "miracleberry", "sitrus_berry": "gold_berry", "leppa_berry": "mysteryberry",
           "nanab_berry": "berry", "pinap_berry": "berry", "bluk_berry": "berry", "wepear_berry": "berry"}
    i = fix.get(i, i)
    return i if i in ITEM_IDS else None


OUTSIDE = {pl for pl in MAPS.values() if not re.match(r"^(viridian_forest|mt_moon|rock_tunnel|seafoam|cerulean_cave|victory_road|safari|power_plant|digletts|underground)", pl)}


# Those caves' grids (blocks a step across and down, and their first step's middle from the room's corner).
GRIDS = {"victory_road_1f": (2, 2, 13.5, 13.5), "victory_road_2f": (2, 2, 13.5, 13.5), "victory_road_3f": (2, 2, 13.5, 13.5),
         "rock_tunnel_1f": (2, 2, 9.5, 9.5), "rock_tunnel_b1f": (2, 2, 9.5, 9.5), "cerulean_cave_1f": (2, 2, 13.5, 13.5),
         "cerulean_cave_2f": (2, 2, 9.5, 11.5), "cerulean_cave_b1f": (2, 2, 13.5, 13.5), "seafoam_islands_1f": (2, 2, 5.5, 9.5),
         "seafoam_islands_b1f": (2, 2, 5.5, 9.5), "seafoam_islands_b2f": (2, 2, 5.5, 9.5), "seafoam_islands_b3f": (2, 2, 5.5, 9.5),
         "seafoam_islands_b4f": (2, 2, 5.5, 9.5), "viridian_forest": (4 / 3, 2, 24.67, 19.0)}


def room_own():
    """Each room's own people and item balls (their keys), from the mod's room files."""
    out = defaultdict(set)
    idir = os.path.join(DATA, "..", "interiors")
    for fn in os.listdir(idir):
        if not fn.endswith(".json") or fn == "prop_models.json": continue
        J = json.load(open(os.path.join(idir, fn), encoding="utf-8"))
        rooms = J.get("rooms", J) if isinstance(J, dict) else {}
        for rid, r in rooms.items():
            if not isinstance(r, dict): continue
            for k in ("people", "items"):
                for o in r.get(k, []) or []:
                    if isinstance(o, dict) and "key" in o: out[rid].add(o["key"])
    return out


def norm(title):
    """A trainer's kind, as Red and Blue's and FireRed's both write it (without a name)."""
    t = re.sub(r"[♂♀]", "", title.upper()).replace("BLACKBELT", "BLACK BELT").replace("POKEMANIAC", "POKÉMANIAC").strip()
    for k in ("COOLTRAINER", "JR.TRAINER", "TEAM ROCKET GRUNT", "ROCKET", "SUPER NERD", "BUG CATCHER", "BLACK BELT", "POKÉMANIAC", "HIKER",
              "PICNICKER", "CAMPER", "LASS", "YOUNGSTER", "JUGGLER", "TAMER", "BURGLAR", "SCIENTIST", "ENGINEER", "SWIMMER", "FISHERMAN"):
        if t.startswith(k): return "ROCKET" if "ROCKET" in k else k
    return t


def main():
    rows, new_trainers = [], []
    used_keys = Counter()
    field_by = defaultdict(list)
    for r in FIELD:
        if len(r) >= 3: field_by[(r[1], r[2])].append(r[0])
    taken = set()
    used_trainer = set()
    seen_tid = Counter()
    skipped = Counter()
    for name, place in sorted(MAPS.items()):
        if place not in PLACES:
            skipped["no place " + place] += 1
            continue
        path = os.path.join(FR, "data", "maps", name, "map.json")
        if not os.path.exists(path): continue
        m = json.load(open(path))
        for o in m.get("object_events", []):
            gfx, script = o.get("graphics_id", ""), o.get("script", "")
            gx, gy = o.get("x", 0), o.get("y", 0)
            if gfx == "OBJ_EVENT_GFX_PUSHABLE_BOULDER":
                # (STRENGTH's boulders: where FireRed starts each one.)
                used_keys[place + "_boulder"] += 1
                key = "%s_boulder_%d" % (place, used_keys[place + "_boulder"])
                rows.append([key, place, "boulder", key, "BOULDER", "-", gx, gy, "none", 0])
                continue
            if gfx == "OBJ_EVENT_GFX_ITEM_BALL":
                it, n = item_of(script)
                ours = our_item(it) if it else None
                if not ours:
                    skipped["item " + str(it)] += 1
                    continue
                have = [k for k in field_by.get((place, ours), []) if k not in taken]
                key = have[0] if have else None
                if key: taken.add(key)
                else:
                    base = "%s_fr_%s" % (place, ours)
                    used_keys[base] += 1
                    key = base if used_keys[base] == 1 else "%s_%d" % (base, used_keys[base])
                rows.append([key, place, "item", key, item_label(ours), "item=" + ours, gx, gy, "none", 0])
                continue
            if place == "route_24" and script == "Route24_EventScript_Rocket":
                # (NUGGET BRIDGE's ROCKET: a scripted battle, the mod's route_24_0, who gives the NUGGET first.)
                used_trainer.add("route_24_0")
                rows.append(["route_24_0", place, "trainer", "route_24_0", "ROCKET", "EKANS 15, ZUBAT 15", gx, gy, facing_of(o.get("movement_type", "")), 0])
                continue
            if o.get("trainer_type", "TRAINER_TYPE_NONE") == "TRAINER_TYPE_NONE": continue
            battle = next((l for l in SCRIPTS.get(script, []) if l.startswith("trainerbattle")), None)
            if not battle:
                skipped["trainer script " + script] += 1
                continue
            parts = [p.strip() for p in battle.split(None, 1)[1].split(",")]
            tid = parts[0]
            cls, tname, party = TR.get(tid, ("", "", ""))
            title = (CLASS_NAME.get(cls, cls.replace("_", " ")) + " " + tname).strip()
            sight = int(o.get("trainer_sight_or_berry_tree_id", "0") or 0)
            facing = facing_of(o.get("movement_type", ""))
            mons = [(s, l) for s, l in PARTY.get(party, []) if s in SPECIES]
            # The mod's own trainer, where it's the same one (same place, same name).
            key = None
            names = [n.strip().upper() for n in tname.split("&")] if tname else []
            pair = seen_tid[tid]
            seen_tid[tid] += 1
            want = names[min(pair, len(names) - 1)] if names else ""
            for r in TRAINERS:
                if r[1] == place and r[0] not in used_trainer and want and r[2].split(" ")[-1].upper() == want:
                    key = r[0]
                    break
            if key is None and place not in OUTSIDE:
                # (In a cave, FireRed's and the mod's are the same trainers, in Red and Blue's way without names: the
                # first of the same kind not matched yet.)
                want_cls = norm(CLASS_NAME.get(cls, cls.replace("_", " ")))
                sex = "\u2642" if gfx.endswith("_M") else "\u2640" if gfx.endswith("_F") else ""
                for strict in (True, False):
                    for r in TRAINERS:
                        if r[1] == place and r[0] not in used_trainer and norm(r[2]) == want_cls and (not strict or not sex or sex in r[2]):
                            key = r[0]
                            break
                    if key: break
            if key is None and pair > 0:
                # (The other half of a pair FireRed battles together: the same battle, so only one of them is listed.)
                continue
            if key is None:
                key = "%s_fr_%s" % (place, re.sub(r"\W+", "_", (cls + "_" + tname).lower()).strip("_"))
                intro = say(parts[1]) if len(parts) > 1 else []
                defeat = say(parts[2]) if len(parts) > 2 else []
                post = []
                for l in SCRIPTS.get(script, []):
                    mm = re.match(r"msgbox (\w+)", l)
                    if mm and "PostBattle" in mm.group(1):
                        post = say(mm.group(1))
                        break
                ccls = CLASS_NAME.get(cls, cls.replace("_", " "))
                new_trainers.append([key, place, title, SPRITE.get(ccls, "youngster"), facing, str(sight), MONEY.get(ccls, "20"),
                                     ",".join("%s:%d" % ml for ml in mons) or "rattata:5", "-", " | ".join(intro) or "-", " | ".join(defeat) or "-",
                                     " | ".join(post) or "-", str(gx), str(gy)])
            used_trainer.add(key)
            team = ", ".join("%s %d" % (s.upper(), l) for s, l in mons)
            rows.append([key, place, "trainer", key, title, team, gx, gy, facing, sight])
        for c in m.get("coord_events", []):
            if "FloorSwitch" not in c.get("script", ""): continue
            used_keys[place + "_switch"] += 1
            key = "%s_switch_%d" % (place, used_keys[place + "_switch"])
            rows.append([key, place, "switch", key, "FLOOR SWITCH", "-", c.get("x", 0), c.get("y", 0), "none", 0])
            # The rock barrier it opens: the two tiles FireRed's switch turns to floor.
            tiles = []
            for l in SCRIPTS.get(c["script"], []):
                mm = re.match(r"setmetatile (\d+), (\d+),", l)
                if mm: tiles.append((int(mm.group(1)), int(mm.group(2))))
            for i, (tx, ty) in enumerate(tiles, 1):
                rows.append(["%s_b%d" % (key, i), place, "barrier", "%s_b%d" % (key, i), "ROCK BARRIER", "opened by " + key, tx, ty, "none", 0])
        for b in m.get("bg_events", []):
            if b.get("type") != "hidden_item": continue
            ours = our_item(b.get("item", ""))
            if not ours:
                skipped["hidden " + str(b.get("item"))] += 1
                continue
            base = "%s_hidden_%s" % (place, ours)
            used_keys[base] += 1
            key = base if used_keys[base] == 1 else "%s_%d" % (base, used_keys[base])
            rows.append([key, place, "hidden", key, item_label(ours), "item=" + ours, b.get("x", 0), b.get("y", 0), "none", 0])
    # The mod's own people outside (people.tsv), for those not standing yet past Viridian, and the field items it has
    # outside that FireRed's don't match (Red and Blue's, at a guessed spot now), so they can be placed or taken away.
    outside = {pl for pl in MAPS.values() if not re.match(r"^(viridian_forest|mt_moon|rock_tunnel|seafoam|cerulean_cave|victory_road|safari|power_plant|digletts|underground)", pl)}
    for r in table("people.tsv"):
        if len(r) < 6 or r[1] not in outside or r[2] == "monster": continue
        if r[0] in ("pallet_town_p0", "cerulean_city_p0", "cerulean_city_p8"): continue
        first = r[5].split(" | ")[0][:60]
        rows.append([r[0], r[1], "person", r[0], r[2].replace("_", " ").upper(), first, r[6] if len(r) > 6 else 0, r[7] if len(r) > 7 else 0, r[3], 0])
    for r in FIELD:
        if len(r) < 5 or r[1] not in outside or r[0] in taken: continue
        rows.append([r[0], r[1], "item", r[0], item_label(r[2]), "item=%s (Red and Blue's)" % r[2], r[3], r[4], "none", 0])
    with open(os.path.join(DATA, "placeables.tsv"), "w", encoding="utf-8") as f:
        f.write("# What the PLACER lists (tools/fr_placeables.py, from FireRed's maps): id place kind key label detail gx gy facing sight\n")
        for r in rows: f.write("\t".join(str(v) for v in r) + "\n")
    # The caves whose floors lay FireRed's map out step for step (their trainers and item balls stand on FireRed's grid:
    # blocks a step, and how far in from the room's corner, worked out from them): what FireRed has there that the room
    # hadn't (its hidden items, the items and trainers it was missing, VICTORY ROAD's boulders, switches and barriers)
    # goes down on the same grid to start with, at the floor's height there (NaN: found in the game).
    own = room_own()
    with open(os.path.join(DATA, "placed.tsv"), "w", encoding="utf-8") as f:
        f.write("# Placed in the mod (tools/fr_placeables.py; John's own go in config/retromon_placed.tsv): key kind place room x y z yaw state\n")
        baked = Counter()
        for r in rows:
            g = GRIDS.get(r[1])
            if not g or r[3] in own.get(r[1], ()): continue
            if r[2] in ("boulder", "switch", "barrier") and not r[1].startswith("victory_road"): continue
            if r[2] == "person": continue
            x, z = g[0] * int(r[6]) + g[2], g[1] * int(r[7]) + g[3]
            yaw = {"up": 180, "down": 0, "left": 90, "right": 270}.get(r[8], 0)
            f.write("\t".join([r[0], r[2], r[1], r[1], "%.2f" % x, "NaN", "%.2f" % z, str(yaw), "placed"]) + "\n")
            baked[r[2]] += 1
    print("baked", dict(baked))
    with open(os.path.join(DATA, "fr_trainers.tsv"), "w", encoding="utf-8") as f:
        f.write("# FireRed's trainers the mod hadn't (tools/fr_placeables.py): trainers.tsv's columns\n")
        for r in new_trainers: f.write("\t".join(r) + "\n")
    c = Counter(r[2] for r in rows)
    print(len(rows), dict(c), "new trainers", len(new_trainers))
    print("skipped", dict(skipped))


if __name__ == "__main__":
    main()
