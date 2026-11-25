"""
A page to compare Retromon's own creatures with Cobblemon's models, side by side in 3D, animated as the mod
animates them, with each species' stats and their sizes in blocks.

Run: python3 tools/monviewer.py <out.html>
"""

import base64, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "retromon")
DATA = os.path.join(HERE, "..", "src", "main", "resources", "retromon", "data")

FAMILIES = [("Leaf armadillos", ["bulbasaur", "ivysaur", "venusaur"]), ("Ember goats", ["charmander", "charmeleon", "charizard"]),
            ("Sea tapirs", ["squirtle", "wartortle", "blastoise"]), ("Quails", ["pidgey", "pidgeotto", "pidgeot"]),
            ("Jerboas", ["rattata", "raticate"]), ("Quill porcupines ♀", ["nidoran_f", "nidorina", "nidoqueen"]),
            ("Quill porcupines ♂", ["nidoran_m", "nidorino", "nidoking"]), ("Mongooses", ["mankey", "primeape"]), ("Shrikes", ["spearow", "fearow"]),
            ("Gliders", ["pichu", "pikachu", "raichu"]), ("Leaf grubs", ["caterpie", "metapod", "butterfree"]),
            ("Lance wasps", ["weedle", "kakuna", "beedrill"]), ("Vipers", ["ekans", "arbok"]), ("Pangolins", ["sandshrew", "sandslash"]),
            ("Moon rabbits", ["cleffa", "clefairy", "clefable"]), ("Fire fennecs", ["vulpix", "ninetales"]), ("Singing chinchillas", ["igglybuff", "jigglypuff", "wigglytuff"]),
            ("Bats", ["zubat", "golbat", "crobat"]), ("Tubers", ["oddish", "gloom", "vileplume", "bellossom"]), ("Mushroom crabs", ["paras", "parasect"]),
            ("Gnats", ["venonat", "venomoth"]), ("Star-nosed moles", ["diglett", "dugtrio"]), ("Lynxes", ["meowth", "persian"]),
            ("Platypuses", ["psyduck", "golduck"]), ("Maned wolves", ["growlithe", "arcanine"]), ("Frogs", ["poliwag", "poliwhirl", "poliwrath", "politoed"]),
            ("Tarsiers", ["abra", "kadabra", "alakazam"]), ("Gorillas", ["machop", "machoke", "machamp"]),
            ("Flytraps", ["bellsprout", "weepinbell", "victreebel"]), ("Sail jellies", ["tentacool", "tentacruel"]), ("Geodes", ["geodude", "graveler", "golem"]),
            ("Fire deer", ["ponyta", "rapidash"]), ("Salamanders", ["slowpoke", "slowbro", "slowking"]),
            ("Bolt nuts", ["magnemite", "magneton"]), ("Kingfisher", ["farfetch_d"]), ("Cassowaries", ["doduo", "dodrio"]), ("Ice seals", ["seel", "dewgong"]),
            ("Bog slimes", ["grimer", "muk"]), ("Shells", ["shellder", "cloyster"]), ("Shadow lanterns", ["gastly", "haunter", "gengar"]),
            ("Basalt wyrms", ["onix", "steelix"]), ("Koalas", ["drowzee", "hypno"]), ("Fiddler crabs", ["krabby", "kingler"]), ("Bulbs", ["voltorb", "electrode"]),
            ("Acorns", ["exeggcute", "exeggutor"]), ("Skull jackals", ["cubone", "marowak"]), ("Kangaroo", ["hitmonlee"]), ("Mantis shrimp", ["hitmonchan"]),
            ("Chameleon", ["lickitung"]), ("Puffballs", ["koffing", "weezing"]), ("Ankylosaurs", ["rhyhorn", "rhydon"]), ("Hens", ["chansey", "blissey"]),
            ("Vine ball", ["tangela"]), ("Anteater", ["kangaskhan"]), ("Sea dragons", ["horsea", "seadra", "kingdra"]), ("Fighting fish", ["goldeen", "seaking"]),
            ("Sea urchins", ["staryu", "starmie"]), ("Mime lemur", ["mr_mime"]), ("Giant sloth", ["snorlax"]),
            # Johto's (2026-11-24)
            ("Mouse-deer", ["chikorita", "bayleef", "meganium"]), ("Ember badgers", ["cyndaquil", "quilava", "typhlosion"]),
            ("River otters", ["totodile", "croconaw", "feraligatr"]), ("Marmots", ["sentret", "furret"]), ("Nightjars", ["hoothoot", "noctowl"]),
            ("Fireflies", ["ledyba", "ledian"]), ("Net-casting spiders", ["spinarak", "ariados"]), ("Torpedo rays", ["chinchou", "lanturn"]),
            ("Doves", ["togepi", "togetic"]), ("Hoopoes", ["natu", "xatu"]), ("Storm alpacas", ["mareep", "flaaffy", "ampharos"]),
            ("Water voles", ["marill", "azumarill"]), ("Stump tortoise", ["sudowoodo"]), ("Maple keys", ["hoppip", "skiploom", "jumpluff"]),
            ("Kinkajou", ["aipom"]), ("Sunchokes", ["sunkern", "sunflora"]), ("Emperor darner", ["yanma"]), ("Mudskippers", ["wooper", "quagsire"]),
            ("Genets", ["eevee", "vaporeon", "jolteon", "flareon", "espeon", "umbreon"]), ("Magpie", ["murkrow"]), ("Ghost bellflower", ["misdreavus"]), ("Rune pebbles", ["unown"]),
            ("Blobfish", ["wobbuffet"]), ("Okapi", ["girafarig"]), ("Pill bugs", ["pineco", "forretress"]), ("Slow worm", ["dunsparce"]),
            ("Flying lizard", ["gligar"]), ("Warthogs", ["snubbull", "granbull"]), ("Lionfish", ["qwilfish"]), ("Tiger beetle", ["scyther", "scizor"]),
            ("Caddis grub", ["shuckle"]), ("Stag beetle", ["heracross"]), ("Wolverine", ["sneasel"]), ("Spectacled bears", ["teddiursa", "ursaring"]),
            ("Fire sea slugs", ["slugma", "magcargo"]), ("Musk oxen", ["swinub", "piloswine"]), ("Decorator crab", ["corsola"]),
            ("Archerfish to cuttlefish", ["remoraid", "octillery"]), ("Puffin", ["delibird"]), ("Flying fish", ["mantine"]), ("Blade heron", ["skarmory"]),
            ("Ember hyenas", ["houndour", "houndoom"]), ("Hippos", ["phanpy", "donphan"]), ("Paper birds", ["porygon", "porygon2"]), ("Kudu", ["stantler"]),
            ("Coati", ["smeargle"]), ("Wallabies", ["tyrogue", "hitmontop"]), ("Ermine", ["smoochum", "jynx"]), ("Numbat", ["elekid", "electabuzz"]), ("Fire salamander", ["magby", "magmar"]), ("Antlion", ["pinsir"]), ("Ankole bull", ["tauros"]), ("Oarfish", ["magikarp", "gyarados"]), ("Sea cow", ["lapras"]), ("Mimic octopus", ["ditto"]), ("Nautilus and ammonite", ["omanyte", "omastar"]), ("Trilobite and sea scorpion", ["kabuto", "kabutops"]), ("Pterosaur", ["aerodactyl"]), ("Gyrfalcon", ["articuno"]), ("Storm eagle", ["zapdos"]), ("Fire kite", ["moltres"]), ("Blue sea dragons", ["dratini", "dragonair", "dragonite"]), ("Galagos", ["mew", "mewtwo"]),
            ("Dairy yak", ["miltank"]), ("Clouded leopard", ["raikou"]), ("Volcano bison", ["entei"]), ("Crystal stag", ["suicune"]),
            ("Thorny lizards", ["larvitar", "pupitar", "tyranitar"]), ("Albatross", ["lugia"]), ("Rainbow pheasant", ["ho_oh"]), ("Moth of time", ["celebi"])]
KEEP = ("idle", "ground")


# (0.66, John's 13:27) The farm's and the pets' creatures (tools/mons11.py): their models and textures in tools/farm,
# not the game's; their own data (tools/farm/farm.tsv), with no Cobblemon model beside them.
FARM = os.path.join(HERE, "farm")
FARM_FAMILIES = [("Farm: cattle", ["moopip", "moobelle", "brawnox"]), ("Farm: sheep", ["lamblet", "fleecelle", "curlram"]),
                 ("Farm: pigs", ["oinklet", "trufflesow", "tuskhog"]), ("Farm: chickens", ["peepling", "cluckmaid", "crowcomb"]),
                 ("Farm: horses", ["foalet", "marebraid", "stormsteed"]), ("Pets: the dog", ["heelhound"]), ("Pets: the cat", ["barnpurr"])]


def resolve(identifier):
    ns, path = identifier.split(":", 1)
    if ns == "retromon" and path.startswith("textures/mons/"):
        fp = os.path.join(FARM, "textures", path[len("textures/mons/"):])
        if os.path.exists(fp) and os.path.basename(path).split(".")[0].split("_")[0] in {s for _, m in FARM_FAMILIES for s in m}: return fp
    return os.path.join(ROOT, "..", ns, path)


def data_uri(identifier):
    return "data:image/png;base64," + base64.b64encode(open(resolve(identifier), "rb").read()).decode()


def farm_rows():
    out = {}
    for line in open(os.path.join(FARM, "farm.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip(): continue
        f = line.rstrip("\n").split("\t")
        sid, name, fam, stage, sex, types, st, grows, get, does = f
        hp, atk, de, spa, spd, spe = (int(v) for v in st.split("/"))
        out[sid] = {"dex": 0, "name": name, "types": types.split(), "stats": {"HP": hp, "Attack": atk, "Defense": de, "Sp. Atk": spa, "Sp. Def": spd, "Speed": spe},
                    "catch": "-", "exp": "-", "growth": "-", "evolves": None,
                    "farm": {"line": fam, "stage": stage, "sex": sex, "grows": grows if grows != "-" else "", "get": get, "does": does}}
    # The grown ones: what they grew from.
    for _, members in FARM_FAMILIES:
        if len(members) == 3:
            for g in members[1:]:
                out[g]["farm"]["grows"] = "a " + out[members[0]]["name"] + (" (if she's a she)" if out[g]["farm"]["sex"] == "she" else " (if he's a he)")
    return out


def model(folder):
    info = json.load(open(os.path.join(folder, "info.json")))
    geo = json.load(open(os.path.join(folder, "model.geo.json")))
    anims = json.load(open(os.path.join(folder, "anim.json"))).get("animations", {}) if os.path.exists(os.path.join(folder, "anim.json")) else {}
    wanted = {info.get("idle"), info.get("ground")}
    wanted |= {p.get("anim") for p in info.get("poses", {}).values()}
    wanted |= set(info.get("actions", {}).values())
    anims = {k: v for k, v in anims.items() if k in wanted}
    g = geo["minecraft:geometry"][0]
    cubes = sum(len(b.get("cubes", [])) for b in g["bones"])
    layers = []
    for layer in info.get("layers", []):
        layers.append({"frames": [data_uri(f) for f in layer["frames"]], "fps": layer.get("fps", 8)})
    from PIL import Image
    import numpy as np
    im = Image.open(resolve(info["texture"])).convert("RGBA")
    # Texels painted: on the texture, or (for parts that only show glowing, such as flames) on a glowing layer.
    painted = np.array(im)[..., 3] > 0
    for layer in info.get("layers", []):
        for f in layer["frames"][:1]:
            g_im = Image.open(resolve(f)).convert("RGBA")
            if g_im.size == im.size:
                painted |= np.array(g_im)[..., 3] > 0
    texels = int(painted.sum())
    return {"geo": g, "anims": anims, "info": {k: info.get(k) for k in ("scale", "idle", "ground", "parts", "poses", "actions", "name", "move", "attack_move",
                                                                         "anchors", "stage")},
            "texture": data_uri(info["texture"]), "layers": layers,
            "detail": {"cubes": cubes, "bones": len(g["bones"]), "texture": f"{im.size[0]}×{im.size[1]}", "texels": texels, "animations": len(anims)}}


def species_rows():
    out = {}
    for line in open(os.path.join(DATA, "species.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        evo = f[15]
        nxt = None
        if evo and evo != "-":
            parts = evo.split(";")[0].split("|")[0].split(":")
            if parts[0] == "level":
                nxt = {"how": f"Level {int(parts[1])}", "level": int(parts[1]), "into": parts[2]}
            elif parts[0] == "item":
                nxt = {"how": parts[1].replace("_", " ").title(), "into": parts[2]}
            elif parts[0] == "happiness":
                nxt = {"how": "Friendship", "into": parts[2]}
        out[f[1]] = {"dex": int(f[0]), "name": f[2], "types": [t for t in (f[3], f[4]) if t and t != "-"],
                     "stats": {"HP": int(f[5]), "Attack": int(f[6]), "Defense": int(f[7]), "Sp. Atk": int(f[9]), "Sp. Def": int(f[10]), "Speed": int(f[8])},
                     "catch": int(f[11]), "exp": int(f[12]), "growth": f[13].replace("_", " ").title(), "evolves": nxt}
    return out


def move_rows(rows, ours):
    """Every move, with what it does, its TM or HM, and the Retromon creatures that learn it by level."""
    sys.path.insert(0, HERE)
    import movetext
    index = json.load(open(os.path.join(ROOT, "moves", "index.json")))
    tm = {}
    for line in open(os.path.join(DATA, "tms.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        tm[f[2]] = {"gs": None if f[4] == "-" else f[4], "rb": None if f[3] == "-" else f[3]}
    learn = {}
    for line in open(os.path.join(DATA, "species.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        if f[1] not in ours:
            continue
        for e in f[16].split(","):
            if ":" in e:
                lv, mv = e.split(":")
                learn.setdefault(mv, {}).setdefault(f[1], int(lv))
    out = []
    for line in open(os.path.join(DATA, "moves.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        mid = f[0]
        ix = index[mid]
        out.append({"id": mid, "n": ix["n"], "name": ix["name"], "type": ix["type"], "category": ix["category"], "power": int(f[3]),
                     "accuracy": int(f[5]), "pp": int(f[6]), "desc": movetext.describe(f[1], f[2], int(f[7]), int(f[3]), f[9] == "1"),
                     "tm": tm.get(mid), "learners": [[sid, lv] for sid, lv in sorted(learn.get(mid, {}).items(), key=lambda x: x[1])],
                     "body": ix["body"], "hit": ix.get("hit"), "hold": ix.get("hold", 0), "length": ix.get("length"), "aura": ix.get("aura")})
    return out


def effect(folder):
    info = json.load(open(os.path.join(folder, "info.json")))
    geo = json.load(open(os.path.join(folder, "model.geo.json")))["minecraft:geometry"][0]
    anims = json.load(open(os.path.join(folder, "anim.json")))["animations"]
    return {"geo": geo, "anims": anims, "info": {"scale": 1, **{k: info.get(k) for k in ("release", "hit", "hold", "length", "aura", "body", "status", "label", "loop")}},
            "texture": data_uri(info["texture"]), "layers": [{"frames": [data_uri(f) for f in l["frames"]], "fps": l.get("fps", 10)} for l in info.get("layers", [])]}


def write_moves(path):
    """The move effects, in a file of their own beside the page (fetched when first wanted)."""
    base = os.path.join(ROOT, "moves")
    out = {"effects": {}, "auras": {}, "statuses": {}}
    for d in sorted(os.listdir(base)):
        f = os.path.join(base, d)
        if not os.path.isdir(f):
            continue
        if d.startswith("aura_"):
            out["auras"][d[5:].upper()] = effect(f)
        elif d.startswith("status_"):
            out["statuses"][d[7:]] = effect(f)
        else:
            out["effects"][d] = effect(f)
    json.dump(out, open(path, "w"), separators=(",", ":"))
    print(path, os.path.getsize(path) // 1024, "KB")


def main(out_path):
    rows = species_rows()
    # In Pokedex order (a line goes where its first Kanto member does: the gliders with Pikachu).
    FAMILIES.sort(key=lambda f: min((rows[s]["dex"] for s in f[1] if rows[s]["dex"] <= 151), default=rows[f[1][0]]["dex"]))
    data = {"families": [], "species": {}}
    # Each creature's models (both looks) go in a file of their own beside the page, fetched when it's first picked;
    # the page itself carries only what the lists and panels need.
    mons_dir = os.path.join(os.path.dirname(os.path.abspath(out_path)), "mons")
    os.makedirs(mons_dir, exist_ok=True)
    heavy = ("geo", "anims", "texture", "layers")
    for fam, members in FAMILIES:
        data["families"].append({"name": fam, "members": members})
        for sid in members:
            ours = model(os.path.join(ROOT, "mons", sid))
            theirs = model(os.path.join(ROOT, "pokemon", sid))
            json.dump({"ours": {k: ours[k] for k in heavy}, "theirs": {k: theirs[k] for k in heavy}}, open(os.path.join(mons_dir, sid + ".json"), "w"),
                      separators=(",", ":"))
            light = lambda d: {k: v for k, v in d.items() if k not in heavy}
            data["species"][sid] = {"stats": rows[sid], "ours": light(ours), "theirs": light(theirs)}
    # (0.66) The farm's and the pets', after the rest.
    farm = farm_rows()
    for fam, members in FARM_FAMILIES:
        data["families"].append({"name": fam, "members": members})
        for sid in members:
            ours = model(os.path.join(FARM, "mons", sid))
            json.dump({"ours": {k: ours[k] for k in heavy}, "theirs": {k: ours[k] for k in heavy}}, open(os.path.join(mons_dir, sid + ".json"), "w"),
                      separators=(",", ":"))
            light = lambda d: {k: v for k, v in d.items() if k not in heavy}
            data["species"][sid] = {"stats": farm[sid], "ours": light(ours), "theirs": light(ours)}
    for sid, d in data["species"].items():
        ev = d["stats"]["evolves"]
        if ev:
            ev["oursName"] = data["species"].get(ev["into"], {}).get("ours", {}).get("info", {}).get("name")
            ev["theirsName"] = rows[ev["into"]]["name"]
    ours = {sid for _, members in FAMILIES for sid in members}
    data["moves"] = move_rows(rows, ours)
    write_moves(os.path.join(os.path.dirname(os.path.abspath(out_path)), "moves.json"))
    page = open(os.path.join(HERE, "monviewer.html")).read()
    page = page.replace("/*DATA*/null", json.dumps(data, separators=(",", ":")))
    open(out_path, "w").write(page)
    print(out_path, os.path.getsize(out_path) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1])
