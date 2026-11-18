"""
The Pokédex reads its entries aloud, in a flat robotic voice (as the Pokédex does on TV): "Pidgey. The tiny
bird Pokémon. A normal and flying type. It usually hides in tall grass..." For all 251, from Gold and Silver's
entries (dex.tsv), spoken by Flite's diphone voice (cmu_us_kal16, the system's libflite through ctypes), held
nearly to one pitch, then made metallic with ffmpeg. Written as small mono Ogg files:
assets/retromon/sounds/dex/<species>.ogg, and their entries in sounds.json (dex.<species>).

Usage: dexvoice.py [species ...]   (all of them if none are named)
"""
import ctypes, json, os, subprocess, sys, tempfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
DATA = os.path.join(ROOT, "retromon", "data")
OUT = os.path.join(ROOT, "assets", "retromon", "sounds", "dex")
SOUNDS = os.path.join(ROOT, "assets", "retromon", "sounds.json")
LIB = "/usr/lib/x86_64-linux-gnu/"

# Names Flite would say wrongly, written as they sound.
SAY = {"nidoran_f": "Nido-ran female", "nidoran_m": "Nido-ran male", "mr_mime": "Mister Mime", "farfetch_d": "Far-fetched", "ho_oh": "Ho-oh",
       "porygon2": "Porygon two", "pokemon": "Poke-ay-mon", "eevee": "Eevee", "onix": "Onix", "abra": "Abra", "kadabra": "Kadabra"}
TYPES = {"normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison", "ground", "flying", "psychic", "bug", "rock", "ghost",
         "dragon", "dark", "steel"}


def voice():
    g = ctypes.RTLD_GLOBAL
    fl = ctypes.CDLL(LIB + "libflite.so.1", mode=g)
    ctypes.CDLL(LIB + "libflite_usenglish.so.1", mode=g)
    ctypes.CDLL(LIB + "libflite_cmulex.so.1", mode=g)
    fl.flite_init()
    v = ctypes.CDLL(LIB + "libflite_cmu_us_kal16.so.1", mode=g)
    v.register_cmu_us_kal16.restype = ctypes.c_void_p
    v.register_cmu_us_kal16.argtypes = [ctypes.c_char_p]
    vox = v.register_cmu_us_kal16(None)
    # cst_voice: name, features, ... - hold it near one pitch, a touch slower and lower: the robot.
    features = ctypes.cast(vox + 8, ctypes.POINTER(ctypes.c_void_p))[0]
    fl.feat_set_float.argtypes = [ctypes.c_void_p, ctypes.c_char_p, ctypes.c_float]
    fl.feat_set_float(features, b"int_f0_target_mean", 98.0)
    fl.feat_set_float(features, b"int_f0_target_stddev", 4.0)
    fl.feat_set_float(features, b"duration_stretch", 1.08)
    fl.flite_text_to_speech.restype = ctypes.c_float
    fl.flite_text_to_speech.argtypes = [ctypes.c_char_p, ctypes.c_void_p, ctypes.c_char_p]
    return fl, vox


def species():
    out = {}
    for line in open(os.path.join(DATA, "species.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        r = line.rstrip("\n").split("\t")
        out[int(r[0])] = r
    return out


def header():
    for line in open(os.path.join(DATA, "species.tsv"), encoding="utf-8"):
        if line.startswith("#"):
            return line[1:].strip().split("\t")
    return []


def spoken(text):
    t = text.replace("POKéMON", "Poke-ay-mon").replace("Pokémon", "Poke-ay-mon").replace("POKé", "Poke-ay").replace("é", "ay")
    return t


def main(only):
    sp = species()
    cols = header()
    t1 = cols.index("type1") if "type1" in cols else 3
    t2 = cols.index("type2") if "type2" in cols else 4
    fl, vox = voice()
    os.makedirs(OUT, exist_ok=True)
    sounds = json.load(open(SOUNDS, encoding="utf-8"))
    done = 0
    for line in open(os.path.join(DATA, "dex.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        dex, category, _h, _w, text = line.rstrip("\n").split("\t")[:5]
        row = sp.get(int(dex))
        if not row:
            continue
        sid = row[1]
        if only and sid not in only:
            continue
        name = SAY.get(sid, row[2].title().replace("♀", " female").replace("♂", " male"))
        types = [t.lower() for t in (row[t1], row[t2]) if t and t.lower() in TYPES]
        types = list(dict.fromkeys(types))
        kind = (" and ".join(types)) + " type" if types else ""
        line_out = f"{name}. The {category.lower()} Poke-ay-mon. {('A ' + kind + '. ') if kind else ''}{spoken(text)}"
        with tempfile.TemporaryDirectory() as tmp:
            wav = os.path.join(tmp, "v.wav")
            fl.flite_text_to_speech(line_out.encode("utf-8"), vox, wav.encode())
            # Metallic and a little bright: a short comb echo, a slight flange, the lows and highs cut.
            filt = ("asetrate=16000*1.04,aresample=22050,highpass=f=140,lowpass=f=6500,"
                    "aecho=0.85:0.75:7|13:0.35|0.22,flanger=delay=1.5:depth=1:speed=0.4:regen=-10,"
                    "acompressor=threshold=0.2:ratio=3:attack=5:release=60,loudnorm=I=-17:TP=-1.5:LRA=7")
            subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-af", filt, "-ac", "1", "-ar", "22050", "-c:a", "libvorbis", "-q:a", "0",
                            os.path.join(OUT, f"{sid}.ogg")], check=True)
        sounds[f"dex.{sid}"] = {"sounds": [{"name": f"retromon:dex/{sid}", "volume": 0.9}]}
        done += 1
    json.dump(sounds, open(SOUNDS, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    print(f"{done} entries spoken")


if __name__ == "__main__":
    main(set(sys.argv[1:]))
