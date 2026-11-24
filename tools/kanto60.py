#!/usr/bin/env python3
"""
0.60 (John's 16:14 to 16:28): the rest of Kanto's insides, laid out from FireRed's own maps (pret/pokefirered: data/layouts,
data/maps), a FireRed tile a block, for the Interior Viewer (tools/interior/kanto60_data.js, built by
tools/interior/rooms64.js) and so the mod:

  viridian_gym                 GIOVANNI's GYM: its maze of low grey walls, the spinner tiles (step on an arrow and you
                               slide that way till a yellow stop square or a wall), his platform, the statues
  celadon_gym                  ERIKA's GYM: the hedges and trees, the flower beds, the sandy paths
  indigo_plateau_lobby         the POKéMON LEAGUE's reception (its CENTER counter and its MART)
  loreleis_room .. hall_of_fame  the ELITE FOUR's rooms, LANCE's hall, the CHAMPION's room, the HALL OF FAME
  bills_house                  the SEA COTTAGE (FireRed's: the two teleporter pods joined by a pipe)
  celadon_mansion_*            the CELADON MANSION's three floors, its roof and the room on the roof
  celadon_hotel, celadon_diner, celadon_chief_house
  saffron_pidgey_house, saffron_fan_club
  rocket_hideout_*             the ROCKET HIDEOUT's four floors (B2F and B3F's spinner mazes) and its lift
  pokemon_tower_*              the POKéMON TOWER's seven floors
  ss_anne_*                    the S.S.ANNE's insides: four corridors, every cabin, the kitchen, the captain's office

Each floor's tiles are sorted by FireRed's tile (its behaviour, whether you can walk on it, its picture). Its warps,
signs, people, trainers and items are FireRed's, matched to the mod's (people.tsv, trainers.tsv and field_items.tsv,
written from the first games) room by room; any FireRed has that the mod hasn't are added, with FireRed's words (and,
for trainers, FireRed's parties).

    python3 -I tools/kanto60.py [--debug OUTDIR]
"""
import collections, json, os, re, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "k60"))
import render  # noqa: E402

P = render.P
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")
OUT_JS = os.path.join(HERE, "interior", "kanto60_data.js")

# (FireRed's map, the room, its name, its floor, the group in the viewer, its look)
MAPS = [("ViridianCity_Gym", "viridian_gym", "Viridian Gym", "", "Viridian City", "vgym"),
        ("CeladonCity_Gym", "celadon_gym", "Celadon Gym", "", "Celadon City", "cgym"),
        ("IndigoPlateau_PokemonCenter_1F", "indigo_plateau_lobby", "Pokémon League", "Reception", "Indigo Plateau", "lobby"),
        ("PokemonLeague_LoreleisRoom", "loreleis_room", "Pokémon League", "Lorelei", "Indigo Plateau", "lorelei"),
        ("PokemonLeague_BrunosRoom", "brunos_room", "Pokémon League", "Bruno", "Indigo Plateau", "bruno"),
        ("PokemonLeague_AgathasRoom", "agathas_room", "Pokémon League", "Agatha", "Indigo Plateau", "agatha"),
        ("PokemonLeague_LancesRoom", "lances_room", "Pokémon League", "Lance", "Indigo Plateau", "lance"),
        ("PokemonLeague_ChampionsRoom", "champions_room", "Pokémon League", "Champion", "Indigo Plateau", "champion"),
        ("PokemonLeague_HallOfFame", "hall_of_fame", "Pokémon League", "Hall of Fame", "Indigo Plateau", "fame"),
        ("Route25_SeaCottage", "bills_house", "Sea Cottage", "Route 25", "Route 25", "cottage"),
        ("CeladonCity_Condominiums_1F", "celadon_mansion_1f", "Celadon Mansion", "1F", "Celadon City", "condo"),
        ("CeladonCity_Condominiums_2F", "celadon_mansion_2f", "Celadon Mansion", "2F", "Celadon City", "condo"),
        ("CeladonCity_Condominiums_3F", "celadon_mansion_3f", "Celadon Mansion", "3F", "Celadon City", "condo"),
        ("CeladonCity_Condominiums_Roof", "celadon_mansion_roof", "Celadon Mansion", "Roof", "Celadon City", "roof"),
        ("CeladonCity_Condominiums_RoofRoom", "celadon_mansion_roof_house", "Celadon Mansion", "Roof room", "Celadon City", "roofroom"),
        ("CeladonCity_Hotel", "celadon_hotel", "Celadon Hotel", "", "Celadon City", "hotel"),
        ("CeladonCity_Restaurant", "celadon_diner", "Celadon Restaurant", "", "Celadon City", "diner"),
        ("CeladonCity_House1", "celadon_chief_house", "Celadon house", "", "Celadon City", "house"),
        ("SaffronCity_House", "saffron_pidgey_house", "Saffron house", "", "Saffron City", "house"),
        ("SaffronCity_PokemonTrainerFanClub", "saffron_fan_club", "Pokémon Trainer Fan Club", "", "Saffron City", "fanclub")] + \
       [("RocketHideout_B%dF" % i, "rocket_hideout_b%df" % i, "Rocket Hideout", "B%dF" % i, "Rocket Hideout", "hideout") for i in range(1, 5)] + \
       [("RocketHideout_Elevator", "rocket_hideout_elevator", "Rocket Hideout", "Lift", "Rocket Hideout", "hideout")] + \
       [("PokemonTower_%dF" % i, "pokemon_tower_%df" % i, "Pokémon Tower", "%dF" % i, "Pokémon Tower", "tower") for i in range(1, 8)] + \
       [("SSAnne_1F_Corridor", "ss_anne_1f", "S.S. Anne", "1F", "S.S. Anne", "ship"), ("SSAnne_2F_Corridor", "ss_anne_2f", "S.S. Anne", "2F", "S.S. Anne", "ship"),
        ("SSAnne_3F_Corridor", "ss_anne_3f", "S.S. Anne", "3F", "S.S. Anne", "ship"), ("SSAnne_B1F_Corridor", "ss_anne_b1f", "S.S. Anne", "B1F", "S.S. Anne", "ship"),
        ("SSAnne_Kitchen", "ss_anne_kitchen", "S.S. Anne", "Kitchen", "S.S. Anne", "ship"),
        ("SSAnne_CaptainsOffice", "ss_anne_captains_room", "S.S. Anne", "Captain's office", "S.S. Anne", "ship")] + \
       [("SSAnne_1F_Room%d" % i, "ss_anne_1f_room%d" % i, "S.S. Anne", "1F cabin %d" % i, "S.S. Anne", "cabin") for i in range(1, 8)] + \
       [("SSAnne_2F_Room%d" % i, "ss_anne_2f_room%d" % i, "S.S. Anne", "2F cabin %d" % i, "S.S. Anne", "cabin") for i in range(1, 7)] + \
       [("SSAnne_B1F_Room%d" % i, "ss_anne_b1f_room%d" % i, "S.S. Anne", "B1F cabin %d" % i, "S.S. Anne", "cabin") for i in range(1, 6)]
ROOM = {m: r for m, r, *_ in MAPS}
KIND = {r: k for m, r, n, f, g, k in MAPS}


def mapconst(m):
    """FireRed's MAP_ constant for a map folder name."""
    s = re.sub(r"(?<=[a-z])(?=[A-Z])", "_", m).upper()
    return "MAP_" + s.replace("S_S_ANNE", "SSANNE").replace("ROUTE25", "ROUTE25")


MAP_OF = {mapconst(m): m for m, *_ in MAPS}
MAP_OF.update({"MAP_SSANNE_1F_CORRIDOR": "SSAnne_1F_Corridor", "MAP_SSANNE_2F_CORRIDOR": "SSAnne_2F_Corridor", "MAP_SSANNE_3F_CORRIDOR": "SSAnne_3F_Corridor",
               "MAP_SSANNE_B1F_CORRIDOR": "SSAnne_B1F_Corridor", "MAP_SSANNE_KITCHEN": "SSAnne_Kitchen", "MAP_SSANNE_CAPTAINS_OFFICE": "SSAnne_CaptainsOffice",
               "MAP_POKEMON_LEAGUE_LORELEIS_ROOM": "PokemonLeague_LoreleisRoom", "MAP_POKEMON_LEAGUE_BRUNOS_ROOM": "PokemonLeague_BrunosRoom",
               "MAP_POKEMON_LEAGUE_AGATHAS_ROOM": "PokemonLeague_AgathasRoom", "MAP_POKEMON_LEAGUE_LANCES_ROOM": "PokemonLeague_LancesRoom",
               "MAP_POKEMON_LEAGUE_CHAMPIONS_ROOM": "PokemonLeague_ChampionsRoom", "MAP_POKEMON_LEAGUE_HALL_OF_FAME": "PokemonLeague_HallOfFame",
               "MAP_INDIGO_PLATEAU_POKEMON_CENTER_1F": "IndigoPlateau_PokemonCenter_1F",
               "MAP_CELADON_CITY_CONDOMINIUMS_ROOF_ROOM": "CeladonCity_Condominiums_RoofRoom", "MAP_CELADON_CITY_CONDOMINIUMS_ROOF": "CeladonCity_Condominiums_Roof"})
for i in range(1, 8): MAP_OF["MAP_SSANNE_1F_ROOM%d" % i] = "SSAnne_1F_Room%d" % i
for i in range(1, 7): MAP_OF["MAP_SSANNE_2F_ROOM%d" % i] = "SSAnne_2F_Room%d" % i
for i in range(1, 6): MAP_OF["MAP_SSANNE_B1F_ROOM%d" % i] = "SSAnne_B1F_Room%d" % i
MAP_OF["MAP_CELADON_CITY_GAME_CORNER"] = "CeladonCity_GameCorner"
ROOM["CeladonCity_GameCorner"] = "game_corner"
OUTSIDE = {"MAP_VIRIDIAN_CITY", "MAP_CELADON_CITY", "MAP_SAFFRON_CITY", "MAP_LAVENDER_TOWN", "MAP_INDIGO_PLATEAU_EXTERIOR", "MAP_ROUTE25",
           "MAP_SSANNE_EXTERIOR", "MAP_SSANNE_DECK", "MAP_INDIGO_PLATEAU_POKEMON_CENTER_2F"}

# The places the mod's tables (the first games') file each room's people, trainers and items under.
POOL = {"ss_anne_1f": "ss_anne_1f", "ss_anne_2f": "ss_anne_2f", "ss_anne_3f": "ss_anne_3f", "ss_anne_b1f": "ss_anne_b1f",
        "celadon_mansion_roof_house": "celadon_mansion_roof_house", "saffron_fan_club": "saffron_fan_club"}
for i in range(1, 8): POOL["ss_anne_1f_room%d" % i] = "ss_anne_1f_rooms"
for i in range(1, 7): POOL["ss_anne_2f_room%d" % i] = "ss_anne_2f_rooms"
for i in range(1, 6): POOL["ss_anne_b1f_room%d" % i] = "ss_anne_b1f_rooms"

# ---------------------------------------------------------------------------------------------------------------------
# The tiles: what each is, by tileset. Kinds:
#   v nothing (black)          w a wall (full height)        b a low wall or ledge (a GYM's maze, a counter's back)
#   . floor                    X the way out                 u stairs        e a lift's door
#   p a plant                  T a tree or hedge             k a counter or table          h a shelf
#   m a machine or PC          s a sofa                      o a chair or stool            B a bed
#   S a statue                 G a gravestone                c a crate, barrel, bin        K a stove or sink
#   P a pillar                 x a teleporter pod (BILL's)   R a pipe (BILL's)            F a platform (GIOVANNI's)
#   Q the HALL OF FAME's machine                             H the horns (LANCE's)
# ---------------------------------------------------------------------------------------------------------------------
def ids(*xs):
    out = set()
    for x in xs:
        if isinstance(x, tuple): out.update(range(x[0], x[1] + 1))
        else: out.add(x)
    return out


def table_of(spec):
    out = {}
    for k, s in spec.items():
        for i in s: out[i] = k
    return out


OVR = {
    "ViridianGym": table_of({"S": ids(0x2a1, 0x2a9), "F": ids(0x2bd, 0x2be, 0x2bf, 0x2cd, 0x2ce, 0x2cf), "X": ids(0x2b2, 0x2b3, 0x2b4)}),
    "CeladonGym": table_of({"S": ids(0x2a1, 0x2b9), "T": ids(0x287, 0x297, 0x28d, 0x295, 0x2bf, 0x2be), "X": ids(0x174, 0x175, 0x176)}),
}
FLOORS = {
    "ViridianGym": table_of({"m": ids(0x2aa, 0x2ab, 0x2ac), "F": ids(0x2bd, 0x2be, 0x2bf, 0x2cd, 0x2ce, 0x2cf)}),
    "CeladonGym": table_of({"f": ids(0x2b6, 0x2b7), "m": ids(0x2b0, 0x2b1, 0x2b2),
                            "s": ids(0x2ab, 0x29d, 0x2ad, 0x2bb, 0x2ae, 0x2a5, 0x2af, 0x2a8, 0x2a9, 0x29e, 0x29f, 0x2aa, 0x2a6, 0x2a7, 0x29c, 0x2a4, 0x2b0)}),
}


def picture(l, mid):
    """A metatile's 16 x 16 picture."""
    m, ll, w, h, b = None, l, 1, 1, [mid]
    orig = render.layout
    render.layout = lambda name: ({}, ll, 1, 1, [mid])
    try:
        return render.draw("x", 1)
    finally:
        render.layout = orig


_pic = {}


def stats(l, mid):
    key = (l["primary_tileset"], l["secondary_tileset"], mid)
    if key in _pic: return _pic[key]
    im = picture(l, mid).convert("RGB")
    px = [im.getpixel((x, y)) for y in range(16) for x in range(16)]
    f = lambda pred: sum(1 for p in px if pred(p)) / 256.0
    s = {"black": f(lambda p: max(p) < 30), "white": f(lambda p: min(p) > 225),
         "green": f(lambda p: p[1] > p[0] + 25 and p[1] > p[2] + 15),
         "mean": tuple(sum(p[i] for p in px) // 256 for i in range(3))}
    _pic[key] = s
    return s


def classify(m, l, w, h, v, att):
    """Each tile's kind (rows), its floor's kind (fl), the spinners."""
    ts = l["secondary_tileset"].replace("gTileset_", "")
    ovr, fov = OVR.get(ts, {}), FLOORS.get(ts, {})
    rows = [["."] * w for _ in range(h)]
    fl = [["a"] * w for _ in range(h)]
    spin = {}
    for z in range(h):
        for x in range(w):
            b = v[z * w + x]
            mid, coll = b & 0x3FF, (b >> 10) & 3
            beh = att(mid)
            st = stats(l, mid)
            k = None
            if mid in ovr: k = ovr[mid]
            elif beh in (0x54, 0x55, 0x56, 0x57): spin[(x, z)] = {0x54: "e", 0x55: "w", 0x56: "n", 0x57: "s"}[beh]; k = "."
            elif beh == 0x58: spin[(x, z)] = "stop"; k = "."
            elif st["black"] > 0.85: k = "v"
            elif not coll: k = "."
            elif beh in (0x81, 0x82, 0x89, 0x8B): k = "h"
            elif beh in (0x83, 0x97, 0x86, 0x95, 0xA0, 0x8D, 0x8E, 0xA3, 0xA1, 0x9E): k = "m"
            elif beh in (0x80, 0x90, 0x99, 0x9B, 0x8C): k = "k"
            elif beh == 0x8A: k = "K"
            elif beh == 0x9A: k = "c"
            elif beh in (0x94, 0x9D, 0x9C, 0x98, 0x85, 0x93, 0x87, 0x88, 0x91, 0x92): k = "w"
            elif beh == 0x84: k = "S"
            elif st["green"] > 0.30: k = "p"
            else: k = "#"
            rows[z][x] = k
            fl[z][x] = fov.get(mid, "a")
    # The unsorted solid tiles: a wall where they reach the room's edge or the black, else a low block.
    seen = set()
    for z in range(h):
        for x in range(w):
            if rows[z][x] != "#" or (x, z) in seen: continue
            grp, todo = [], [(x, z)]
            while todo:
                c = todo.pop()
                if c in seen: continue
                cx, cz = c
                if not (0 <= cx < w and 0 <= cz < h) or rows[cz][cx] != "#": continue
                seen.add(c); grp.append(c)
                todo += [(cx + 1, cz), (cx - 1, cz), (cx, cz + 1), (cx, cz - 1)]
            edge = any(cx in (0, w - 1) or cz in (0, h - 1) or any(0 <= cx + dx < w and 0 <= cz + dz < h and rows[cz + dz][cx + dx] == "v"
                                                                  for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) for cx, cz in grp)
            big = len(grp) >= 10
            for cx, cz in grp: rows[cz][cx] = "w" if (edge or big) else "b"
    return rows, fl, spin


# ---------------------------------------------------------------------------------------------------------------------
def texts(m):
    """FireRed's words on a map, by label (the TRAINER FAN CLUB's fans all talk about LANCE: the game fills in whoever's
    most popular)."""
    d = os.path.join(P, "data/maps", m)
    tx, cur = {}, None
    if os.path.exists(os.path.join(d, "text.inc")):
        for line in open(os.path.join(d, "text.inc"), encoding="utf-8"):
            mm = re.match(r"(\w+)::", line)
            if mm: cur = mm.group(1); tx[cur] = ""; continue
            mm = re.search(r'\.string "(.*)"', line)
            if mm and cur: tx[cur] += mm.group(1)
    return {k: clean(s.replace("{STR_VAR_1}", "LANCE" if "FanClub" in m else "{STR_VAR_1}")) for k, s in tx.items()}


def clean(s):
    s = s.replace("\\p", " | ").replace("\\n", " ").replace("\\l", " ").replace("$", "").replace("…", "...")
    s = re.sub(r"\{FONT_\w+\}|\{COLOR \w+\}|\{SHADOW \w+\}|\{PAUSE \w+\}|\{PLAY_SE \w+\}", "", s)
    s = s.replace("{PLAYER}", "{player}").replace("{RIVAL}", "{rival}").replace("{STR_VAR_1}", "")
    return re.sub(r"\s+", " ", s).strip().replace(" | |", " |")


def scripts(m):
    """Each script's lines, and the trainer it battles (FireRed's TRAINER_ constant), its words before and after."""
    d = os.path.join(P, "data/maps", m)
    TX = texts(m)
    body, cur = collections.defaultdict(list), None
    if os.path.exists(os.path.join(d, "scripts.inc")):
        for line in open(os.path.join(d, "scripts.inc"), encoding="utf-8"):
            mm = re.match(r"(\w+)::", line)
            if mm: cur = mm.group(1); continue
            if cur: body[cur].append(line.strip())
    out = {}
    for name, lines in body.items():
        words, trainer = [], None
        for ln in lines:
            mm = re.search(r"trainerbattle_\w+ (TRAINER_\w+), (\w+), (\w+)(?:, (\w+))?", ln)
            if mm:
                trainer = {"const": mm.group(1), "intro": TX.get(mm.group(2), ""), "defeat": TX.get(mm.group(3), "")}
                continue
            mm = re.search(r"(?:msgbox|message) (\w+)", ln)
            if mm and mm.group(1) in TX: words.append(TX[mm.group(1)])
        if trainer: trainer["after"] = words[0] if words else ""
        out[name] = {"words": words, "trainer": trainer}
    return out, TX


def frlg_trainers():
    """FireRed's trainers: TRAINER_ constant -> (class, name, party [(species, level)])."""
    src = open(os.path.join(P, "src/data/trainers.h"), encoding="utf-8").read()
    parties = open(os.path.join(P, "src/data/trainer_parties.h"), encoding="utf-8").read()
    pmap = {}
    for mm in re.finditer(r"static const struct \w+ (sParty_\w+)\[\] = \{(.*?)\};", parties, re.S):
        mons = re.findall(r"\.lvl = (\d+),.*?\.species = SPECIES_(\w+)", mm.group(2), re.S)
        pmap[mm.group(1)] = [(s.lower(), int(lv)) for lv, s in mons]
    out = {}
    for mm in re.finditer(r"\[(TRAINER_\w+)\] = \{(.*?)\n    \},", src, re.S):
        body = mm.group(2)
        cls = re.search(r"\.trainerClass = CLASS_(\w+)", body)
        name = re.search(r'\.trainerName = _\("(.*?)"\)', body)
        party = re.search(r"\.party = \w+\((sParty_\w+)\)", body)
        out[mm.group(1)] = (cls.group(1).replace("_", " ") if cls else "", name.group(1) if name else "", pmap.get(party.group(1), []) if party else [])
    return out


def table(name):
    return [l.rstrip("\n").split("\t") for l in open(os.path.join(DATA, name), encoding="utf-8") if l.strip() and not l.startswith("#")]


def words(s):
    return set(re.findall(r"[a-z]{3,}", s.lower().replace("é", "e")))


def best(line, rows):
    w = words(line)
    scored = sorted(((len(w & words(r[5])) / max(1, len(w | words(r[5]))), r) for r in rows), key=lambda t: -t[0])
    return scored[0][1] if scored and scored[0][0] >= 0.18 else None


GFX_SKIN = {"SCIENTIST": "scientist", "POKE_MANIAC": "super_nerd", "YOUNGSTER": "youngster", "GYM_GUY": "gym_guide", "OLD_MAN_1": "gramps",
            "OLD_MAN_2": "gramps", "WOMAN_1": "lady", "WOMAN_2": "beauty", "WOMAN_3": "middle_aged_woman", "BOY": "boy", "FISHER": "fishing_guru",
            "GENTLEMAN": "gentleman", "SAILOR": "sailor", "LASS": "girl", "LITTLE_GIRL": "little_girl", "LITTLE_BOY": "little_boy", "MAN": "super_nerd",
            "BALDING_MAN": "balding_guy", "CHEF": "cook", "ROCKET_M": "rocket", "ROCKET_F": "rocket", "GIOVANNI": "leader_giovanni",
            "CHANNELER": "channeler", "MR_FUJI": "mr_fuji", "NURSE": "nurse", "CLERK": "clerk", "WORKER_M": "clerk", "WORKER_F": "link_receptionist",
            "BILL": "bill", "CLEFAIRY": "clefairy", "MEOWTH": "meowth", "EEVEE": "eevee", "POKE_BALL": "poke_ball", "BLUE": "blue", "PROF_OAK": "oak",
            "LORELEI": "lorelei", "BRUNO": "bruno", "AGATHA": "agatha", "LANCE": "lance", "ERIKA": "leader_erika", "BEAUTY": "beauty",
            "COOLTRAINER_M": "cooltrainer_m", "COOLTRAINER_F": "cooltrainer_f", "BLACK_BELT": "hiker", "TAMER": "rocker", "ROCKER": "rocker",
            "CAPTAIN": "captain", "SUPER_NERD": "super_nerd", "CAMPER": "youngster", "PICNICKER": "picnicker", "GAMBLER": "gentleman",
            "NIDORAN_M": "nidoran_m", "PSYDUCK": "psyduck", "SLOWPOKE": "slowpoke", "SEEL": "seel", "MACHOP": "machop", "CHANSEY": "chansey",
            "PIDGEY": "pidgey", "GHOST": "gastly", "TWIN": "little_girl", "HIKER": "hiker"}
POKEMON_GFX = {"CLEFAIRY", "MEOWTH", "EEVEE", "NIDORAN_M", "PSYDUCK", "SLOWPOKE", "SEEL", "MACHOP", "CHANSEY", "PIDGEY", "SPEAROW", "CUBONE",
               "JIGGLYPUFF", "PIKACHU", "POLIWRATH", "LAPRAS", "SNORLAX", "DODUO", "FEAROW", "MAROWAK", "GENGAR", "ARTICUNO", "ZAPDOS", "MOLTRES",
               "MEWTWO", "WIGGLYTUFF", "VENUSAUR", "CHARIZARD", "BLASTOISE"}


# The leaders and the ELITE FOUR (FireRed's scripted battles): the mod's trainer row with their sprite.
BOSS = {"GIOVANNI": "leader_giovanni", "ERIKA": "leader_erika", "LORELEI": "lorelei", "BRUNO": "bruno", "AGATHA": "agatha", "LANCE": "lance"}
# Your rival, where he battles you in these rooms (the mod makes his party for you, by your starter).
RIVAL = {"ss_anne_2f": "rival_ssanne", "pokemon_tower_2f": "rival_tower", "champions_room": "rival_champion"}


def skin_of(g):
    return GFX_SKIN.get(g, g.lower())


def main():
    debug = sys.argv[sys.argv.index("--debug") + 1] if "--debug" in sys.argv else None
    people, trainers, items = table("people.tsv"), table("trainers.tsv"), table("field_items.tsv")
    have_people, have_items, have_trainers = {r[0] for r in people}, {r[0] for r in items}, {r[0] for r in trainers}
    item_ids = {r[0] for r in table("items.tsv")}
    FR = frlg_trainers()
    pools = collections.defaultdict(lambda: {"p": [], "t": [], "i": []})
    for r in people: pools[r[1]]["p"].append(r)
    for r in trainers: pools[r[1]]["t"].append(r)
    for r in items: pools[r[1]]["i"].append(r)
    rooms, new_people, new_items, new_trainers = {}, [], [], []
    for m, rid, name, fname, group, kind in MAPS:
        mp, l, w, h, v = render.layout(m)
        att = render.attrs(l)
        rows, fl, spin = classify(m, l, w, h, v, att)
        SC, TX = scripts(m)
        # ----- The warps -----
        warps = []
        for wv in mp["warp_events"]:
            x, z, dest = wv["x"], wv["y"], wv["dest_map"]
            beh = att(v[z * w + x] & 0x3FF)
            to = MAP_OF.get(dest)
            tx = tz = None
            if to:
                dw = json.load(open(os.path.join(P, "data/maps", to, "map.json")))["warp_events"] if to != "CeladonCity_GameCorner" else []
                di = int(wv["dest_warp_id"]) if str(wv["dest_warp_id"]).isdigit() else 0
                if di < len(dw): tx, tz = dw[di]["x"], dw[di]["y"]
            if to == "CeladonCity_GameCorner": tx, tz = 15, 2
            if dest in OUTSIDE or dest == "MAP_DYNAMIC" and kind != "hideout": wk = "exit"
            elif dest == "MAP_DYNAMIC": wk = "lift_out"
            elif to == "RocketHideout_Elevator": wk = "lift"
            elif beh in (0x6C, 0x6D, 0x6E, 0x6F) or (rows[z][x] in "#wb" and beh == 0x67): wk = "stairs"
            elif beh in (0x69, 0x60): wk = "door"
            elif beh == 0x65 or (z >= h - 2): wk = "mat"
            else: wk = "pad"
            side = "s" if z >= h - 2 else "n" if z <= 2 else "w" if x <= 1 else "e" if x >= w - 2 else "s"
            warps.append({"x": x, "z": z, "kind": wk, "to": ROOM.get(to) if to else None, "tx": tx, "tz": tz, "beh": beh, "side": side,
                          "dest": dest.replace("MAP_", "").lower()})
            if wk == "stairs": rows[z][x] = "u"
            elif wk in ("exit", "mat") and rows[z][x] in "v#wb": rows[z][x] = "X"
            elif wk == "lift": rows[z][x] = "e"
        # ----- People, trainers, items -----
        pool = pools[POOL.get(rid, rid)]
        objs = []
        for o in mp["object_events"]:
            g = o["graphics_id"].replace("OBJ_EVENT_GFX_", "")
            sc = o.get("script") or ""
            x, z = o["x"], o["y"]
            face = (o.get("movement_type") or "").replace("MOVEMENT_TYPE_", "")
            S = SC.get(sc, {"words": [], "trainer": None})
            flag = o.get("flag") or ""
            if g == "ITEM_BALL":
                nm = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", sc.split("EventScript_Item")[-1]).lower() if "EventScript_Item" in sc else ""
                nm = {"hpup": "hp_up", "pp_up": "pp_up", "elixir": "elixer", "max_elixir": "max_elixer", "t_m": "tm"}.get(nm, nm)
                if nm.startswith("tm") and nm[2:].isdigit(): nm = "tm%02d" % int(nm[2:])
                pick = next((r for r in pool["i"] if r[2] == nm), None) or (pool["i"][0] if pool["i"] and nm not in item_ids else None)
                if pick: pool["i"].remove(pick); key = pick[0]; it = pick[2]
                elif nm in item_ids:
                    key = "%s_fr_%s" % (rid, nm); it = nm
                    if key not in have_items: new_items.append([key, rid, nm, "-1", "-1"]); have_items.add(key)
                else:
                    print("  (no such item: %s in %s)" % (nm, rid)); continue
                objs.append({"kind": "item", "key": key, "x": x, "z": z, "item": it})
                continue
            tr = S["trainer"] if S else None
            if g in ("BLUE",) or (g.startswith("VAR") and "Rival" in sc):
                objs.append({"kind": "rival", "key": RIVAL.get(rid, "rival_" + rid), "skin": "blue", "x": x, "z": z, "face": face, "name": "{rival}"})
                continue
            if g in BOSS:
                pick = next((r for r in pool["t"] if r[3] == BOSS[g]), None)
                if pick:
                    pool["t"].remove(pick)
                    objs.append({"kind": "trainer", "key": pick[0], "name": pick[2].title(), "skin": pick[3], "x": x, "z": z, "face": face, "who": g, "sight": 0, "boss": True})
                    continue
            if o.get("trainer_type", "TRAINER_TYPE_NONE") != "TRAINER_TYPE_NONE" or tr:
                if not tr:
                    print("  (no trainer script for %s %s)" % (rid, sc)); continue
                const = tr["const"]
                pick = next((r for r in pool["t"] if r[3] == skin_of(g)), None) or (pool["t"][0] if pool["t"] else None)
                if pick is not None:
                    pool["t"].remove(pick)
                    key, title, skin = pick[0], pick[2], pick[3]
                else:
                    cls, nm, party = FR.get(const, ("", "", []))
                    key = "%s_fr_%s" % (rid, const.replace("TRAINER_", "").lower())
                    title = (cls + " " + nm).strip().upper().replace("POKEMON", "POKéMON").replace("POKeMON", "POKéMON")
                    skin = skin_of(g)
                    if key not in have_trainers and party:
                        new_trainers.append([key, rid, title, skin, {"FACE_UP": "up", "FACE_DOWN": "down", "FACE_LEFT": "left", "FACE_RIGHT": "right"}.get(face, "down"),
                                             str(o.get("trainer_sight_or_berry_tree_id", "3") or "3"), "30",
                                             ",".join("%s:%d" % sp for sp in party), "-", tr["intro"] or "...", tr["defeat"] or "...", tr["after"] or "...", "-1", "-1"])
                        have_trainers.add(key)
                    elif not party:
                        print("  (no FireRed party for %s %s)" % (rid, const)); continue
                objs.append({"kind": "trainer", "key": key, "name": title.title(), "skin": skin, "x": x, "z": z, "face": face, "who": const,
                             "sight": int(o.get("trainer_sight_or_berry_tree_id") or 0) if str(o.get("trainer_sight_or_berry_tree_id") or "0").isdigit() else 0})
                continue
            if g in ("ROCK_SMASH_ROCK", "PUSHABLE_BOULDER", "CUT_TREE"): continue
            fr = S["words"][0] if S and S["words"] else ""
            if g.startswith("0x") or g.startswith("VAR") or "Journal" in sc or "Clipboard" in sc:
                if fr: objs.append({"kind": "notebook", "x": x, "z": z, "lines": fr.split(" | ")})
                continue
            if g in POKEMON_GFX:
                objs.append({"kind": "mon", "species": g.lower(), "x": x, "z": z, "face": face, "lines": fr.split(" | ") if fr else ["..."], "role": sc.split("EventScript_")[-1]})
                continue
            skin = skin_of(g)
            pick = (best(fr, pool["p"]) if fr else None) or next((r for r in pool["p"] if r[2] == skin), None)
            if pick is not None:
                pool["p"].remove(pick); key, line, skin = pick[0], pick[5], pick[2]
            else:
                role = sc.split("EventScript_")[-1] or "p%d" % len(objs)
                key = "%s_fr_%s" % (rid, re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", role).lower())
                line = fr or "..."
                if key not in have_people: new_people.append([key, rid, skin, "down", "stay", line, "-1", "-1"]); have_people.add(key)
                else: line = next((r[5] for r in people + new_people if r[0] == key), line)
            objs.append({"kind": "person", "key": key, "skin": skin, "x": x, "z": z, "lines": line.split(" | "), "face": face,
                         "name": skin.replace("_", " ").title(), "role": sc.split("EventScript_")[-1], "gfx": g, "flag": flag})
        # ----- Signs, things to look at, hidden items -----
        signs, hidden = [], []
        for b in mp.get("bg_events", []):
            if b.get("type") == "hidden_item":
                it = b["item"].replace("ITEM_", "").lower()
                it = {"elixir": "elixer"}.get(it, it)
                if it in item_ids: hidden.append({"x": b["x"], "z": b["y"], "item": it, "key": "%s_hidden_%s_%d_%d" % (rid, it, b["x"], b["y"])})
                continue
            sc = b.get("script") or ""
            if not sc: continue
            S = SC.get(sc)
            lines = [s for l2 in (S["words"] if S else []) for s in l2.split(" | ")]
            nm = sc.split("EventScript_")[-1]
            if not lines:
                mm = TX.get(sc.replace("EventScript", "Text")) or TX.get(m + "_Text_" + nm)
                lines = mm.split(" | ") if mm else []
            if not lines: lines = {"Poster": ["Hey!"]}.get(nm, [])
            if not lines: continue
            signs.append({"x": b["x"], "z": b["y"], "lines": lines, "kind": nm})
        rooms[rid] = {"W": w, "D": h, "name": name, "floor": fname, "group": group, "kind": kind, "map": m,
                      "rows": ["".join(r) for r in rows], "fl": ["".join(r) for r in fl],
                      "ids": [[v[z * w + x] & 0x3FF for x in range(w)] for z in range(h)],
                      "spin": [[x, z, d] for (x, z), d in sorted(spin.items())],
                      "warps": warps, "objs": objs, "signs": signs, "hidden": hidden}
        if debug:
            from PIL import ImageDraw
            im = render.draw(m, 2)
            d = ImageDraw.Draw(im)
            for z in range(h):
                for x in range(w):
                    c = rows[z][x]
                    col = {"w": (255, 60, 60), "b": (255, 160, 0), ".": (120, 255, 120), "v": (90, 90, 90)}.get(c, (80, 200, 255))
                    d.text((x * 32 + 3, z * 32 + 2), c + fl[z][x], fill=col)
                    if (x, z) in spin: d.text((x * 32 + 18, z * 32 + 16), spin[(x, z)][0], fill=(255, 255, 0))
            for wv in warps: d.rectangle([wv["x"] * 32, wv["z"] * 32, wv["x"] * 32 + 31, wv["z"] * 32 + 31], outline=(255, 0, 255))
            for o in objs: d.ellipse([o["x"] * 32 + 8, o["z"] * 32 + 8, o["x"] * 32 + 24, o["z"] * 32 + 24], outline=(255, 255, 255))
            im.save(os.path.join(debug, "cls_%s.png" % rid))
    js = ("// Written by tools/kanto60.py from pret/pokefirered: the rest of KANTO's insides, a FireRed tile a block.\nconst K60 = " +
          json.dumps(rooms, separators=(",", ":")) + ";\n")
    open(OUT_JS, "w").write(js)
    if "--write" in sys.argv:
        for name, rows in (("people.tsv", new_people), ("field_items.tsv", new_items), ("trainers.tsv", new_trainers)):
            if rows:
                with open(os.path.join(DATA, name), "a", encoding="utf-8") as f:
                    for r in rows: f.write("\t".join(r) + "\n")
    print(len(rooms), "rooms;", len(new_people), "people,", len(new_items), "items,", len(new_trainers), "trainers new", "(written)" if "--write" in sys.argv else "(not written)")
    for rid, f in rooms.items():
        print("%-28s %2dx%-2d %s" % (rid, f["W"], f["D"], " ".join("%s%s>%s" % (w["kind"], (w["x"], w["z"]), w["to"] or w["dest"]) for w in f["warps"])))
        print("     ", ", ".join("%s:%s" % (o["kind"][0], o.get("key") or o.get("species")) for o in f["objs"]), "| signs", len(f["signs"]), "spin", len(f["spin"]))
    for r in new_trainers: print("NEW TRAINER", r[:8])
    for r in new_people: print("NEW PERSON", r[:3], r[5][:60])


if __name__ == "__main__":
    main()
