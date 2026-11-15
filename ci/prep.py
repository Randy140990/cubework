import os, sys
import numpy as np
os.environ.setdefault("OPENCV_IO_ENABLE_OPENEXR", "1")
import cv2
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
    im.save(os.path.join(dst, name), quality=q, optimize=True, progressive=False, subsampling=0 if q >= 90 else 2)
    im.resize((1024, 512), Image.LANCZOS).save(os.path.join(dst, "preview_" + name), quality=85)
    print("saved", name, im.size, os.path.getsize(os.path.join(dst, name)))

d = first("8k_earth_daymap.jpg", "nasa_day.jpg")
if d: save(Image.open(d), "planet_day.jpg", 92); print("day from", d)
n = first("8k_earth_nightmap.jpg", "nasa_night.jpg")
if n: save(Image.open(n), "planet_night.jpg", 90); print("night from", n)
c = first("8k_earth_clouds.jpg", "nasa_clouds.jpg")
if c: save(Image.open(c).convert("L"), "planet_clouds.jpg", 90); print("clouds from", c)

s = first("starmap_8k.exr")
if s:
    hdr = cv2.imread(s, cv2.IMREAD_UNCHANGED)
    print("exr", None if hdr is None else (hdr.shape, hdr.dtype))
    if hdr is not None:
        hdr = hdr[:, :, :3][:, :, ::-1].astype(np.float32)  # BGR -> RGB
        hdr = np.maximum(hdr, 0)
        lum = hdr.mean(axis=2)
        for p in (10, 50, 90, 99, 99.9, 99.99):
            print("lum p%s = %g" % (p, np.percentile(lum, p)))
        p50, p999 = np.percentile(lum, 50), np.percentile(lum, 99.9)
        for tag, k in (("a", 0.6), ("b", 1.2), ("c", 2.5), ("d", 5.0)):
            v = hdr * (k / max(p999, 1e-9))
            out = v / (1.0 + v)
            out = np.power(np.clip(out, 0, 1), 1 / 2.2)
            img = Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGB")
            img.resize((2048, 1024), Image.LANCZOS).save(os.path.join(dst, "stars_preview_%s.jpg" % tag), quality=88)
            # A crop at full size round the Milky Way's centre, to judge how sharp the stars are.
            h, w = out.shape[:2]
            img.crop((int(w * 0.70), int(h * 0.55), int(w * 0.70) + 1024, int(h * 0.55) + 512)).save(os.path.join(dst, "stars_crop_%s.jpg" % tag), quality=90)
        np.save("/tmp/raw/stars_scale.npy", np.array([p50, p999]))
