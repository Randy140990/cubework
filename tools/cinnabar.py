#!/usr/bin/env python3
"""
CINNABAR ISLAND's insides, laid out from FireRed's own maps (pret/pokefirered: data/layouts, data/maps), a FireRed tile
a block, for the Interior Viewer (tools/interior/cinnabar_data.js, built by tools/interior/rooms62.js) and so the mod:

  cinnabar_gym                  BLAINE's GYM: its rooms behind green walls, the quiz machines and their gates, the
                                long purple pipe down the middle, BLAINE's rug, the statues by the door
  cinnabar_lab                  the POKéMON LAB's hall: the reception, the glass table, the three doors along it
  cinnabar_lab_trade_room       the MEETING ROOM (FireRed's LOUNGE): the yellow table on its blue rug, the sofas
  cinnabar_lab_metronome_room   the R&D ROOM (FireRed's RESEARCH ROOM): the desks with their PCs, the shelves
  cinnabar_lab_fossil_room      the TESTING ROOM (FireRed's EXPERIMENT ROOM): the machines, the revival machine
  pokemon_mansion_1f..b1f       the POKéMON MANSION's four floors: its pink brick walls, the rubble, the pillars,
                                the statues (the secret switch) and their barred gates, the stairs, 3F's holes

Each floor's tiles are sorted by FireRed's tile (its picture, and whether you can walk on it). Its warps, signs,
people, trainers and items are FireRed's, matched to the mod's (people.tsv, trainers.tsv and field_items.tsv, written
from the first games) room by room; any FireRed has that the mod hasn't are added. The gates (the GYM's, opened by a
right answer or by beating the room's trainer; the MANSION's, swapped by its statues) are listed with the floor: the
mod puts them in as real blocks, as LT.SURGE's door (game/Game.java).

    python3 -I tools/cinnabar.py
"""
import collections, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import silph  # noqa: E402
import celadon  # noqa: E402

DATA = silph.DATA
OUT_JS = os.path.join(HERE, "interior", "cinnabar_data.js")
ids = celadon.ids

MAPS = [("CinnabarIsland_Gym", "cinnabar_gym", "Cinnabar Gym", ""),
        ("CinnabarIsland_PokemonLab_Entrance", "cinnabar_lab", "Pokémon Lab", "Hall"),
        ("CinnabarIsland_PokemonLab_Lounge", "cinnabar_lab_trade_room", "Pokémon Lab", "Meeting Room"),
        ("CinnabarIsland_PokemonLab_ResearchRoom", "cinnabar_lab_metronome_room", "Pokémon Lab", "R&D Room"),
        ("CinnabarIsland_PokemonLab_ExperimentRoom", "cinnabar_lab_fossil_room", "Pokémon Lab", "Testing Room"),
        ("PokemonMansion_1F", "pokemon_mansion_1f", "Pokémon Mansion", "1F"), ("PokemonMansion_2F", "pokemon_mansion_2f", "Pokémon Mansion", "2F"),
        ("PokemonMansion_3F", "pokemon_mansion_3f", "Pokémon Mansion", "3F"), ("PokemonMansion_B1F", "pokemon_mansion_b1f", "Pokémon Mansion", "B1F")]
ROOM = {m: r for m, r, _, _ in MAPS}
MAP_OF = {"MAP_" + celadon.silph.snake(m).upper().replace("ISLAND__", "ISLAND_"): m for m, _, _, _ in MAPS}
MAP_OF.update({"MAP_CINNABAR_ISLAND_GYM": "CinnabarIsland_Gym", "MAP_CINNABAR_ISLAND_POKEMON_LAB_ENTRANCE": "CinnabarIsland_PokemonLab_Entrance",
               "MAP_CINNABAR_ISLAND_POKEMON_LAB_LOUNGE": "CinnabarIsland_PokemonLab_Lounge",
               "MAP_CINNABAR_ISLAND_POKEMON_LAB_RESEARCH_ROOM": "CinnabarIsland_PokemonLab_ResearchRoom",
               "MAP_CINNABAR_ISLAND_POKEMON_LAB_EXPERIMENT_ROOM": "CinnabarIsland_PokemonLab_ExperimentRoom",
               "MAP_POKEMON_MANSION_1F": "PokemonMansion_1F", "MAP_POKEMON_MANSION_2F": "PokemonMansion_2F", "MAP_POKEMON_MANSION_3F": "PokemonMansion_3F",
               "MAP_POKEMON_MANSION_B1F": "PokemonMansion_B1F"})


def table_of(spec):
    out = {}
    for k, s in spec.items():
        for i in s: out[i] = k
    return out


# ----- BLAINE's GYM (gTileset_CinnabarGym) -----
# w the outer walls, b the green walls between its rooms, Q a quiz machine, T the purple pipe (and its channel), S a
# statue, X the way out; floors: g the orange tiles, r BLAINE's rug, m the mat by the door.
GYM = table_of({"v": ids(0x8), "X": ids(0x174, 0x175, 0x176), "Q": ids(0x290, 0x2a0, 0x298, 0x2a8, 0x2c8, 0x2c9),
                "T": ids((0x2c3, 0x2c5), (0x2cb, 0x2cd), 0x2dc, 0x2dd, (0x2d3, 0x2d7), 0x2de, 0x2df), "S": ids(0x2af, 0x2b7)})
GYM_FL = table_of({"r": ids((0x2a9, 0x2ab), (0x2b1, 0x2b3), (0x2b9, 0x2bb)), "m": ids((0x2ac, 0x2ae))})
# The six gates (FireRed's OpenDoor1..6), each the cells it shuts, and the quiz machine (its two sign spots) that
# opens it, the answer (YES or NO), and the trainer whose beating opens it too (FireRed's: a wrong answer, and they
# come for you).
GYM_GATES = [
    {"n": 1, "cells": [(26, 9), (27, 9)], "quiz": [(22, 10), (23, 10)], "yes": True, "trainer": "QUINN"},
    {"n": 2, "cells": [(17, 9), (18, 9)], "quiz": [(15, 2), (16, 2)], "yes": False, "trainer": "AVERY"},
    {"n": 3, "cells": [(17, 16), (18, 16)], "quiz": [(13, 10), (14, 10)], "yes": False, "trainer": "RAMON"},
    {"n": 4, "cells": [(11, 22), (11, 23)], "quiz": [(13, 17), (14, 17)], "yes": False, "trainer": "DEREK"},
    {"n": 5, "cells": [(5, 17), (6, 17)], "quiz": [(1, 18), (2, 18)], "yes": True, "trainer": "DUSTY"},
    {"n": 6, "cells": [(5, 9), (6, 9)], "quiz": [(1, 10), (2, 10)], "yes": False, "trainer": "ZAC"},
]
QUESTIONS = {1: "CATERPIE evolves into METAPOD?", 2: "There are nine certified POKéMON LEAGUE BADGES?", 3: "POLIWAG evolves three times?",
             4: "Are electric moves effective against GROUND-type POKéMON?", 5: "POKéMON of the same kind and level are not identical?",
             6: "TM28 contains TOMBSTONY?"}
# FireRed's trainers by name (their order in its map) to the mod's (Red and Blue's, trainers.tsv), by where they stand.
GYM_TRAINERS = {"ERIK": "cinnabar_gym_1", "QUINN": "cinnabar_gym_2", "AVERY": "cinnabar_gym_3", "RAMON": "cinnabar_gym_4", "DEREK": "cinnabar_gym_5",
                "DUSTY": "cinnabar_gym_6", "ZAC": "cinnabar_gym_7"}

# ----- The POKéMON LAB (gTileset_Lab, on the Building tiles) -----
# w walls (and their windows and pictures), D a doorway to a room, p a plant, k a counter, table or desk, m a machine
# or PC, F the revival machine, h a shelf, c a crate, o a stool or chair, s a sofa; floors: l the white tiles, b the
# blue rug, t the mat by the door.
LAB = table_of({"v": ids(0x8), "X": ids(0x1a, 0x1b, 0x1c, 0x90, 0x91, 0x92),
                "w": ids((0x68, 0x6e), (0x70, 0x76), 0x314, 0x315, 0x316, 0x31c, 0x31d, 0x31e, 0x324, 0x325, 0x2f9, 0x2fa, 0x2bd, 0x2be, 0x2c6),
                "D": ids(0x2c5), "p": ids(0x2bf, 0x2c7, 0x8c, 0x94, 0x2df, 0x2e7),
                "k": ids(0x2d4, 0x2d5, 0x2d3, 0x2cf, 0x2d6, 0x2d7, 0x2c8, 0x2c9, 0x2d0, 0x2d1, (0x2e0, 0x2e2), (0x2e8, 0x2ea), (0x2f0, 0x2f2), 0x311, 0x312,
                         (0x30d, 0x30f), 0x77, 0x300, 0x7f, 0x308),
                "m": ids(0x6f, 0x8f, 0x62, 0x304, 0x31f, (0x318, 0x31b), (0x320, 0x323), 0x313),
                "F": ids(0x2b8, 0x2b9, 0x2c0, 0x2c1, 0x2dc, 0x2dd), "h": ids(0x301, 0x302, 0x2fb, 0x2fc), "c": ids(0x99),
                "o": ids(0x2ca, 0x2cb, 0x2d8, 0x2f8, (0x305, 0x307), (0x2fd, 0x2ff), 0x30c),
                "s": ids(0x2e5, 0x2de, 0x2e6, 0x2e4, 0x2e3, 0x2eb)})
LAB_FL = table_of({"b": ids((0x2ec, 0x2ef), (0x2f3, 0x2f8)), "t": ids(0x88, 0x89, 0x8a)})
LAB_DOORS = {(13, 5): "cinnabar_lab_trade_room", (19, 5): "cinnabar_lab_metronome_room", (25, 5): "cinnabar_lab_fossil_room"}

# ----- The POKéMON MANSION (gTileset_PokemonMansion) -----
# w its walls (pink brick, cream render, the windows), c a pillar or a pile of rocks, S a statue (the secret switch),
# p a plant, k a table, o a chair, h a bookshelf, m a PC or machine, u the stairs, H a hole (3F's); floors: f the stone
# tiles, n the planks (B1F), q the white tiles, r the red carpet, R the pink runner, d rubble.
MAN = table_of({"v": ids(0x8), "X": ids(0x1a, 0x1b, 0x1c), "c": ids(0x2bd, 0x2c5, 0x2cd, 0x2cb, 0x2cc, 0x37c, 0x2c7),
                "S": ids(0x314, 0x315, 0x316, 0x34d, 0x34e, 0x34f, 0x31c, 0x32c),
                "p": ids(0x30b, 0x313, 0x323, 0x324, 0x359, 0x361, 0x369),
                "k": ids((0x310, 0x312), (0x318, 0x31a), 0x2f3 if False else 0x310), "o": ids(0x2f0, 0x2f1, 0x35a, 0x35b),
                "h": ids(0x375, 0x376, 0x377), "m": ids(0x31d, 0x31e, 0x31f, 0x325, 0x326, 0x327, 0x36c, 0x36d, 0x36e),
                "u": ids(0x2d0, 0x2d1, 0x2d2, 0x2d8, 0x2d9, 0x2da, (0x2e0, 0x2e2), 0x2d5, 0x2d6, 0x2d7, 0x2dd, 0x2de, 0x2df, 0x2e5, 0x2e6, 0x2e7,
                         0x363, 0x36b),
                "H": ids(0x35f, 0x34b, 0x34c)})
MAN_FL = table_of({"n": ids(0x286, 0x287, 0x28e, 0x28f, 0x291, 0x297, 0x29f, 0x2b0, (0x351, 0x353), 0x357, 0x35c, 0x35e, (0x363, 0x366), 0x36b, 0x37d),
                   "q": ids(0x282, 0x283, 0x32f, 0x337, 0x340),
                   "r": ids(0x2e8, 0x2e9, 0x2ea, 0x348, 0x349, 0x34a), "R": ids(0x2b6, 0x2b7, 0x2c6),
                   "d": ids((0x2b8, 0x2bc), 0x2c0, (0x2c2, 0x2c4), (0x2c8, 0x2ca), 0x333, 0x334, (0x33b, 0x33d), 0x33f, 0x343, 0x345, 0x346)})
BARRIER = ids(0x2f8, 0x2f9, 0x2fa, 0x300, 0x301, 0x302, 0x354, 0x355, 0x356, 0x350, 0x358, 0x360, 0x362, 0x368, 0x36a, 0x370, 0x372)
# The MANSION's gates (FireRed's PressSwitch/ResetSwitch): the cells each shuts, and whether it's shut while the switch
# is off (as the MANSION starts) or while it's on. The statue each floor's switch is in (its sign spot, its eyes).
MAN_GATES = {
    "pokemon_mansion_1f": [((22, 24), (10, 11), "off"), ((27, 29), (25, 26), "off"), ((32, 34), (25, 26), "off"), ((31, 33), (18, 19), "on")],
    "pokemon_mansion_2f": [((24, 26), (14, 15), "off"), ((10, 10), (30, 32), "off"), ((12, 12), (6, 8), "on")],
    "pokemon_mansion_3f": [((17, 19), (11, 12), "off"), ((21, 21), (6, 8), "on")],
    "pokemon_mansion_b1f": [((33, 35), (20, 21), "on"), ((16, 16), (28, 30), "on"), ((12, 12), (10, 12), "off"), ((20, 22), (22, 23), "off")],
}
# (The tiles beside a vertical gate's opening, its wall's end, whichever way the switch is: wall.)
MAN_POSTS = {"pokemon_mansion_2f": [(10, 28), (10, 29), (12, 4), (12, 5)], "pokemon_mansion_3f": [(21, 4), (21, 5)],
             "pokemon_mansion_b1f": [(16, 26), (16, 27), (12, 8), (12, 9)]}

GFX_SKIN = {"SCIENTIST": "scientist", "POKE_MANIAC": "super_nerd", "YOUNGSTER": "youngster", "BLAINE": "leader_blaine", "GYM_GUY": "gym_guide",
            "OLD_MAN_2": "gramps", "WOMAN_2": "beauty", "BOY": "boy", "FISHER": "fishing_guru"}
NAME = {"GYM_GUY": "Gym Guide", "OLD_MAN_2": "Old man", "WOMAN_2": "Woman", "BOY": "Boy", "SCIENTIST": "Scientist", "POKE_MANIAC": "Poké Maniac"}
# FireRed's three traders at the LAB (its in-game trades, the first games' too): who, what they want, what they give.
TRADERS = {"CinnabarIsland_PokemonLab_Lounge_EventScript_Clifton": "electrode_trade", "CinnabarIsland_PokemonLab_Lounge_EventScript_Norma": "tangela_trade",
           "CinnabarIsland_PokemonLab_ExperimentRoom_EventScript_Garett": "seel_trade"}


def main():
    people, trainers, items = silph.table("people.tsv"), silph.table("trainers.tsv"), silph.table("field_items.tsv")
    have_people, have_items = {r[0] for r in people}, {r[0] for r in items}
    item_ids = {r[0] for r in silph.table("items.tsv")}
    rooms, new_people, new_items = {}, [], []
    for m, rid, name, fname in MAPS:
        mp, l, w, h, v = celadon.grid(m)
        sec = l["secondary_tileset"]
        coll = lambda b: bool((b >> 10) & 3)
        if sec == "gTileset_CinnabarGym":
            def kind(b, x, z):
                t = b & 0x3FF
                if t in GYM: return GYM[t]
                if not coll(b): return "."
                return "w" if (z <= 1 or x == 0 or x == w - 1) else "b"
            flr = lambda b: GYM_FL.get(b & 0x3FF, "g")
        elif sec == "gTileset_Lab":
            def kind(b, x, z):
                t = b & 0x3FF
                if t in (0x73, 0x74) and z >= 3: return "h"
                if t in LAB: return LAB[t]
                return "w" if coll(b) else "."
            flr = lambda b: LAB_FL.get(b & 0x3FF, "l")
        else:
            def kind(b, x, z):
                t = b & 0x3FF
                if t in BARRIER: return "w"
                if t in MAN: return MAN[t]
                return "w" if coll(b) else "."
            flr = lambda b: MAN_FL.get(b & 0x3FF, "f")
        rows = [[kind(v[z * w + x], x, z) for x in range(w)] for z in range(h)]
        fl = [[flr(v[z * w + x]) for x in range(w)] for z in range(h)]
        # The gates: their cells floor (the mod puts the gate in, as blocks); a vertical gate's posts wall.
        gates = []
        if rid == "cinnabar_gym":
            for g in GYM_GATES:
                for x, z in g["cells"]: rows[z][x] = "."; fl[z][x] = "g"
                gates.append({"n": g["n"], "cells": g["cells"], "shut": "quiz", "trainer": GYM_TRAINERS[g["trainer"]], "yes": g["yes"],
                              "quiz": g["quiz"], "question": QUESTIONS[g["n"]]})
        for i, ((x0, x1), (z0, z1), when) in enumerate(MAN_GATES.get(rid, [])):
            cells = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
            for x, z in cells:
                rows[z][x] = "."
                fl[z][x] = "n" if rid.endswith("b1f") else "f"
            gates.append({"n": i + 1, "cells": cells, "shut": when})
        for x, z in MAN_POSTS.get(rid, []): rows[z][x] = "w"
        # The warps: the way out, the stairs, 3F's holes, the LAB's doors; and where you land (FireRed's warps that are
        # only somewhere to arrive, under its holes, aren't ways anywhere).
        warps = []
        for wv in mp["warp_events"]:
            x, z, dest = wv["x"], wv["y"], wv["dest_map"]
            here = rows[z][x]
            to = MAP_OF.get(dest)
            if dest == "MAP_CINNABAR_ISLAND": wk = "exit"
            elif here == "D": wk = "door"
            elif here == "H": wk = "hole"
            elif here == "u" or any(0 <= z + dz < h and 0 <= x + dx < w and rows[z + dz][x + dx] == "u" for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                wk = "stairs"; rows[z][x] = "u"
            elif to and rid == "cinnabar_lab" or dest == "MAP_DYNAMIC": wk = "back"
            else: wk = "arrive"
            tx = tz = None
            if to:
                d = json.load(open(os.path.join(silph.P, "data/maps", to, "map.json")))["warp_events"][int(wv["dest_warp_id"])]
                tx, tz = d["x"], d["y"]
            warps.append({"x": x, "z": z, "kind": wk, "to": ROOM.get(to) if to else None, "tx": tx, "tz": tz})
        # The LAB's rooms: their way back is their doormat.
        if rid.startswith("cinnabar_lab_"):
            for wv in warps:
                if wv["kind"] in ("arrive", "back"): wv["kind"] = "back"; wv["to"] = "cinnabar_lab"
        ST, TX = celadon.script_texts(m)
        pl = [r for r in people if r[1] == rid]
        il = [r for r in items if r[1] == rid]
        tl = [r for r in trainers if r[1] == rid]
        objs = []
        for o in mp["object_events"]:
            g = o["graphics_id"].replace("OBJ_EVENT_GFX_", "")
            sc = o.get("script") or ""
            x, z = o["x"], o["y"]
            face = (o.get("movement_type") or "").replace("MOVEMENT_TYPE_", "")
            if g == "ITEM_BALL":
                nm = silph.snake(sc.split("EventScript_Item")[-1]) if "EventScript_Item" in sc else ""
                nm = {"hpup": "hp_up", "h_p_up": "hp_up", "t_m22": "tm_solarbeam", "t_m14": "tm_blizzard", "tm22": "tm_solarbeam", "tm14": "tm_blizzard"}.get(nm, nm)
                pick = next((r for r in il if r[2] == nm), None)
                if pick: il.remove(pick); key = pick[0]
                elif nm in item_ids:
                    key = "%s_fr_%s" % (rid, nm)
                    if key not in have_items: new_items.append([key, rid, nm, "-1", "-1"]); have_items.add(key)
                else:
                    print("  (no such item in the mod: %s)" % nm); continue
                objs.append({"kind": "item", "key": key, "x": x, "z": z})
                continue
            if o.get("trainer_type", "TRAINER_TYPE_NONE") != "TRAINER_TYPE_NONE":
                who = sc.split("EventScript_")[-1].upper()
                key = GYM_TRAINERS.get(who) if rid == "cinnabar_gym" else None
                pick = next((r for r in tl if r[0] == key), None) if key else (tl[0] if tl else None)
                if pick is None: print("  (no trainer for %s in %s)" % (who, rid)); continue
                tl.remove(pick)
                objs.append({"kind": "trainer", "key": pick[0], "name": pick[2].title(), "skin": pick[3], "x": x, "z": z, "face": face, "dir": pick[4], "who": who})
                continue
            if g == "BLAINE":
                objs.append({"kind": "trainer", "key": "cinnabar_gym_0", "name": "Blaine", "skin": "leader_blaine", "x": x, "z": z, "face": face, "dir": "down", "who": "BLAINE"})
                continue
            skin = GFX_SKIN.get(g, "youngster")
            fr = " | ".join(ST.get(sc, [])[:1]) if ST.get(sc) else ""
            trade = TRADERS.get(sc)
            pick = None if trade else (celadon.best(fr, pl) if fr else None) or next((r for r in pl if r[2] == skin), None) or (pl[0] if pl else None)
            if pick is not None:
                pl.remove(pick); key, line, skin = pick[0], pick[5], pick[2]
            else:
                key = "%s_fr_%s" % (rid, silph.snake(sc.split("EventScript_")[-1]) or "p%d" % len(objs))
                line = fr or "..."
                if key not in have_people: new_people.append([key, rid, skin, "down", "stay", line, "-1", "-1"]); have_people.add(key)
                else: line = next((r[5] for r in people if r[0] == key), line)
            objs.append({"kind": "person", "key": key, "skin": skin, "x": x, "z": z, "lines": line.split(" | "), "face": face,
                         "name": NAME.get(g, skin.replace("_", " ").title()), "role": sc.split("EventScript_")[-1], **({"trade": trade} if trade else {})})
        signs, switches = [], []
        for b in mp.get("bg_events", []):
            sc = b.get("script") or ""
            if b.get("type") == "hidden_item" or not sc: continue
            nm = sc.split("EventScript_")[-1]
            lines = [s for l in (ST.get(sc) or []) for s in l.split(" | ")]
            if nm == "Statue" and rid.startswith("pokemon_mansion"):
                switches.append({"x": b["x"], "z": b["y"]}); continue
            if nm.startswith("Quiz"): continue
            if nm == "GymStatue": lines = ["CINNABAR POKéMON GYM", "LEADER: BLAINE", "WINNING TRAINERS: {rival}"]
            if nm.startswith("Diary"):
                dkey = "PokemonMansion_Text_" + nm
                lines = ["Diary: " + nm.replace("Diary", "").replace("th", "th").replace("st", "st")] + [s for s in (TX.get(dkey) or "").split(" | ") if s] if TX.get(dkey) else lines
            if not lines: lines = {"DrFujiPhoto": ["A photo of the LAB's founder, DR. FUJI."], "BlaineFujiPhoto": ["It's a photo of BLAINE and MR. FUJI.", "They're standing shoulder to shoulder with big grins."],
                                   "MeetingRoomSign": ["MEETING ROOM"], "RAndDRoomSign": ["R-and-D ROOM"], "TestingRoomSign": ["TESTING ROOM"],
                                   "AmberPipe": ["An amber pipe!"], "Computer": ["There's an e-mail message here."]}.get(nm, [])
            if not lines: continue
            signs.append({"x": b["x"], "z": b["y"], "lines": lines, "kind": nm})
        hidden = []
        for b in mp.get("bg_events", []):
            if b.get("type") == "hidden_item":
                it = b["item"].replace("ITEM_", "").lower()
                it = {"elixir": "elixer"}.get(it, it)
                if it not in item_ids: continue
                hidden.append({"x": b["x"], "z": b["y"], "item": it, "key": "%s_hidden_%s" % (rid, it)})
        rooms[rid] = {"W": w, "D": h, "name": name, "floor": fname, "map": m, "rows": ["".join(r) for r in rows], "fl": ["".join(r) for r in fl],
                      "ids": [[v[z * w + x] & 0x3FF for x in range(w)] for z in range(h)], "warps": warps, "objs": objs, "signs": signs, "hidden": hidden,
                      "gates": gates, "switches": switches}
    js = "// Written by tools/cinnabar.py from pret/pokefirered: CINNABAR's GYM, LAB and MANSION, a FireRed tile a block.\nconst CINNABAR = " + \
         json.dumps(rooms, separators=(",", ":")) + ";\n"
    open(OUT_JS, "w").write(js)
    for name, rows in (("people.tsv", new_people), ("field_items.tsv", new_items)):
        if rows:
            with open(os.path.join(DATA, name), "a", encoding="utf-8") as f:
                for r in rows: f.write("\t".join(r) + "\n")
    print(len(rooms), "rooms;", len(new_people), "people and", len(new_items), "items added")
    for rid, f in rooms.items():
        print(rid, f["W"], f["D"], collections.Counter("".join(f["rows"])).most_common(), [(w["kind"], w["x"], w["z"], w["to"]) for w in f["warps"]],
              [(o["kind"], o["key"]) for o in f["objs"]], len(f["signs"]), "signs", len(f["gates"]), "gates", f["switches"])


if __name__ == "__main__":
    main()
