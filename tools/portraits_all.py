"""Every one of our creatures' pictures for the party, the Pokédex and the summaries (textures/gui/mon_r), not just
the first 81 (0.59.6: the game asks for one for every creature in mons/names.json, and the rest had none).
python3 tools/portraits_all.py [species...]   (all named, or those; only missing ones with --missing)"""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import monshow as MS
names = json.load(open(os.path.join(MS.MONS, "names.json")))
out = os.path.join(MS.ROOT, "textures", "gui", "mon_r")
want = [a for a in sys.argv[1:] if not a.startswith("--")] or sorted(names)
for sid in want:
    path = os.path.join(out, sid + ".png")
    if "--missing" in sys.argv and os.path.exists(path): continue
    pic, _ = MS.render(MS.Creature(sid), 0, 128, yaw=-26, pitch=12, ss=3, fill=0.86)
    pic.save(path, optimize=True)
    print(sid)
