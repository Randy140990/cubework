#!/usr/bin/env python3
"""
(0.71, John's 27 Nov 18:56: "build a spreadsheet which takes me on a linear Kanto pathway ... so I can basically check
it off ... Gyms need to operate the right way") The Kanto playthrough checklist: FireRed's Kanto in the order you play
it, place by place, every row something to do and what should happen, with what Retromon has of it now, a tick box,
whether it worked, and notes. Written as an .xlsx for the Sheets artifact to bring in.

From the mod's own data: the story steps (tools/kanto_sheet_rows.json, the Kanto Campaign Events sheet, with 0.71's
statuses), every trainer (trainers.tsv), every item ball (field_items.tsv), and which of them the mod has standing
now (the rooms' people and balls, and the few outside).

    python3 tools/kanto_checklist.py <out.xlsx>
"""
import glob
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
ROOMS = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "interiors")


def table(name):
    out = []
    for l in open(os.path.join(DATA, name), encoding="utf-8"):
        if l.startswith("#") or not l.strip(): continue
        out.append(l.rstrip("\n").split("\t"))
    return out


SPECIES = {r[1]: r[2] for r in table("species.tsv")}
ITEMS = {r[0]: r[1] for r in table("items.tsv")}
MOVES = {}
for r in table("moves.tsv"):
    MOVES[r[0]] = r[1] if len(r) > 1 else r[0]
for r in table("tms.tsv"):
    mv = MOVES.get(r[2], r[2].upper().replace("_", " "))
    ITEMS[r[0]] = "%s %s" % (r[3] if r[3] != "-" else r[4], mv)


def item_name(i):
    return ITEMS.get(i, i.upper().replace("_", " "))


def mon_name(s):
    return SPECIES.get(s, s.upper())


TRAINERS = [r for r in table("trainers.tsv") if len(r) >= 9]
FIELD = [r for r in table("field_items.tsv") if len(r) >= 3]

# What the mod has standing now: the rooms' people and item balls, and outside only Pallet, Route 1 and Viridian.
IN_ROOMS, BALLS, BALL_ORDER = set(), set(), []
for f in glob.glob(os.path.join(ROOMS, "*.json")):
    if "prop_models" in f: continue
    try:
        C = json.load(open(f))
    except Exception:
        continue
    for rid, r in C.get("rooms", {}).items():
        for p in r.get("people", []): IN_ROOMS.add(p["key"])
        for b in r.get("items", []) or []:
            if isinstance(b, dict):
                BALLS.add(b.get("key"))
                BALL_ORDER.append(b.get("key"))
# (Taken away again where they lie: not in FireRed.)
GONE = {"route_2_i1"}
OUTSIDE_NOW = {"pallet_town", "route_1", "viridian_city"}
ITEM_SPOTS = {"route_2_parlyz_heal", "viridian_city_potion", "route_2_i0", "route_3_oran_berry", "route_4_great_ball", "route_4_persim_berry",
              "route_4_razz_berry", "route_4_i0"}


def outdoors(pl):
    return pl.endswith("_city") or pl.endswith("_town") or pl.endswith("_island") or (pl.startswith("route_") and "gate" not in pl and "house" not in pl)


FACING = {"up": "north", "down": "south", "left": "west", "right": "east", "none": "round"}


def trainer_rows(place):
    rows = []
    for r in TRAINERS:
        if r[1] != place or r[8] not in ("-", "") or r[2].startswith("RIVAL"): continue
        key, title, facing, sight = r[0], r[2], r[4], int(r[5] or 0)
        team = ", ".join("%s %s" % (mon_name(m.split(":")[0]), m.split(":")[1]) for m in r[7].split(",") if ":" in m)
        sees = ("Sees you %d step%s ahead, facing %s" % (sight, "" if sight == 1 else "s", FACING.get(facing, facing))) if sight > 0 else "Only battles if you talk to them"
        if place == "ss_anne_bow": now = "Left out"
        elif outdoors(place) and place not in OUTSIDE_NOW: now = "Not placed yet"
        else: now = "In" if key in IN_ROOMS or place in OUTSIDE_NOW else "Not placed yet"
        rows.append(("Trainer", "Battle %s" % title, "%s. Team: %s." % (sees, team), now, None))
    return rows


FIELD_ITEM = {r[0]: r[2] for r in FIELD}


def item_rows(place):
    """Outside: the games' item balls for the place (only a few are put down yet). Inside: the room's own balls, as
    the mod lays them (FireRed's), each place's by its key."""
    rows = []
    if outdoors(place):
        for r in FIELD:
            if r[1] != place or r[0] in GONE: continue
            rows.append(("Item", "Pick up the %s" % item_name(r[2]), "In a POKé BALL on the ground; once taken it stays gone.",
                         "In" if r[0] in ITEM_SPOTS else "Not placed yet", None))
        return rows
    for k in BALL_ORDER:
        if not k.startswith(place + "_"): continue
        it = FIELD_ITEM.get(k) or k.split("_fr_")[-1]
        rows.append(("Item", "Pick up the %s" % item_name(it), "In a POKé BALL on the floor; once taken it stays gone.", "In", None))
    return rows


STEPS = {int(r[0]): r for r in json.load(open(os.path.join(HERE, "kanto_sheet_rows.json")))}
# 0.71's statuses, as the sheet has them now.
NOW71 = {
    8: ("In", "MUM brings you the TOWN MAP outside the lab (Let's Go's way)."), 9: ("In", None), 10: ("In", None), 12: ("In", None),
    14: ("Partly", "Place a tree by the side door and it can be CUT."), 16: ("In", None), 19: ("Not placed yet", "The two BLACK BELTs stand outside."),
    21: ("In", None), 22: ("Not placed yet", "The five and the ROCKET stand outside."), 23: ("In", None),
    24: ("Not placed yet", "The ROCKET stands outside the back door."), 25: ("In", None), 35: ("In", None),
    36: ("Partly", "Place the tree in the way and it can be CUT."), 38: ("Partly", "Place the tree and it can be CUT."), 39: ("In", None), 44: ("In", None),
    47: ("In", None), 51: ("In", None), 53: ("In", None), 54: ("In", None), 57: ("Partly", "Place the tree in the way and it can be CUT."), 63: ("In", None),
    64: ("In", None), 67: ("In", None), 68: ("In", None), 72: ("In", None), 73: ("Partly", "Boulders can't be pushed yet."), 75: ("In", None), 76: ("In", None),
    79: ("In", None), 80: ("In", None), 82: ("Partly", "Boulders can't be pushed yet."), 86: ("In", None)}

# What to check, step by step, where it says more than the sheet's line.
CHECK = {
    1: "You wake in your room upstairs. MUM is downstairs; the PC on your desk works.",
    2: "OAK calls out as you step into the grass at the north edge, and walks you to his lab.",
    3: "Three POKé BALLs on the table; you can only take one. Your rival takes the one strong against yours.",
    4: "No battle yet: in Retromon your rival battles you when you bring OAK's PARCEL back (Let's Go's way).",
    5: "The clerk on Route 1 gives you a POTION, once.",
    6: "The clerk calls you over in the Viridian MART and gives you OAK's PARCEL. The MART sells nothing yet. The old man lies across the road north.",
    7: "Your rival battles you in the lab (lose and nothing's lost). OAK gives you each a POKéDEX, and you five POKé BALLs. The MART now sells.",
    8: "MUM meets you outside the lab with the TOWN MAP.",
    9: "He's up now: his catching lesson, then the TEACHY TV. The way north is open.",
    10: "JESSIE and JAMES wait by the sign as you come up from Route 1; JESSIE walks over and battles you (EKANS and KOFFING).",
    11: "Your rival comes along the top path from the LEAGUE's gate and battles you. The gate's guard won't let you through without the BOULDERBADGE.",
    12: "The GYM's doors are locked: you're turned back with \"It looks like the GYM LEADER isn't here.\"",
    13: "Through the south gate, the forest (its BUG CATCHERs, PIKACHU in the grass) and the north gate to Pewter.",
    14: "Pay ¥50 at the desk. The scientist by the side door gives you the OLD AMBER.",
    15: "Try to leave Pewter east before beating BROCK: a boy stops you and walks you to the GYM.",
    17: "Leaving east with the BOULDERBADGE, OAK's AIDE catches you up with the RUNNING SHOES.",
    18: "The man in the Route 4 POKéMON CENTER sells you a MAGIKARP for ¥500, once.",
    19: "Two BLACK BELTs teach MEGA PUNCH and MEGA KICK, once each.",
    20: "JESSIE and JAMES battle you on B2F (EKANS and KOFFING, Lv 12). SUPER NERD MIGUEL guards the fossils: beat him and take one; the other's his.",
    21: "At the foot of NUGGET BRIDGE your rival comes down off it and battles you (PIDGEOTTO 18, ABRA 15, RATTATA 15, his starter 17).",
    22: "Five in a row on the bridge, then the ROCKET: the NUGGET, his offer to join, then his battle.",
    23: "The CLEFAIRY says it's BILL and asks for help. Run the CELL SEPARATOR on his PC: BILL's himself again, and gives you the S.S. TICKET.",
    24: "Round the back of the robbed house, the ROCKET battles you, then gives back TM28 DIG.",
    26: "Each of Saffron's four gates: the guard's thirsty and won't let you by.",
    27: "Leave a POKéMON with the DAY CARE man; it grows as you walk. Take it back for a fee.",
    28: "Down the stairs on Route 5 and along to Route 6.",
    29: "Hear the CHAIRMAN out: he gives you the BIKE VOUCHER.",
    30: "The voucher for a BICYCLE. Riding it, you go faster; the CYCLING ROAD's gates let you through.",
    31: "The FISHING GURU gives you the OLD ROD.",
    32: "Without the S.S. TICKET the sailor stops you at the gangway; with it, you go aboard.",
    33: "Your rival on 2F (PIDGEOTTO, RATICATE, KADABRA, his starter's second stage).",
    34: "The CAPTAIN gives you HM01 CUT.",
    35: "Step off in Vermilion: \"The S.S.ANNE has set sail!\" The sailor won't let you back up.",
    37: "The AIDE in Route 2's east gate gives you HM05 FLASH once you've caught 10 kinds.",
    38: "East of Cerulean, the tree you CUT, then Route 9 and the Route 10 CENTER.",
    39: "It's pitch dark but for a little circle round you. With FLASH (and the BOULDERBADGE) you're asked as you go in, and it lights the tunnel.",
    40: "Your rival on 2F (PIDGEOTTO 25, two of GYARADOS, GROWLITHE, EXEGGCUTE, KADABRA 20, his starter 25).",
    41: "The CHANNELERS on 3F to 5F.",
    42: "Without the SILPH SCOPE: \"Be gone... Intruders...\", a GHOST you can only run from, and no way up.",
    43: "Under Saffron, from Route 8 to Route 7.",
    44: "The old lady on 1F gives you TEA (any drink from the Dept. Store roof works too).",
    45: "The EEVEE in a POKé BALL on the table in the roof house.",
    46: "Its floors' clerks, the lift, the roof's machines and the thirsty girl (a drink for a TM).",
    47: "The man in the DINER gives you the COIN CASE. The clerk sells 50 coins for ¥1000. The slots: bet 1 to 3, A spins and stops each reel. The prize corner next door: POKéMON, TMs and items for coins.",
    48: "The switch behind the poster (past the ROCKET by it): the stairs down open.",
    49: "The arrow tiles carry you; the ROCKET on B4F drops the LIFT KEY; the lift takes you down.",
    50: "JESSIE and JAMES first (ARBOK and WEEZING, Lv 32), then GIOVANNI. He leaves the SILPH SCOPE.",
    52: "With the SILPH SCOPE: it's MAROWAK. Beat it and it's laid to rest; the stairs up open.",
    53: "JESSIE and JAMES (Lv 34) and three ROCKETs, then MR. FUJI: he takes you home.",
    54: "At home, MR. FUJI gives you the POKé FLUTE.",
    55: "A SNORLAX asleep across Route 12, and another on Route 16. Play the POKé FLUTE: it wakes and battles you.",
    56: "The FISHING GURU's brother gives you the SUPER ROD.",
    57: "The girl in the house by the gate gives you HM02 FLY. FLY works outside battle with the THUNDERBADGE.",
    58: "The gates only let you by on your BICYCLE. On Route 17, with seven badges, JESSIE and JAMES (Lv 52).",
    59: "The FISHING GURU's brother in Fuchsia gives you the GOOD ROD.",
    60: "Pay ¥500 for 30 SAFARI BALLs and 600 steps. HM03 SURF in the SECRET HOUSE; the GOLD TEETH lie in the west area.",
    61: "The WARDEN takes his GOLD TEETH and gives you HM04 STRENGTH.",
    63: "The AIDE on 2F gives you the ITEMFINDER once you've caught 30 kinds.",
    64: "The AIDE on 2F gives you the EXP.SHARE once you've caught 50 kinds.",
    65: "With TEA (or any drink), the guard lets you into Saffron.",
    66: "The CARD KEY on 5F opens the shutters.",
    67: "Your rival by the warp pads on 7F (PIDGEOT 37, ALAKAZAM 35, his starter's last stage 40). The worker gives you LAPRAS.",
    68: "JESSIE and JAMES (Lv 36), then GIOVANNI on 11F. The PRESIDENT gives you the MASTER BALL.",
    69: "The KARATE MASTER and his pupils; then HITMONLEE or HITMONCHAN.",
    70: "MR. PSYCHIC gives you TM29 PSYCHIC.",
    71: "Bring the COPYCAT a POKé DOLL for TM31 MIMIC.",
    73: "SURF south from Fuchsia. ARTICUNO deep in the SEAFOAM ISLANDS.",
    74: "SURF to the POWER PLANT; ZAPDOS inside.",
    75: "The statues' switches open and shut the gates. The SECRET KEY on B1F.",
    77: "Bring a fossil to the LAB: it comes back to life.",
    78: "Left out: the Sevii Islands aren't on your map.",
    80: "With all eight badges your rival comes along the top path again (PIDGEOT 47, RHYHORN 45, ALAKAZAM 50, his starter's last stage 53).",
    81: "Each guard checks his badge as you go north.",
    82: "VICTORY ROAD's three floors, its trainers, MOLTRES on 2F.",
    83: "LORELEI, BRUNO, AGATHA, LANCE in turn; each door opens once you've won.",
    84: "Your rival, the CHAMPION (six POKéMON, his starter's last stage 63). Win and the title's yours.",
    85: "OAK, and your team goes into the HALL OF FAME.",
    86: "Before you're CHAMPION you're turned back at its mouth. MEWTWO on B1F.",
    87: "Left out: the Sevii Islands aren't on your map.",
}

GYMS = {
    "pewter": ("pewter_gym", 16, "BOULDERBADGE", "tm_bide", "It lets FLASH light ROCK TUNNEL.",
               "Up past the CAMPER to BROCK at the top."),
    "cerulean": ("cerulean_gym", 25, "CASCADEBADGE", "tm_bubblebeam", "CUT works outside battle with it.",
                 "Round the pool to MISTY, past her trainers."),
    "vermilion": ("vermilion_gym", 36, "THUNDERBADGE", "tm_thunderbolt", "FLY works outside battle with it.",
                  "Search the trash cans: the first switch, then the second right next to it (get it wrong and both reset). Then the electric door opens."),
    "celadon": ("celadon_gym", 51, "RAINBOWBADGE", "tm_mega_drain", "-",
                "Through the garden, past ERIKA's trainers."),
    "fuchsia": ("fuchsia_gym", 62, "SOULBADGE", "tm_toxic", "SURF works outside battle with it.",
                "Invisible walls: you bump into them, and find the way through to KOGA."),
    "saffron": ("saffron_gym", 72, "MARSHBADGE", "tm_psywave", "-",
                "The warp tiles: each sends you to another room, till you reach SABRINA."),
    "cinnabar": ("cinnabar_gym", 76, "VOLCANOBADGE", "tm_fire_blast", "-",
                 "The quiz machines: answer right and the gate opens; wrong and you battle that room's trainer."),
    "viridian": ("viridian_gym", 79, "EARTHBADGE", "tm_fissure", "The last guard on ROUTE 23 lets you by with it.",
                 "The arrow tiles carry you across the floor; past GIOVANNI's trainers to him."),
}
GUIDES = {"pewter", "cerulean", "vermilion", "fuchsia", "saffron", "cinnabar", "viridian"}


def gym_rows(city):
    room, step, badge, tm, use, puzzle = GYMS[city]
    st = STEPS[step]
    rows = []
    door = {"viridian": "Turned back at the door until you've the other seven badges; then it opens.",
            "cinnabar": "Locked without the SECRET KEY. With it: \"used the SECRET KEY. The door opened!\" the first time."}.get(city, "The door opens and you go in.")
    rows.append(("Gym", "Go in", door, "In", step))
    if city in GUIDES: rows.append(("Gym", "Talk to the GYM GUIDE by the door", "He tells you about the LEADER and the type to bring.", "In", step))
    rows.extend((k, do, check, now, step) for k, do, check, now, _ in trainer_rows(room))
    rows.append(("Gym", "The GYM's way through", puzzle, "In", step))
    leader = [r for r in TRAINERS if r[1] == room and r[8] not in ("-", "")]
    team = ", ".join("%s %s" % (mon_name(m.split(":")[0]), m.split(":")[1]) for m in leader[0][7].split(",") if ":" in m) if leader else "-"
    rows.append(("Gym", "Battle %s" % (leader[0][2] if leader else "the LEADER"), "Team: %s. Lose and you black out to your last POKéMON CENTER." % team, "In", step))
    rows.append(("Gym", "Win", ("You receive the %s, then %s (%s TM). %s" % (badge, item_name(tm), "FireRed's" if city == "cinnabar" else "Red and Blue's",
                                                                           use if use != "-" else "")).strip(), "In", step))
    return rows


def step_row(n, kind=None):
    st = STEPS[n]
    now, note = NOW71.get(n, (st[4], st[5]))
    what = CHECK.get(n, st[3])
    if note and now != "In": what = what + " (" + note + ")"
    k = kind or ("Rival" if n in (7, 11, 21, 33, 40, 67, 80, 84) else "TEAM ROCKET" if n == 10 else "Story")
    if st[4] == "Left out": now = "Left out"
    return (k, st[3] if n not in (4,) else "Rival battle 1", what, now, n)


# The order you play it: each leg, and in each, the steps and the places (their trainers and items).
ORDER = [
    ("Start", [("step", 1), ("step", 2), ("step", 3), ("step", 4)]),
    ("To BROCK", [("step", 5), ("place", "route_1"), ("step", 6), ("place", "viridian_city"), ("step", 7), ("step", 8), ("step", 9), ("step", 10),
                  ("step", 11), ("place", "route_22"), ("step", 12), ("place", "route_2"), ("step", 13), ("place", "viridian_forest"),
                  ("place", "pewter_city"), ("step", 14), ("step", 15), ("gym", "pewter"), ("step", 17)]),
    ("To MISTY", [("place", "route_3"), ("step", 18), ("place", "mt_moon_1f"), ("place", "mt_moon_b1f"), ("step", 20), ("place", "mt_moon_b2f"),
                  ("place", "route_4"), ("step", 19), ("place", "cerulean_city"), ("step", 21), ("step", 22), ("place", "route_24"), ("place", "route_25"),
                  ("step", 23), ("step", 24), ("gym", "cerulean")]),
    ("To LT. SURGE", [("step", 26), ("step", 27), ("step", 28), ("place", "underground_path_ns"), ("place", "route_6"), ("place", "vermilion_city"),
                      ("step", 29), ("step", 30), ("step", 31), ("step", 32), ("place", "ss_anne_1f_rooms"), ("place", "ss_anne_b1f_rooms"),
                      ("place", "ss_anne_b1f_room3"), ("place", "ss_anne_b1f_room5"), ("place", "ss_anne_kitchen"), ("step", 33), ("place", "ss_anne_2f"),
                      ("place", "ss_anne_2f_rooms"), ("place", "ss_anne_2f_room2"), ("place", "ss_anne_bow"), ("step", 34), ("step", 35),
                      ("gym", "vermilion"), ("place", "route_11"), ("step", 37)]),
    ("To ERIKA", [("place", "route_9"), ("step", 38), ("place", "route_10"), ("step", 39), ("place", "rock_tunnel_1f"), ("place", "rock_tunnel_b1f"),
                  ("step", 40), ("step", 41), ("place", "pokemon_tower_3f"), ("place", "pokemon_tower_4f"), ("place", "pokemon_tower_5f"), ("step", 42),
                  ("place", "route_8"), ("step", 43), ("place", "underground_path_we"), ("step", 44), ("step", 45), ("step", 46), ("step", 47),
                  ("step", 48), ("place", "game_corner"), ("step", 49), ("place", "rocket_hideout_b1f"), ("place", "rocket_hideout_b2f"),
                  ("place", "rocket_hideout_b3f"), ("step", 50), ("place", "rocket_hideout_b4f"), ("gym", "celadon")]),
    ("To KOGA", [("step", 52), ("place", "pokemon_tower_6f"), ("step", 53), ("place", "pokemon_tower_7f"), ("step", 54), ("step", 55),
                 ("place", "route_12"), ("step", 56), ("place", "route_13"), ("place", "route_14"), ("place", "route_15"), ("place", "route_16"),
                 ("step", 57), ("step", 58), ("place", "route_17"), ("place", "route_18"), ("step", 59), ("step", 60), ("place", "safari_zone_center"),
                 ("place", "safari_zone_east"), ("place", "safari_zone_north"), ("place", "safari_zone_west"), ("step", 61), ("place", "wardens_house"),
                 ("gym", "fuchsia"), ("step", 63), ("step", 64)]),
    ("To SABRINA", [("step", 65), ("step", 66)] + [("place", "silph_co_%df" % i) for i in range(2, 7)] + [("step", 67), ("place", "silph_co_7f")]
     + [("place", "silph_co_%df" % i) for i in range(8, 11)] + [("step", 68), ("place", "silph_co_11f"), ("step", 69), ("place", "fighting_dojo"),
                                                                  ("step", 70), ("step", 71), ("gym", "saffron")]),
    ("To BLAINE", [("place", "route_19"), ("place", "route_20"), ("step", 73), ("place", "seafoam_islands_1f"), ("place", "seafoam_islands_b1f"),
                   ("place", "seafoam_islands_b2f"), ("place", "seafoam_islands_b3f"), ("place", "seafoam_islands_b4f"), ("step", 74), ("place", "power_plant"),
                   ("step", 75), ("place", "pokemon_mansion_1f"), ("place", "pokemon_mansion_2f"), ("place", "pokemon_mansion_3f"),
                   ("place", "pokemon_mansion_b1f"), ("gym", "cinnabar"), ("step", 77), ("step", 78), ("place", "route_21")]),
    ("To GIOVANNI", [("gym", "viridian")]),
    ("The LEAGUE", [("step", 80), ("step", 81), ("step", 82), ("place", "victory_road_1f"), ("place", "victory_road_2f"), ("place", "victory_road_3f"),
                    ("step", 83), ("place", "loreleis_room"), ("place", "brunos_room"), ("place", "agathas_room"), ("place", "lances_room"), ("step", 84),
                    ("step", 85)]),
    ("After the LEAGUE", [("step", 86), ("place", "cerulean_cave_1f"), ("place", "cerulean_cave_2f"), ("place", "cerulean_cave_b1f"), ("step", 87)]),
]

PLACE_NAME = {}
for r in table("places.tsv"):
    if len(r) > 2: PLACE_NAME[r[0]] = r[2]


def where(pl):
    import re
    fl = re.search(r"_(b?\d+f)$", pl)
    if pl in PLACE_NAME: return PLACE_NAME[pl] + (" " + fl.group(1).upper() if fl and not PLACE_NAME[pl].upper().endswith(fl.group(1).upper()) else "")
    return pl.replace("_", " ").upper().replace("POKEMON", "POKéMON").replace("SS ANNE", "S.S. ANNE").replace(" 1F", " 1F")


def build():
    rows = []
    last_step = 1
    for leg, items in ORDER:
        for kind, v in items:
            if kind == "step":
                st = STEPS[v]
                k, do, check, now, n = step_row(v)
                rows.append((leg, st[2], k, do, check, now, n))
                last_step = v
            elif kind == "gym":
                st = STEPS[GYMS[v][1]]
                for k, do, check, now, n in gym_rows(v):
                    rows.append((leg, st[2], k, do, check, now, n))
                last_step = GYMS[v][1]
            else:
                for k, do, check, now, n in trainer_rows(v) + item_rows(v):
                    rows.append((leg, where(v), k, do, check, now, last_step))
    return rows


def main():
    import openpyxl
    from openpyxl.styles import Alignment, Font, PatternFill
    rows = build()
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Playthrough"
    n = len(rows)
    first, last = 6, 5 + n
    # Above the table: what it is, and how far you've got.
    ws.append(["Kanto in FireRed's order, place by place. Tick DONE as you check each row; set WORKS? and add notes where it doesn't."])
    ws.append(["Done", "=COUNTIF(I%d:I%d,TRUE)" % (first, last), "of", n, "Works", '=COUNTIF(J%d:J%d,"Works")' % (first, last), "Wrong",
               '=COUNTIF(J%d:J%d,"Wrong")' % (first, last), "Missing", '=COUNTIF(J%d:J%d,"Missing")' % (first, last)])
    ws.append(["Retromon now is read from the mod's code at 0.71. Picker step: the EVENT PICKER's step (OPTION) to jump there."])
    ws.append([])
    ws.append(["#", "Leg", "Where", "Kind", "What happens", "Check that", "Retromon now", "Picker step", "Done", "Works?", "Your notes"])
    for i, (leg, wh, k, do, check, now, st) in enumerate(rows, 1):
        ws.append([i, leg, wh, k, do, check, now, st, False, None, None])
    for c in ws[5]:
        c.font = Font(bold=True)
        c.fill = PatternFill("solid", fgColor="F3F3F3")
    ws["A1"].font = Font(bold=True, size=12)
    for c in ws[2]: c.font = Font(bold=True)
    for row in ws.iter_rows(min_row=first):
        for c in (row[4], row[5], row[10]): c.alignment = Alignment(wrap_text=True, vertical="top")
    ws.freeze_panes = "A6"
    wb.save(sys.argv[1])
    kinds, nows = {}, {}
    for r in rows:
        kinds[r[2]] = kinds.get(r[2], 0) + 1
        nows[r[5]] = nows.get(r[5], 0) + 1
    print(n, "rows", kinds, nows)

if __name__ == "__main__":
    main()
