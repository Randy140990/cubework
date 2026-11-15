import os, sys
import numpy as np
from PIL import Image
Image.MAX_IMAGE_PIXELS = None
src, dst = sys.argv[1], sys.argv[2]
os.makedirs(dst, exist_ok=True)

def first(*names):
    for n in names:
        p = os.path.join(src, n)
        if os.path.exists(p) and os.path.getsize(p) > 10000:
            return p
    return None

def save(img, name, q=92, size=(4096, 2048)):
    im = img.convert("RGB") if img.mode not in ("RGB", "L") else img
    im = im.resize(size, Image.LANCZOS)
    im.save(os.path.join(dst, name), quality=q, optimize=True, subsampling=0 if q >= 90 else 2)
    im.resize((1024, 512), Image.LANCZOS).save(os.path.join(dst, "preview_" + name), quality=85)
    print("saved", name, im.size, os.path.getsize(os.path.join(dst, name)))

for kind, out, q in (("day", "planet_day.jpg", 92), ("night", "planet_night.jpg", 90), ("clouds", "planet_clouds.jpg", 90)):
    p = first("sss_%s.jpg" % kind)
    if p:
        try:
            im = Image.open(p)
            print(kind, "source", im.size, im.mode)
            save(im.convert("L") if kind == "clouds" else im, out, q)
        except Exception as e:
            print("failed", kind, e)

def read_exr(path):
    import OpenEXR
    try:
        with OpenEXR.File(path) as f:
            ch = f.channels()
            print("channels", list(ch.keys()))
            if "RGB" in ch:
                return np.asarray(ch["RGB"].pixels, dtype=np.float32)
            if "RGBA" in ch:
                return np.asarray(ch["RGBA"].pixels, dtype=np.float32)[:, :, :3]
            return np.dstack([np.asarray(ch[c].pixels, dtype=np.float32) for c in ("R", "G", "B")])
    except AttributeError:
        import Imath
        f = OpenEXR.InputFile(path)
        dw = f.header()["dataWindow"]
        w, h = dw.max.x - dw.min.x + 1, dw.max.y - dw.min.y + 1
        pt = Imath.PixelType(Imath.PixelType.FLOAT)
        return np.dstack([np.frombuffer(f.channel(c, pt), dtype=np.float32).reshape(h, w) for c in "RGB"])

s = first("starmap_8k.exr")
if s:
    try:
        hdr = np.maximum(read_exr(s), 0)
        print("exr", hdr.shape, hdr.dtype)
        lum = hdr.mean(axis=2)
        for p in (10, 50, 90, 99, 99.9, 99.99):
            print("lum p%s = %g" % (p, np.percentile(lum, p)))
        p999 = float(np.percentile(lum, 99.9))
        for tag, k in (("a", 0.6), ("b", 1.2), ("c", 2.5), ("d", 5.0)):
            v = hdr * (k / max(p999, 1e-9))
            out = np.power(np.clip(v / (1.0 + v), 0, 1), 1 / 2.2)
            img = Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGB")
            img.resize((2048, 1024), Image.LANCZOS).save(os.path.join(dst, "stars_preview_%s.jpg" % tag), quality=88)
            h, w = out.shape[:2]
            img.crop((int(w * 0.70), int(h * 0.55), int(w * 0.70) + 1024, int(h * 0.55) + 512)).save(os.path.join(dst, "stars_crop_%s.jpg" % tag), quality=90)
            if tag in ("b", "c"):
                img.save(os.path.join(dst, "stars_%s.jpg" % tag), quality=92, optimize=True, subsampling=0)
                print("stars", tag, os.path.getsize(os.path.join(dst, "stars_%s.jpg" % tag)))
            del v, out, img
    except Exception as e:
        import traceback
        traceback.print_exc()
