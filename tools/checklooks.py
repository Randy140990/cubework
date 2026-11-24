"""Every picture area_looks.tsv names is one of ours (textures/block/area): python3 tools/checklooks.py"""
import os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, "..", "src/main/resources/assets/retromon/textures/block/area")
have = {f[:-4] for f in os.listdir(TEX) if f.endswith(".png")}
NAMED = {"swap", "panel", "glass", "hedge", "leafy_hedge", "cube", "steam", "mist"}
missing = {}
for line in open(os.path.join(HERE, "..", "src/main/resources/retromon/data/area_looks.tsv")):
    if line.startswith("#") or not line.strip(): continue
    r = line.rstrip("\n").split("\t")
    if r[0] not in NAMED or r[8] in ("-", ""): continue
    parts = [p.split("=", 1)[1] for p in r[8].split(",") if "=" in p] if r[0] != "cube" and r[0] not in ("steam", "mist") else [r[8]]
    for v in parts:
        v = re.sub(r"@\d+", "", v).replace("!", "")
        for t in v.split("|"):
            t = t.strip()
            if t and t not in have and t + "_0" not in have and not t.startswith("prefix:"): missing.setdefault(t, r[9] if len(r) > 9 else "")
for t, w in sorted(missing.items()): print("missing picture:", t, "(", w, ")")
print(len(missing), "missing")
sys.exit(1 if missing else 0)
