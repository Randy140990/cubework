#!/usr/bin/env python3
"""
Reads the Pokémon data out of pret's disassemblies of the original games and writes it as the tables the mod
loads (src/main/resources/retromon/data):

- species.tsv  all 251, from Gold/Silver (pokegold): base stats, types, catch rate, base EXP, growth rate,
               gender ratio, evolutions and level-up moves, and TM/HM compatibility.
- moves.tsv    all 251 moves: effect, power, type, accuracy, PP, effect chance, priority, high crit.
- wild.tsv     wild encounter tables: Kanto from Yellow (pokeyellow), with the species Yellow lacks taken
               from Red and Blue (pokered) so all 151 can be caught; Johto from Silver (pokegold).
- places.tsv   every town, route and dungeon: its name, its music and its encounter table.

Usage: pret_import.py <pret folder with pokered, pokeyellow, pokegold>
"""
import glob
import os, re, sys

PRET = sys.argv[1] if len(sys.argv) > 1 else "/home/claude/pret"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "retromon", "data")
GOLD = os.path.join(PRET, "pokegold")
RED = os.path.join(PRET, "pokered")
YELLOW = os.path.join(PRET, "pokeyellow")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def strip(line):
    return line.split(";")[0].strip()


def ident(const):
    """BULBASAUR -> bulbasaur, MR__MIME -> mr_mime, NIDORAN_F -> nidoran_f, HO_OH -> ho_oh."""
    return re.sub(r"_+", "_", const.lower()).strip("_")


# ---------- species ----------

def species_constants():
    out = []
    for line in read(os.path.join(GOLD, "constants/pokemon_constants.asm")).splitlines():
        m = re.match(r"\s*const (\w+)\s*;\s*([0-9a-f]+)", line)
        if m and m.group(1) not in ("EGG",):
            out.append(m.group(1))
        if len(out) == 251:
            break
    return out


def names(path, macro):
    out = []
    for line in read(path).splitlines():
        m = re.match(r'\s*' + macro + r'\s+"([^"]*)"', line)
        if m:
            out.append(m.group(1).replace("@", "").strip())
    return out


GROWTH = ["MEDIUM_FAST", "SLIGHTLY_FAST", "SLIGHTLY_SLOW", "MEDIUM_SLOW", "FAST", "SLOW"]
GENDER = {"GENDER_F0": 0, "GENDER_F12_5": 31, "GENDER_F25": 63, "GENDER_F50": 127, "GENDER_F75": 191, "GENDER_F100": 254, "GENDER_UNKNOWN": -1}


def base_stats(consts):
    files = re.findall(r'INCLUDE "(data/pokemon/base_stats/[^"]+)"', read(os.path.join(GOLD, "data/pokemon/base_stats.asm")))
    out = {}
    for f, c in zip(files, consts):
        text = read(os.path.join(GOLD, f))
        lines = [strip(l) for l in text.splitlines() if strip(l)]
        stats = None
        types = catch = exp = growth = gender = None
        tm = []
        for l in lines:
            if l.startswith("db") and stats is None and re.match(r"db\s+\d+,\s*\d+", l):
                stats = [int(x) for x in re.findall(r"\d+", l)][:6]
            elif l.startswith("db") and "," in l and types is None and stats is not None:
                types = [t.strip() for t in l[2:].split(",")]
            elif l.startswith("db") and catch is None and types is not None:
                catch = int(re.findall(r"\d+", l)[0])
            elif l.startswith("db") and exp is None and catch is not None:
                exp = int(re.findall(r"\d+", l)[0])
            elif "GROWTH_" in l:
                growth = re.search(r"GROWTH_(\w+)", l).group(1)
            elif "GENDER_" in l:
                gender = GENDER[re.search(r"(GENDER_\w+)", l).group(1)]
            elif l.startswith("tmhm"):
                tm = [ident(t.strip()) for t in l[4:].split(",") if t.strip()]
        out[c] = dict(stats=stats, types=types, catch=catch, exp=exp, growth=growth, gender=gender, tm=tm)
    return out


TYPE_NAME = {"PSYCHIC_TYPE": "PSYCHIC"}


def evos_attacks():
    text = read(os.path.join(GOLD, "data/pokemon/evos_attacks.asm"))
    out = {}
    cur = None
    phase = 0
    for raw in text.splitlines():
        m = re.match(r"(\w+)EvosAttacks:", raw.strip())
        if m:
            cur = m.group(1)
            out[cur] = {"evos": [], "learn": []}
            phase = 0
            continue
        if cur is None:
            continue
        l = strip(raw)
        if not l.startswith("db"):
            continue
        parts = [p.strip() for p in l[2:].split(",")]
        if parts == ["0"]:
            phase += 1
            continue
        if phase == 0:
            kind = parts[0].replace("EVOLVE_", "").lower()
            out[cur]["evos"].append((kind, parts[1:]))
        elif phase == 1:
            out[cur]["learn"].append((int(parts[0]), ident(parts[1])))
    return out


def label_for(const):
    # The labels in evos_attacks are the species names in CamelCase (Bulbasaur, NidoranF, MrMime, HoOh...).
    special = {"NIDORAN_F": "NidoranF", "NIDORAN_M": "NidoranM", "MR__MIME": "MrMime", "FARFETCH_D": "FarfetchD", "HO_OH": "HoOh", "PORYGON2": "Porygon2"}
    if const in special:
        return special[const]
    return "".join(p.capitalize() for p in const.split("_"))


# ---------- moves ----------

def moves():
    consts = []
    for line in read(os.path.join(GOLD, "constants/move_constants.asm")).splitlines():
        m = re.match(r"\s*const (\w+)\s*;\s*([0-9a-f]+)", line)
        if m and m.group(1) != "NO_MOVE":
            consts.append(m.group(1))
        if len(consts) == 251:
            break
    mnames = names(os.path.join(GOLD, "data/moves/names.asm"), "li")
    crit = set(re.findall(r"db (\w+)", read(os.path.join(GOLD, "data/moves/critical_hit_moves.asm"))))
    prio = {e: int(p) for e, p in re.findall(r"db (EFFECT_\w+),\s*(\d+)", read(os.path.join(GOLD, "data/moves/effects_priorities.asm")))}
    out = []
    for line in read(os.path.join(GOLD, "data/moves/moves.asm")).splitlines():
        m = re.match(r"\s*move (\w+),\s*(EFFECT_\w+),\s*(\d+),\s*(\w+),\s*(\d+),\s*(\d+),\s*(\d+)", line)
        if not m:
            continue
        c, eff, power, typ, acc, pp, chance = m.groups()
        i = len(out)
        out.append(dict(id=ident(c), name=mnames[i], effect=eff.replace("EFFECT_", ""), power=int(power), type=TYPE_NAME.get(typ, typ),
                        acc=int(acc), pp=int(pp), chance=int(chance), prio=prio.get(eff, 1), crit=c in crit))
    assert len(out) == 251, len(out)
    return out



# ---------- text ----------

def clean(t):
    t = t.replace("#MON", "POKéMON").replace("#", "POKé").replace("<PKMN>", "POKéMON").replace("<PLAYER>", "{player}").replace("<RIVAL>", "{rival}")
    t = t.replace("<USER>", "").replace("<TARGET>", "").replace("<BSP>", " ").replace("@", "").replace("<……>", "...").replace("……", "...")
    t = re.sub(r"<[A-Z_]+>", "", t)
    return re.sub(r"\s+", " ", t).strip()


def text_blocks(folder):
    """Every text label in a game's text/*.asm (and data/text) -> its paragraphs."""
    out = {}
    paths = []
    for d in ("text", "data/text"):
        root = os.path.join(folder, d)
        if os.path.isdir(root):
            for f in sorted(os.listdir(root)):
                if f.endswith(".asm"):
                    paths.append(os.path.join(root, f))
    for path in paths:
        label = None
        paras, cur = [], []
        def flush():
            if cur:
                joined = ""
                for frag in cur:
                    joined += frag if (not joined or joined.endswith("-")) else " " + frag
                paras.append(clean(joined))
                cur.clear()
        for raw in read(path).splitlines():
            m = re.match(r"^(_?\w+)::?\s*$", raw)
            if m:
                if label:
                    flush()
                    out[label] = [p for p in paras if p]
                label, paras, cur = m.group(1), [], []
                continue
            l = raw.strip()
            m = re.match(r'(text|line|cont|para|next)\s+"(.*)"', l)
            if m:
                kind, body = m.groups()
                if kind == "para":
                    flush()
                cur.append(body)
                continue
            if l.startswith("text_ram") or l.startswith("text_decimal") or l.startswith("text_bcd") or l.startswith("text_start"):
                cur.append("...")
            if l in ("done", "prompt", "text_end"):
                flush()
        if label:
            flush()
            out[label] = [p for p in paras if p]
    return out


def script_texts(folder, mapname):
    """In a map's script: each text label -> the text_far labels it shows (in order)."""
    path = os.path.join(folder, "scripts", mapname + ".asm")
    if not os.path.exists(path):
        return {}
    out, label = {}, None
    for raw in read(path).splitlines():
        m = re.match(r"^(\.?\w+):+", raw)
        if m and not raw.startswith("\t"):
            # Local labels (.PreBattleText) belong to the text they're in.
            if not m.group(1).startswith("."):
                label = m.group(1)
                out.setdefault(label, [])
            continue
        m = re.search(r"text_far (_\w+)", raw)
        if m and label:
            out[label].append(m.group(1))
        m = re.search(r"ld hl, (\w+Text\w*)", raw)
        if m and label:
            out[label].append("@" + m.group(1))
    return out


def resolve(label, scripts, texts, depth=0):
    """A text label from a map's pointers to its paragraphs (following text_far and nested labels)."""
    if depth > 3:
        return []
    for ref in scripts.get(label, []):
        if ref.startswith("@"):
            got = resolve(ref[1:], scripts, texts, depth + 1)
        else:
            got = texts.get(ref, [])
        if got:
            return got
    return texts.get("_" + label, [])


# ---------- items ----------

def items():
    consts = []
    for line in read(os.path.join(GOLD, "constants/item_constants.asm")).splitlines():
        m = re.match(r"\s*const (\w+)\s*;\s*([0-9a-f]+)", line)
        if m and m.group(1) != "NO_ITEM":
            consts.append(m.group(1))
    inames = names(os.path.join(GOLD, "data/items/names.asm"), "li")
    attrs = []
    for line in read(os.path.join(GOLD, "data/items/attributes.asm")).splitlines():
        l = strip(line)
        if l.startswith("item_attribute"):
            parts = [x.strip() for x in l[len("item_attribute"):].split(",")]
            attrs.append((parts[0], parts[4]))
    rows = []
    for i, c in enumerate(consts):
        if i >= len(attrs) or i >= len(inames) or c.startswith("ITEM_") or c.startswith("TM") or c.startswith("HM"):
            continue
        price, pocket = attrs[i]
        price = int(price.replace("$", ""), 16) if price.startswith("$") else int(price)
        name = clean(inames[i])
        if "TERU-SAMA" in name or not name:
            continue
        rows.append((ident(c), name, price if price != 0x9999 else 0, pocket))
    return rows


def marts():
    text = read(os.path.join(YELLOW, "data/items/marts.asm"))
    out = []
    for label, items_ in re.findall(r"(\w+)ClerkText::\s*\n\s*script_mart ([^\n]+)", text):
        if label.startswith("Unused"):
            continue
        city = re.sub(r"(Mart|Mart\dF|Lobby|Mart\dF)$", "", label)
        city = re.sub(r"Mart\dF", "", city).replace("Clerk1", "").replace("Clerk2", "")
        place = {"Viridian": "viridian_city", "Pewter": "pewter_city", "Cerulean": "cerulean_city", "Vermilion": "vermilion_city", "Lavender": "lavender_town",
                 "Celadon": "celadon_city", "Fuchsia": "fuchsia_city", "Cinnabar": "cinnabar_island", "Saffron": "saffron_city", "IndigoPlateau": "indigo_plateau"}
        key = next((v for k, v in place.items() if label.startswith(k)), ident(label))
        floor = re.search(r"Mart(\d)F(Clerk\d)?", label)
        mid = key + ("_" + floor.group(1) + "f" + (floor.group(2)[-1] if floor.group(2) else "") if floor else "")
        ids = [ident(x.strip()) for x in items_.split(",")]
        ids = [x.replace("parlyz_heal", "parlyz_heal").replace("thunder_stone", "thunderstone").replace("x_defend", "x_defend") for x in ids]
        out.append((mid, key, ids))
    return out


# ---------- wild ----------

GEN1_GRASS = [51, 51, 39, 25, 25, 25, 13, 13, 11, 3]


def gen1_tables(folder, version=None):
    """Map const -> {'grass': (rate, [(level, species)]), 'water': ...} from a Gen 1 game, for Red or Blue where they differ."""
    labels = re.findall(r"dw (\w+)WildMons", read(os.path.join(folder, "data/wild/grass_water.asm")))
    consts = re.findall(r"map_const (\w+),", read(os.path.join(folder, "constants/map_constants.asm")))
    pointers = list(zip(labels, consts))
    out = {}
    for label, mapc in pointers:
        if label == "Nothing":
            continue
        path = os.path.join(folder, "data/wild/maps", label + ".asm")
        if not os.path.exists(path):
            continue
        lines = read(path).splitlines()
        keep = True
        kind = None
        tables = {}
        for raw in lines:
            l = strip(raw)
            if l.startswith("IF DEF(_RED)"):
                keep = version in (None, "red")
                continue
            if l.startswith("IF DEF(_BLUE)"):
                keep = version in (None, "blue")
                continue
            if l.startswith("ENDC"):
                keep = True
                continue
            m = re.match(r"def_(grass|water)_wildmons (\d+)", l)
            if m:
                kind = m.group(1)
                tables[kind] = (int(m.group(2)), [])
                continue
            m = re.match(r"db\s+(\d+),\s*(\w+)", l)
            if m and kind and keep:
                tables[kind][1].append((int(m.group(1)), ident(m.group(2))))
        out[ident(mapc)] = tables
    return out


def wild():
    yellow = gen1_tables(YELLOW)
    rows = []
    have = set()
    for place, t in yellow.items():
        for kind, (rate, slots) in t.items():
            if rate and slots:
                have.update(s for _, s in slots)
    # Yellow's Super Rod and Good Rod.
    rods = {}
    for line in read(os.path.join(YELLOW, "data/wild/super_rod.asm")).splitlines():
        m = re.match(r"\s*db (\w+), (.*)", line)
        if m and m.group(1) != "-1":
            parts = [p.strip() for p in m.group(2).split(",")]
            slots = [(int(parts[i + 1]), ident(parts[i])) for i in range(0, len(parts) - 1, 2)]
            rods[ident(m.group(1))] = slots
            have.update(s for _, s in slots)
    have.update(["magikarp", "goldeen", "poliwag"])
    # What Yellow lacks, from Red's and Blue's grass and water, in the same places at the same odds.
    extra = {}
    missing_after = set()
    for version in ("red", "blue"):
        rb = gen1_tables(RED, version)
        for place, t in rb.items():
            for kind, (rate, slots) in t.items():
                for i, (lv, sp) in enumerate(slots):
                    if sp in have:
                        continue
                    w = GEN1_GRASS[i] if i < len(GEN1_GRASS) else 3
                    extra.setdefault((place, kind), {}).setdefault((sp, lv), 0)
                    extra[(place, kind)][(sp, lv)] = max(extra[(place, kind)][(sp, lv)], w)
    for place, t in yellow.items():
        for kind, (rate, slots) in t.items():
            if not rate or not slots:
                continue
            ws = [(sp, lv, GEN1_GRASS[i] if i < 10 else 3) for i, (lv, sp) in enumerate(slots)]
            for (sp, lv), w in sorted(extra.get((place, kind), {}).items()):
                ws.append((sp, lv, w))
            rows.append((place, kind, "any", rate, ws))
    # Red/Blue-only species in places Yellow has no table for (rare): their own table.
    for (place, kind), add in extra.items():
        if (place, kind) not in {(r[0], r[1]) for r in rows}:
            rb_rate = 25
            rows.append((place, kind, "any", rb_rate, [(sp, lv, w) for (sp, lv), w in sorted(add.items())]))
    for place, slots in rods.items():
        rows.append((place, "super_rod", "any", 0, [(sp, lv, 64) for lv, sp in slots]))
    rows.append(("*kanto", "old_rod", "any", 0, [("magikarp", 5, 256)]))
    rows.append(("*kanto", "good_rod", "any", 0, [("goldeen", 10, 128), ("poliwag", 10, 128)]))
    # Silver's Johto: grass by morning, day and night, and water.
    silver_lines = read(os.path.join(GOLD, "data/wild/johto_grass.asm")).splitlines()
    grass_w = [30, 30, 20, 10, 5, 4, 1]
    place, rates, times, cur, keep = None, None, {}, None, True
    for raw in silver_lines:
        l = strip(raw)
        if l.startswith("IF DEF(_GOLD)"):
            keep = False
            continue
        if l.startswith("IF DEF(_SILVER)") or l.startswith("ELIF DEF(_SILVER)") or l.startswith("ELSE"):
            keep = True
            continue
        if l.startswith("ENDC"):
            keep = True
            continue
        m = re.match(r"def_grass_wildmons (\w+)", l)
        if m:
            place, times, cur = ident(m.group(1)), {"morn": [], "day": [], "nite": []}, None
            continue
        m = re.match(r"db (\d+) percent, (\d+) percent, (\d+) percent", l)
        if m:
            rates = [int(x) for x in m.groups()]
            continue
        c = raw.strip().lstrip(";").strip()
        if c in ("morn", "day", "nite"):
            cur = c
            continue
        m = re.match(r"db (\d+), (\w+)", l)
        if m and cur and keep and place:
            times[cur].append((int(m.group(1)), ident(m.group(2))))
            continue
        if l.startswith("end_grass_wildmons") and place:
            for i, tod in enumerate(("morn", "day", "nite")):
                slots = times[tod][:7]
                rows.append((place, "grass", tod, round(rates[i] * 256 / 100), [(sp, lv, grass_w[j] * 256 // 100) for j, (lv, sp) in enumerate(slots)]))
            place = None
    water_w = [60, 30, 10]
    place = None
    keep = True
    for raw in read(os.path.join(GOLD, "data/wild/johto_water.asm")).splitlines():
        l = strip(raw)
        if l.startswith("IF DEF(_GOLD)"):
            keep = False
            continue
        if l.startswith("IF DEF(_SILVER)") or l.startswith("ELIF DEF(_SILVER)") or l.startswith("ELSE") or l.startswith("ENDC"):
            keep = True
            continue
        if not keep:
            continue
        m = re.match(r"def_water_wildmons (\w+)", l)
        if m:
            place, slots, rate = ident(m.group(1)), [], 0
            continue
        m = re.match(r"db (\d+) percent$", l)
        if m and place:
            rate = int(m.group(1))
            continue
        m = re.match(r"db (\d+), (\w+)", l)
        if m and place:
            slots.append((int(m.group(1)), ident(m.group(2))))
            continue
        if l.startswith("end_water_wildmons") and place:
            rows.append((place, "water", "any", round(rate * 256 / 100), [(sp, lv, water_w[j] * 256 // 100) for j, (lv, sp) in enumerate(slots[:3])]))
            place = None
    allk = set()
    for r in rows:
        if not r[0].startswith("*") and r[0] in KANTO_PLACES:
            allk.update(sp for sp, _, _ in r[4])
    return rows, allk


# ---------- places ----------

KANTO_PLACES = set()


def places():
    out = []
    # Kanto (Yellow): towns, routes and the dungeons with wild Pokémon, with their music.
    songs = re.findall(r"db (MUSIC_\w+), BANK\(\w+\) ; (\w+)", read(os.path.join(YELLOW, "data/maps/songs.asm")))
    tnames = {lab: clean(n) for lab, n in re.findall(r'(\w+)Name:\s+db "([^"]+)"', read(os.path.join(YELLOW, "data/maps/names.asm")))}
    outdoor = re.findall(r"outdoor_map\s+\d+,\s*\d+,\s*(\w+)Name", read(os.path.join(YELLOW, "data/maps/town_map_entries.asm")))
    wildmaps = set(gen1_tables(YELLOW).keys())
    dungeon_names = {"viridian_forest": "ViridianForest", "mt_moon": "MountMoon", "rock_tunnel": "RockTunnel", "pokemon_tower": "PokemonTower",
                     "seafoam_islands": "SeafoamIslands", "victory_road": "VictoryRoad", "digletts_cave": "DiglettsCave", "pokemon_mansion": "PokemonMansion",
                     "safari_zone": "SafariZone", "cerulean_cave": "CeruleanCave", "power_plant": "PowerPlant", "ss_anne": "SSAnne", "silph_co": "SilphCo",
                     "rocket_hideout": "RocketHQ", "underground_path": "UndergroundPath"}
    for i, (music, mapc) in enumerate(songs):
        pid = ident(mapc)
        if i < len(outdoor):
            if mapc.startswith("UNUSED"):
                continue
            name = tnames.get(outdoor[i], pid.upper().replace("_", " "))
        elif pid in wildmaps:
            base = next((v for k, v in dungeon_names.items() if pid.startswith(k)), None)
            name = tnames.get(base, pid.upper().replace("_", " ")) if base else pid.upper().replace("_", " ")
        else:
            continue
        KANTO_PLACES.add(pid)
        out.append((pid, "kanto", name, ident(music.replace("MUSIC_", "")), pid if pid in wildmaps else "-"))
    # Johto (Silver): every outdoor map and dungeon, named by its landmark, with its music.
    lnames = {}
    for lab, n in re.findall(r'(\w+)Name:\s+db "([^"]+)"', read(os.path.join(GOLD, "data/maps/landmarks.asm"))):
        lnames[lab] = clean(n)
    lconsts = [m for m in re.findall(r"const (LANDMARK_\w+)", read(os.path.join(GOLD, "constants/landmark_constants.asm")))]
    llabels = re.findall(r"landmark\s+-?\d+,\s*-?\d+,\s*(\w+)Name", read(os.path.join(GOLD, "data/maps/landmarks.asm")))
    land = {c: lnames.get(l, "") for c, l in zip(lconsts, llabels)}
    johto_first = None
    for m in re.finditer(r"map (\w+), TILESET_\w+, (\w+), (LANDMARK_\w+), (MUSIC_\w+)", read(os.path.join(GOLD, "data/maps/maps.asm"))):
        mapl, env, lm, music = m.groups()
        if env not in ("TOWN", "ROUTE", "CAVE", "DUNGEON"):
            continue
        pid = re.sub(r"(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])", "_", mapl).lower()
        pid = re.sub(r"(?<=[a-z])(?=\d)", "_", pid)
        # As the games' constants spell them: UNION_CAVE_1F, UNION_CAVE_B1F.
        pid = re.sub(r"_(b_)?(\d+)_f\b", lambda m: "_" + ("b" if m.group(1) else "") + m.group(2) + "f", pid)
        pid = re.sub(r"_(b_)?(\d+)_f_", lambda m: "_" + ("b" if m.group(1) else "") + m.group(2) + "f_", pid)
        region = "johto" if lm in land and lconsts.index(lm) < lconsts.index("LANDMARK_PALLET_TOWN") else "kanto"
        if region == "kanto":
            continue
        out.append((pid, "johto", land.get(lm, pid.upper()), ident(music.replace("MUSIC_", "")), pid))
    return out


def write_tsv(name, header, rows):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as f:
        f.write("# " + "\t".join(header) + "\n")
        for r in rows:
            f.write("\t".join(str(x) for x in r) + "\n")


# ---------- trainers and people (Kanto, from Yellow) ----------

LEADER_BADGES = {"BROCK": "BOULDERBADGE", "MISTY": "CASCADEBADGE", "LT_SURGE": "THUNDERBADGE", "ERIKA": "RAINBOWBADGE", "KOGA": "SOULBADGE",
                 "SABRINA": "MARSHBADGE", "BLAINE": "VOLCANOBADGE", "GIOVANNI": "EARTHBADGE"}
ELITE = {"LORELEI", "BRUNO", "AGATHA", "LANCE"}


def trainer_classes():
    consts = re.findall(r"trainer_const (\w+)", read(os.path.join(YELLOW, "constants/trainer_constants.asm")))[1:]
    cnames = names(os.path.join(YELLOW, "data/trainers/names.asm"), "li")
    money = [int(x) // 100 for x in re.findall(r"pic_money \w+,\s*(\d+)", read(os.path.join(YELLOW, "data/trainers/pic_pointers_money.asm")))]
    labels = re.findall(r"dw (\w+)Data", read(os.path.join(YELLOW, "data/trainers/parties.asm")))
    text = read(os.path.join(YELLOW, "data/trainers/parties.asm"))
    parties = {}
    for lab in labels:
        m = re.search(r"^" + lab + r"Data:\n(.*?)(?=^\w+Data:|\Z)", text, re.S | re.M)
        out = []
        if m:
            for line in m.group(1).splitlines():
                l = strip(line)
                if not l.startswith("db"):
                    continue
                parts = [p.strip() for p in l[2:].split(",")]
                if parts[-1] == "0":
                    parts = parts[:-1]
                if parts[0] == "$FF":
                    mons = [(int(parts[i]), ident(parts[i + 1])) for i in range(1, len(parts) - 1, 2)]
                else:
                    lv = int(parts[0])
                    mons = [(lv, ident(x)) for x in parts[1:]]
                out.append(mons)
        parties[lab] = out
    classes = {}
    for i, c in enumerate(consts):
        if i >= len(labels):
            break
        classes[c] = dict(name=clean(cnames[i]) if i < len(cnames) else c, money=money[i] if i < len(money) else 10, parties=parties.get(labels[i], []))
    return classes


def kanto_people():
    classes = trainer_classes()
    texts = text_blocks(YELLOW)
    places_set = KANTO_PLACES
    trainers, people, balls = [], [], []
    objdir = os.path.join(YELLOW, "data/maps/objects")
    for f in sorted(os.listdir(objdir)):
        mapname = f[:-4]
        obj = read(os.path.join(objdir, f))
        m = re.search(r"def_warps_to (\w+)", obj)
        mapc = m.group(1) if m else ""
        place = ident(mapc)
        scripts = script_texts(YELLOW, mapname)
        sc = read(os.path.join(YELLOW, "scripts", mapname + ".asm")) if os.path.exists(os.path.join(YELLOW, "scripts", mapname + ".asm")) else ""
        tptr = dict((c, l) for l, c in re.findall(r"dw_const (\w+),\s*(TEXT_\w+)", sc))
        headers = re.findall(r"trainer (EVENT_\w+),\s*(\d+),\s*(\w+),\s*(\w+),\s*(\w+)", sc)
        n_trainer = 0
        n_person = 0
        n_ball = 0
        for om in re.finditer(r"object_event\s+(\d+),\s*(\d+),\s*(SPRITE_\w+),\s*(\w+),\s*(\w+),\s*(TEXT_\w+)(?:,\s*(\w+)(?:,\s*(\d+))?)?", obj):
            x, y, sprite, move, facing, tconst, opp, idx = om.groups()
            sprite = sprite.replace("SPRITE_", "").lower()
            label = tptr.get(tconst, "")
            if opp and opp.startswith("OPP_"):
                cls = opp[4:]
                info = classes.get(cls)
                if not info:
                    continue
                pi = int(idx) - 1
                party = info["parties"][pi] if 0 <= pi < len(info["parties"]) else []
                if not party:
                    continue
                key = f"{place}_{n_trainer}"
                n_trainer += 1
                if cls in LEADER_BADGES or cls in ELITE:
                    stem = mapname + cls.title().replace("_", "")
                    tl = {k: v for k, v in texts.items() if k.startswith("_" + mapname)}
                    def pick(*words):
                        for k, v in tl.items():
                            if all(w in k for w in words) and v:
                                return v
                        return []
                    leader = cls.replace("LT_SURGE", "LtSurge").title().replace("_", "")
                    before = pick(leader, "PreBattle") or pick("PreBattle") or resolve(label, scripts, texts)
                    defeat = pick(leader, "Received", "Badge") or pick(leader, "Badge") or pick(leader, "Received") or pick("EndBattle") or pick("Defeat") or ["..."]
                    after = pick(leader, "Advice") or pick(leader, "PostBattle") or pick(leader, "Info") or ["..."]
                    sight = 0
                    gym = place.endswith("_gym")
                    badge = LEADER_BADGES.get(cls, "-") if gym else "-"
                    title = ("LEADER " if cls in LEADER_BADGES and gym else "ROCKET BOSS " if cls in LEADER_BADGES else "ELITE FOUR ") + info["name"]
                    if cls in LEADER_BADGES:
                        sprite = "leader_" + cls.lower()
                else:
                    h = headers[n_trainer - 1] if n_trainer - 1 < len(headers) else None
                    sight = int(h[1]) if h else 3
                    before = resolve(h[2], scripts, texts) if h else []
                    defeat = resolve(h[3], scripts, texts) if h else []
                    after = resolve(h[4], scripts, texts) if h else []
                    badge = "-"
                    title = info["name"]
                trainers.append((key, place, title, sprite, facing.lower(), sight, info["money"], ",".join(f"{sp}:{lv}" for lv, sp in party), badge,
                                 " | ".join(before) or "-", " | ".join(defeat) or "-", " | ".join(after) or "-", x, y))
            elif sprite == "poke_ball" and opp and not opp.isdigit():
                balls.append((f"{place}_i{n_ball}", place, opp.lower(), x, y))
                n_ball += 1
            elif not opp or not opp.isdigit():
                lines = resolve(label, scripts, texts) if label else []
                if not lines or sprite in ("poke_ball", "boulder", "fossil", "paper", "clipboard", "snorlax", "pokedex", "lying_old_man_unused_1"):
                    continue
                key = f"{place}_p{n_person}"
                n_person += 1
                people.append((key, place, sprite, facing.lower(), move.lower(), " | ".join(lines), x, y))
    return trainers, people, balls


def dex_entries():
    """Gold's Pokédex entries, in dex order: category, height, weight and the entry's text."""
    base = os.path.join(GOLD, "data/pokemon")
    files = dict(re.findall(r"(\w+)PokedexEntry::\s+INCLUDE \"([^\"]+)\"", read(os.path.join(base, "dex_entries_gold.asm"))))
    order = re.findall(r"dw (\w+)PokedexEntry", read(os.path.join(base, "dex_entry_pointers.asm")))
    out = []
    for i, lab in enumerate(order):
        src = read(os.path.join(GOLD, files[lab]))
        cat = re.search(r'db "([^"@]*)@?" ; species name', src).group(1)
        h, w = map(int, re.search(r"dw (\d+), (\d+) ; height, weight", src).groups())
        lines = re.findall(r'(?:db|next|page)\s+"([^"]*)"', src.split("height, weight", 1)[1])
        text = clean(" ".join(l.replace("@", "") for l in lines)).replace("- ", "")
        out.append((i + 1, cat, f"{h // 100}'{h % 100:02d}\"", f"{w / 10:.1f}", text))
    return out


CRYSTAL = os.path.join(os.path.dirname(YELLOW), "pokecrystal")


def tm_list():
    """The TMs and HMs of Gen 1 (Yellow) and Gen 2 (Gold): one item per move, with its number in each."""
    items = {}
    for gen, root in (("1", YELLOW), ("2", GOLD)):
        src = read(os.path.join(root, "constants/item_constants.asm"))
        n = h = 0
        for kind, mv in re.findall(r"^\s*add_(tm|hm)\s+(\w+)", src, re.M):
            if kind == "tm":
                n += 1
                num = f"TM{n:02d}"
            else:
                h += 1
                num = f"HM{h:02d}"
            key = f"{kind}_{ident(mv)}"
            row = items.setdefault(key, [key, kind, ident(mv), "-", "-"])
            row[3 if gen == "1" else 4] = num
    return list(items.values())


def tmhm_gen1():
    """What each of the 151 can learn from Gen 1's TMs and HMs (added to Gen 2's lists)."""
    out = []
    for f in sorted(glob.glob(os.path.join(YELLOW, "data/pokemon/base_stats/*.asm"))):
        src = read(f)
        dex = re.search(r"db DEX_(\w+)", src)
        body = re.search(r"tmhm(.*?); end", src, re.S)
        if not dex or not body:
            continue
        moves = [ident(m) for m in re.findall(r"[A-Z][A-Z0-9_]+", body.group(1))]
        out.append((ident(dex.group(1)), ",".join(moves)))
    return out


def camel_to_place(root):
    """MapName (as in the scripts and attributes) -> our place id, through the map constants."""
    out = {}
    for c in re.findall(r"map_const (\w+),", read(os.path.join(root, "constants/map_constants.asm"))):
        out[c.replace("_", "").lower()] = ident(c)
    return out


def gifts():
    """The TMs and HMs people give in Kanto: the gym leaders' TMs and the HMs (by the place they're given in)."""
    names = camel_to_place(YELLOW)
    out = []
    for f in sorted(glob.glob(os.path.join(YELLOW, "scripts/*.asm"))):
        place = names.get(os.path.splitext(os.path.basename(f))[0].lower())
        if not place:
            continue
        for kind, mv in re.findall(r"(?:lb bc|ld a),\s*(TM|HM)_(\w+)", read(f)):
            out.append((place, f"{kind.lower()}_{ident(mv)}"))
    return sorted(set(out))


def town_map():
    """The Pokégear's town maps (Crystal): each landmark's spot and name, which landmark each place is in, and the routes between them."""
    consts = re.findall(r"const (LANDMARK_\w+)", read(os.path.join(CRYSTAL, "constants/landmark_constants.asm")))
    kanto_from = consts.index("LANDMARK_PALLET_TOWN")
    src = read(os.path.join(CRYSTAL, "data/maps/landmarks.asm"))
    marks = re.findall(r"landmark\s+(-?\d+),\s*(-?\d+),\s*(\w+)", src)
    names = {k: v.replace("¯", " ").replace("<BSP>", " ").replace("@", "") for k, v in re.findall(r"(\w+):\s+db \"([^\"]*)\"", src)}
    points = {}
    for i, (x, y, label) in enumerate(marks):
        if i == 0 or i >= len(consts):
            continue
        key = consts[i][len("LANDMARK_"):].lower()
        name = names.get(label, key.upper().replace("_", " "))
        kind = "route" if "ROUTE" in name else "town" if any(w in name for w in ("CITY", "TOWN", "ISLAND", "PLATEAU")) else "spot"
        points[key] = (key, "kanto" if i >= kanto_from else "johto", int(x), int(y), kind, name.strip())
    places = camel_to_place(CRYSTAL)
    of = {}
    for m, lm in re.findall(r"map (\w+), \w+, \w+, LANDMARK_(\w+)", read(os.path.join(CRYSTAL, "data/maps/maps.asm"))):
        pid = places.get(m.lower())
        if pid:
            of[pid] = lm.lower()
    edges = set()
    cur = None
    for line in read(os.path.join(CRYSTAL, "data/maps/attributes.asm")).splitlines():
        a = re.match(r"\s*map_attributes (\w+),", line)
        if a:
            cur = places.get(a.group(1).lower())
            continue
        c = re.match(r"\s*connection \w+, (\w+),", line)
        if c and cur:
            other = places.get(c.group(1).lower())
            la, lb = of.get(cur), of.get(other)
            if la and lb and la != lb:
                edges.add(tuple(sorted((la, lb))))
    return list(points.values()), of, sorted(edges)



def table_rows(name):
    path = os.path.join(OUT, name)
    return [l.rstrip("\n").split("\t") for l in open(path, encoding="utf-8") if not l.startswith("#") and l.strip()]


def map_sizes():
    """Each Kanto map's size in steps (the games give it in 2x2-step blocks)."""
    out = []
    for c, w, h in re.findall(r"map_const (\w+),\s*(\d+),\s*(\d+)", read(os.path.join(YELLOW, "constants/map_constants.asm"))):
        out.append((ident(c), int(w) * 2, int(h) * 2))
    return out


# ---------- write ----------

def main():
    os.makedirs(OUT, exist_ok=True)
    consts = species_constants()
    assert len(consts) == 251, len(consts)
    pnames = names(os.path.join(GOLD, "data/pokemon/names.asm"), "dname")
    stats = base_stats(consts)
    ea = evos_attacks()
    rows = []
    for i, c in enumerate(consts):
        s = stats[c]
        e = ea[label_for(c)]
        evos = []
        for kind, args in e["evos"]:
            if kind == "level":
                evos.append(f"level:{args[0]}:{ident(args[1])}")
            elif kind == "item":
                evos.append(f"item:{ident(args[0])}:{ident(args[1])}")
            elif kind == "trade":
                evos.append(f"trade:{'-' if args[0] == '-1' else ident(args[0])}:{ident(args[1])}")
            elif kind == "happiness":
                evos.append(f"happiness:{args[0].replace('TR_', '').lower()}:{ident(args[1])}")
            elif kind == "stat":
                evos.append(f"stat:{args[0]}:{args[1].replace('ATK_', '').replace('_DEF', '').lower()}:{ident(args[2])}")
        learn = ",".join(f"{lv}:{mv}" for lv, mv in e["learn"])
        t1, t2 = [TYPE_NAME.get(t, t) for t in s["types"]]
        rows.append("\t".join(map(str, [i + 1, ident(c), pnames[i], t1, t2 if t2 != t1 else "-", *s["stats"], s["catch"], s["exp"], s["growth"], s["gender"],
                                         "|".join(evos) or "-", learn, ",".join(s["tm"]) or "-"])))
    with open(os.path.join(OUT, "species.tsv"), "w", encoding="utf-8") as f:
        f.write("# dex\tid\tname\ttype1\ttype2\thp\tatk\tdef\tspe\tspa\tspd\tcatch\tbase_exp\tgrowth\tgender\tevolutions\tlearnset\ttmhm\n")
        f.write("\n".join(rows) + "\n")
    mv = moves()
    with open(os.path.join(OUT, "moves.tsv"), "w", encoding="utf-8") as f:
        f.write("# id\tname\teffect\tpower\ttype\taccuracy\tpp\tchance\tpriority\thigh_crit\n")
        for m in mv:
            f.write("\t".join(map(str, [m["id"], m["name"], m["effect"], m["power"], m["type"], m["acc"], m["pp"], m["chance"], m["prio"],
                                        1 if m["crit"] else 0])) + "\n")
    print(len(rows), "species,", len(mv), "moves")
    it = items()
    write_tsv("items.tsv", ["id", "name", "price", "pocket"], it)
    write_tsv("marts.tsv", ["mart", "place", "items"], [(m, p, ",".join(i)) for m, p, i in marts()])
    pl = places()
    write_tsv("places.tsv", ["id", "region", "name", "music", "wild"], pl)
    wrows, kanto_species = wild()
    write_tsv("wild.tsv", ["place", "method", "time", "rate_per_256", "slots species:level:weight"],
              [(p, k, t, r, ",".join(f"{sp}:{lv}:{w}" for sp, lv, w in sl)) for p, k, t, r, sl in wrows])
    tr, pe, balls = kanto_people()
    write_tsv("field_items.tsv", ["key", "place", "item", "x", "y"], balls)
    write_tsv("sizes.tsv", ["place", "width_steps", "height_steps"], map_sizes())
    write_tsv("tms.tsv", ["id", "kind", "move", "gen1_number", "gen2_number"], tm_list())
    write_tsv("tmhm1.tsv", ["species", "moves"], tmhm_gen1())
    write_tsv("gifts.tsv", ["place", "item"], gifts())
    pts, of, edges = town_map()
    write_tsv("map_points.tsv", ["landmark", "region", "x", "y", "kind", "name"], pts)
    # Our places (Yellow's Kanto names, Crystal's Johto) to landmarks; Gen 1's own places fall back to their town.
    fallback = {"pokemon_tower": "lavender_town", "safari_zone": "fuchsia_city", "cerulean_cave": "cerulean_city", "pokemon_mansion": "cinnabar_island",
                "silph_co": "saffron_city", "rocket_hideout": "celadon_city", "ss_anne": "vermilion_city", "seafoam": "seafoam_islands",
                "mt_moon": "mt_moon", "rock_tunnel": "rock_tunnel", "victory_road": "victory_road", "digletts_cave": "digletts_cave",
                "power_plant": "power_plant", "indigo_plateau": "indigo_plateau", "viridian_forest": "route_2", "underground": "saffron_city",
                "pokemon_fan_club": "vermilion_city", "game_corner": "celadon_city", "bike_shop": "cerulean_city", "museum": "pewter_city"}
    rows = []
    for r in table_rows("places.tsv"):
        pid = r[0]
        lm = of.get(pid)
        if not lm:
            for k, v in fallback.items():
                if pid.startswith(k) or k in pid:
                    lm = v
                    break
        if not lm:
            for k in sorted((p[0] for p in pts), key=len, reverse=True):
                if pid.startswith(k):
                    lm = k
                    break
        if lm:
            rows.append((pid, lm))
    write_tsv("place_landmarks.tsv", ["place", "landmark"], rows)
    write_tsv("map_edges.tsv", ["a", "b"], edges)
    write_tsv("dex.tsv", ["dex", "category", "height", "weight_lbs", "text"], dex_entries())
    write_tsv("trainers.tsv", ["key", "place", "title", "sprite", "facing", "sight", "money", "party species:level", "badge", "before", "defeat", "after", "x", "y"], tr)
    write_tsv("people.tsv", ["key", "place", "sprite", "facing", "movement", "lines", "x", "y"], pe)
    print(len(tr), "trainers,", len(pe), "people")
    gen1 = {ident(c) for c in species_constants()[:151]}
    print(len(it), "items,", len(pl), "places,", len(wrows), "wild tables; Kanto wild species", len(kanto_species))
    print("not in Kanto's wild:", sorted(gen1 - kanto_species))


if __name__ == "__main__":
    main()
