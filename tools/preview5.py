"""A contact sheet of creatures (ours, and Cobblemon's beside each): python3 preview5.py out.png sid ..."""
import os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import monshow as MS

out, sids = sys.argv[1], sys.argv[2:]
cell = 220
cols = 4
rows = (len(sids) + cols - 1) // cols
W = Image.new("RGB", (cols * cell * 2, rows * (cell + 24)), (222, 238, 228))
d = ImageDraw.Draw(W)
try: font = ImageFont.truetype(MS.FONT, 16)
except Exception: font = None
for i, sid in enumerate(sids):
    x0, y0 = (i % cols) * cell * 2, (i // cols) * (cell + 24)
    for j, folder in enumerate((os.path.join(MS.MONS, sid), os.path.join(MS.ROOT, "pokemon", sid))):
        try:
            cr = MS.Creature(sid, folder)
            im, _ = MS.render(cr, 0.3, cell, yaw=-35, pitch=14)
            W.paste(im, (x0 + j * cell, y0 + 20), im)
        except Exception as e:
            d.text((x0 + j * cell + 4, y0 + 40), str(e)[:40], fill=(150, 0, 0))
    nm = ""
    try:
        import json; nm = json.load(open(os.path.join(MS.MONS, sid, "info.json")))["name"]
    except Exception: pass
    d.text((x0 + 4, y0 + 2), f"{nm} ({sid})", fill=(30, 40, 40), font=font)
W.save(out)
print(out)
