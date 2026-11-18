"""
The first 151 Pokémon as Retromon draws them: each Cobblemon model built the way the mod builds it (portraits.py's
geometry, the pose it stands in), with its texture and its glowing layer (a flame, eyes), seen from the front
three-quarters and from behind, side by side, for a sense check of every model. Writes one picture per Pokémon
(front | back) and gallery.json (number, name, types, height) into the folder given.

    python3 tools/gallery.py OUT_DIR [species ...]
"""

import json
import math
import os
import sys
from multiprocessing import Pool

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portraits as P  # noqa: E402

ROOT = P.ROOT
DATA = os.path.join(ROOT, "..", "..", "retromon", "data")
SIZE, SS = 300, 2


def view(yaw_deg, pitch_deg):
    flip = np.diag([-1.0, -1.0, 1.0])
    face = P.rot_zyx(0, math.pi, 0)
    turn = P.rot_zyx(0, math.radians(yaw_deg), 0)
    tilt = P.rot_zyx(math.radians(pitch_deg), 0, 0)
    return tilt @ turn @ face @ flip


def texture(tex_id):
    ns, path = tex_id.split(":", 1)
    return np.array(Image.open(os.path.join(ROOT, "..", ns, path)).convert("RGBA")).astype(np.float32)


def raster(proj, tex, tw, th, N, scale, cx, cy, img, depth, lit, layer):
    """Draws the quads into img (lit, depth-tested); a layer is drawn only on the model's own surface, glowing."""
    th_img, tw_img = tex.shape[0], tex.shape[1]
    su, sv = tw_img / tw, th_img / th
    light = np.array([-0.45, -0.65, 0.6])
    light /= np.linalg.norm(light)
    for Pq, uvs in proj:
        S = np.empty((4, 3))
        S[:, 0] = (Pq[:, 0] - cx) * scale + N / 2
        S[:, 1] = (cy - Pq[:, 1]) * scale + N / 2
        S[:, 2] = Pq[:, 2]
        nrm = np.cross(Pq[1] - Pq[0], Pq[2] - Pq[0])
        ln = np.linalg.norm(nrm)
        if ln < 1e-9:
            nrm = np.cross(Pq[2] - Pq[1], Pq[3] - Pq[1])
            ln = np.linalg.norm(nrm)
        nrm = nrm / ln if ln > 1e-9 else np.array([0, 0, 1.0])
        if nrm[2] < 0:
            nrm = -nrm
        shade = 0.62 + 0.38 * max(0.0, float(nrm @ light)) if lit else 1.0
        UV = np.array(uvs, np.float64)
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = S[list(tri)]
            ta, tb, tc = UV[list(tri)]
            x0, x1 = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(N - 1, math.ceil(max(a[0], b[0], c[0]))))
            y0, y1 = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(N - 1, math.ceil(max(a[1], b[1], c[1]))))
            if x1 < x0 or y1 < y0:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            w0 = ((b[1] - c[1]) * (xs - c[0]) + (c[0] - b[0]) * (ys - c[1])) / den
            w1 = ((c[1] - a[1]) * (xs - c[0]) + (a[0] - c[0]) * (ys - c[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            u = (w0 * ta[0] + w1 * tb[0] + w2 * tc[0]) * su
            v = (w0 * ta[1] + w1 * tb[1] + w2 * tc[1]) * sv
            ui = np.clip(np.floor(u).astype(int), 0, tw_img - 1)
            vi = np.clip(np.floor(v).astype(int), 0, th_img - 1)
            col = tex[vi, ui]
            sub_d = depth[y0:y1 + 1, x0:x1 + 1]
            sub_i = img[y0:y1 + 1, x0:x1 + 1]
            if layer:
                ok = inside & (col[..., 3] > 8) & (z >= sub_d - 0.02)
                if not ok.any():
                    continue
                al = (col[ok][:, 3:4] / 255.0)
                sub_i[ok, :3] = sub_i[ok, :3] * (1 - al) + col[ok][:, :3] * al
                sub_i[ok, 3] = np.maximum(sub_i[ok, 3], col[ok][:, 3])
                continue
            ok = inside & (col[..., 3] > 100) & (z > sub_d)
            if not ok.any():
                continue
            sub_d[ok] = z[ok]
            c2 = col[ok].copy()
            c2[:, :3] *= shade
            c2[:, 3] = 255
            sub_i[ok] = c2


def render(species):
    folder = os.path.join(P.MODELS, species)
    info = json.load(open(os.path.join(folder, "info.json")))
    geo = json.load(open(os.path.join(folder, "model.geo.json")))
    tex = texture(info.get("texture") or f"retromon:textures/pokemon/{species}.png")
    layers = [texture(l["frames"][0]) for l in info.get("layers", []) if l.get("frames")]
    hidden = [t["part"] for t in info.get("parts", []) if t.get("visible") is False]
    # The pose it stands in as the game plays it: its battle idle first (as BedrockModel.java picks), else its ground idle.
    pick = dict(info, ground=info.get("idle") or info.get("ground"))
    quads, tw, th = P.model_quads(geo, P.idle_pose(folder, pick), hidden)
    if not quads:
        return None
    views = [view(-26, 10), view(180 - 30, 10)]
    # One scale for both views (fitted to whichever is wider), so front and back match.
    projs = [[(np.array([V @ p for p in Pq]), uvs) for Pq, uvs in quads] for V in views]
    N = SIZE * SS
    spans = []
    for proj in projs:
        allp = np.concatenate([Pq for Pq, _ in proj])
        mn, mx = allp.min(axis=0), allp.max(axis=0)
        spans.append((mn, mx, max(mx[0] - mn[0], mx[1] - mn[1], 1e-3)))
    span = max(s[2] for s in spans)
    scale = N * 0.84 / span
    out = Image.new("RGBA", (SIZE * 2, SIZE), (0, 0, 0, 0))
    for i, proj in enumerate(projs):
        mn, mx, _ = spans[i]
        cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
        img = np.zeros((N, N, 4), np.float32)
        depth = np.full((N, N), -1e9, np.float32)
        raster(proj, tex, tw, th, N, scale, cx, cy, img, depth, True, False)
        for lt in layers:
            raster(proj, lt, tw, th, N, scale, cx, cy, img, depth, False, True)
        im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").resize((SIZE, SIZE), Image.Resampling.BOX)
        out.paste(im, (i * SIZE, 0))
    return out, float(info.get("height", 1))


def job(args):
    sp, out_dir = args
    try:
        r = render(sp)
    except Exception as e:  # one broken model shouldn't stop the rest
        return sp, None, str(e)
    if r is None:
        return sp, None, "no geometry"
    im, h = r
    im.save(os.path.join(out_dir, sp + ".webp"), lossless=True, quality=100, method=4)
    return sp, h, None


def main():
    out_dir = sys.argv[1]
    only = set(sys.argv[2:])
    os.makedirs(out_dir, exist_ok=True)
    rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(DATA, "species.tsv"), encoding="utf-8") if not l.startswith("#")]
    rows = [r for r in rows if int(r[0]) <= 151 and (not only or r[1] in only)]
    with Pool(2) as pool:
        res = {sp: (h, err) for sp, h, err in pool.map(job, [(r[1], out_dir) for r in rows])}
    entries = []
    for r in rows:
        h, err = res[r[1]]
        entries.append({"n": int(r[0]), "id": r[1], "name": r[2], "types": [t for t in (r[3], r[4]) if t and t != "-"], "height": h, "error": err})
    json.dump(entries, open(os.path.join(out_dir, "gallery.json"), "w"), indent=1)
    print(sum(1 for e in entries if not e["error"]), "of", len(entries), "drawn")


if __name__ == "__main__":
    main()
