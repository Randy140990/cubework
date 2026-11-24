"""Ours only, bigger: two views each (front three-quarter and from behind). python3 preview6.py out.png sid ..."""
import os, sys, json
from PIL import Image, ImageDraw, ImageFont
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import monshow as MS

out, sids = sys.argv[1], sys.argv[2:]
cell = 300
cols = 3
rows = (len(sids) + cols - 1) // cols
W = Image.new("RGB", (cols * cell * 2, rows * (cell + 24)), (222, 238, 228))
d = ImageDraw.Draw(W)
try: font = ImageFont.truetype(MS.FONT, 18)
except Exception: font = None
for i, sid in enumerate(sids):
    x0, y0 = (i % cols) * cell * 2, (i // cols) * (cell + 24)
    try:
        cr = MS.Creature(sid, os.path.join(MS.MONS, sid))
        for j, (yaw, pitch) in enumerate(((-35, 14), (140, 18))):
            im, _ = MS.render(cr, float(os.environ.get('T', '0.3')), cell * 2, yaw=yaw, pitch=pitch, fill=0.92)
            bb = im.getbbox()
            if bb:
                im = im.crop(bb)
                k = min((cell - 10) / im.width, (cell - 10) / im.height)
                im = im.resize((max(1, int(im.width * k)), max(1, int(im.height * k))), Image.LANCZOS)
            W.paste(im, (x0 + j * cell + (cell - im.width) // 2, y0 + 22 + (cell - im.height)), im)
    except Exception as e:
        d.text((x0 + 4, y0 + 40), str(e)[:60], fill=(150, 0, 0))
    nm = json.load(open(os.path.join(MS.MONS, sid, "info.json")))["name"]
    d.text((x0 + 4, y0 + 2), f"{nm} ({sid})", fill=(30, 40, 40), font=font)
W.save(out)
print(out)
