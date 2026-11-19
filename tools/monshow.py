"""
Pictures and films of Retromon's own creatures (tools/mons.py), posed by their own animations the way the mod
plays them: a still line-up, and a short film of them standing, walking and calling.

Run: python3 tools/monshow.py <out folder>
"""

import json, math, os, re, subprocess, sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import portraits  # noqa: E402  (its geometry is built as the mod builds it)

ROOT = portraits.ROOT
MONS = os.path.join(ROOT, "mons")
ORDER = ["bulbasaur", "charmander", "squirtle", "pidgey", "rattata"]
ALL = ["bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard", "squirtle", "wartortle", "blastoise", "pidgey", "pidgeotto", "pidgeot",
       "rattata", "raticate", "nidoran_f", "nidorina", "nidoqueen", "nidoran_m", "nidorino", "nidoking", "mankey", "primeape", "spearow", "fearow",
       "pichu", "pikachu", "raichu"]
TYPES = {"bulbasaur": ("GRASS", "#4FA64A"), "charmander": ("FIRE", "#E8662C"), "squirtle": ("WATER", "#2F74BA"), "pidgey": ("NORMAL · FLYING", "#8C80A8"),
         "rattata": ("NORMAL", "#B7864A")}
FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"


# ---------- animations at a moment ----------

def molang(expr, t):
    if isinstance(expr, (int, float)):
        return float(expr)
    e = str(expr).replace("query.", "q.").replace("q.anim_time", f"({t})").replace("q.life_time", f"({t})")
    e = re.sub(r"math\.(\w+)", r"m_\1", e)
    ns = {"m_sin": lambda d: math.sin(math.radians(d)), "m_cos": lambda d: math.cos(math.radians(d)), "m_abs": abs, "m_pi": math.pi,
          "m_clamp": lambda v, a, b: max(a, min(b, v)), "m_lerp": lambda a, b, k: a + (b - a) * k, "m_min": min, "m_max": max,
          "m_sqrt": math.sqrt, "m_floor": math.floor, "m_ceil": math.ceil, "m_round": round, "m_mod": lambda a, b: a % b}
    return float(eval(e, {"__builtins__": {}}, ns))


def channel(ch, t, default):
    if isinstance(ch, dict):
        ks = sorted(ch.items(), key=lambda kv: float(kv[0]))
        times = [float(k) for k, _ in ks]
        vals = [[molang(x, t) for x in (v if isinstance(v, list) else [v, v, v])] for _, v in ks]
        if t <= times[0]:
            return vals[0]
        if t >= times[-1]:
            return vals[-1]
        for i in range(len(times) - 1):
            if times[i] <= t <= times[i + 1]:
                k = (t - times[i]) / max(1e-9, times[i + 1] - times[i])
                return [a + (b - a) * k for a, b in zip(vals[i], vals[i + 1])]
    if not isinstance(ch, list):
        ch = [ch, ch, ch]
    return [molang(x, t) for x in ch[:3]]


def pose_at(anims, names_times):
    """Each bone's (rotation, position, scale) from these animations at these moments, added together as the mod does."""
    out = {}
    for name, t in names_times:
        a = anims.get(name)
        if not a:
            continue
        L = a.get("animation_length", 0)
        loops = a.get("loop") in (True, "true")  # "hold_on_last_frame" plays once and stays
        tt = (t % L) if (loops and L) else min(t, L) if L else t
        for bone, chans in a.get("bones", {}).items():
            r0, p0, s0 = out.get(bone, ([0, 0, 0], [0, 0, 0], [1, 1, 1]))
            r = channel(chans["rotation"], tt, [0, 0, 0]) if "rotation" in chans else [0, 0, 0]
            p = channel(chans["position"], tt, [0, 0, 0]) if "position" in chans else [0, 0, 0]
            s = channel(chans["scale"], tt, [1, 1, 1]) if "scale" in chans else [1, 1, 1]
            out[bone] = ([a1 + b1 for a1, b1 in zip(r0, r)], [a1 + b1 for a1, b1 in zip(p0, p)], [a1 * b1 for a1, b1 in zip(s0, s)])
    return out


# ---------- drawing ----------

class Creature:
    def __init__(self, sid, folder=None, tex_override=None):
        folder = folder or os.path.join(MONS, sid)
        self.sid = sid
        self.info = json.load(open(os.path.join(folder, "info.json")))
        self.geo = json.load(open(os.path.join(folder, "model.geo.json")))
        p = os.path.join(folder, "anim.json")
        self.anims = json.load(open(p)).get("animations", {}) if os.path.exists(p) else {}
        tex_id = tex_override or self.info.get("texture")
        ns, path = tex_id.split(":", 1)
        self.tex = np.array(Image.open(os.path.join(ROOT, "..", ns, path)).convert("RGBA")).astype(np.float32)
        self.glow = []  # each glow layer's frames, with its frames per second
        for layer in self.info.get("layers", []):
            frames = []
            for fr in layer["frames"]:
                ns, path = fr.split(":", 1)
                frames.append(np.array(Image.open(os.path.join(ROOT, "..", ns, path)).convert("RGBA")).astype(np.float32))
            self.glow.append((frames, layer.get("fps", 10)))
        self.name = self.info.get("name", sid.upper())

    def anim_for(self, pose):
        if pose == "idle":
            return self.info.get("idle") or self.info.get("ground")
        p = self.info.get("poses", {}).get(pose)
        return p["anim"] if p else self.info.get("idle") or self.info.get("ground")

    def quads(self, t, pose="idle", action=None, action_t=0.0):
        nt = [(self.anim_for(pose), t)]
        if action:
            nt.append((self.info.get("actions", {}).get(action), action_t))
        anim = pose_at(self.anims, nt)
        hidden = [p["part"] for p in self.info.get("parts", []) if p.get("visible") is False]
        q, tw, th = portraits.model_quads(self.geo, anim, hidden)
        return q, tw, th


def render(cr, t, size, pose="idle", action=None, action_t=0.0, yaw=-30, pitch=12, frame=None, ss=3, fill=0.8):
    """The creature at a moment, on a clear background. frame: (cx, cy, span) to keep it still across a film."""
    quads, tw, th = cr.quads(t, pose, action, action_t)
    V = portraits.view_matrix(yaw, pitch)
    s = cr.info.get("scale", 1.0)
    proj = [(np.array([V @ (p * s) for p in P]), uvs) for P, uvs in quads]
    if frame is None:
        allp = np.concatenate([P for P, _ in proj])
        mn, mx = allp.min(axis=0), allp.max(axis=0)
        frame = ((mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, max(mx[0] - mn[0], mx[1] - mn[1], 1e-3))
    cx, cy, span = frame
    N = size * ss
    scale = N * fill / span
    img = np.zeros((N, N, 4), np.float32)
    depth = np.full((N, N), -1e9, np.float32)
    light = np.array([-0.5, -0.7, 0.55])
    light /= np.linalg.norm(light)
    texes = [(cr.tex, False)] + [(fr[int(t * fps) % len(fr)], True) for fr, fps in cr.glow]
    for P, uvs in proj:
        S = np.empty((4, 3))
        S[:, 0] = (P[:, 0] - cx) * scale + N / 2
        S[:, 1] = (cy - P[:, 1]) * scale + N / 2
        S[:, 2] = P[:, 2]
        nrm = np.cross(P[1] - P[0], P[2] - P[0])
        ln = np.linalg.norm(nrm)
        if ln < 1e-9:
            nrm = np.cross(P[2] - P[1], P[3] - P[1])
            ln = np.linalg.norm(nrm)
        nrm = nrm / ln if ln > 1e-9 else np.array([0, 0, 1.0])
        if nrm[2] < 0:
            nrm = -nrm
        shade = 0.6 + 0.45 * max(0.0, float(nrm @ light))
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
            sub_d = depth[y0:y1 + 1, x0:x1 + 1]
            sub_i = img[y0:y1 + 1, x0:x1 + 1]
            for tex, glow in texes:
                th_img, tw_img = tex.shape[0], tex.shape[1]
                u = (w0 * ta[0] + w1 * tb[0] + w2 * tc[0]) * tw_img / tw
                v = (w0 * ta[1] + w1 * tb[1] + w2 * tc[1]) * th_img / th
                ui = np.clip(np.floor(u).astype(int), 0, tw_img - 1)
                vi = np.clip(np.floor(v).astype(int), 0, th_img - 1)
                col = tex[vi, ui]
                if not glow:
                    ok = inside & (col[..., 3] > 100) & (z > sub_d)
                    if not ok.any():
                        continue
                    sub_d[ok] = z[ok]
                    c2 = col[ok].copy()
                    c2[:, :3] *= shade
                    c2[:, 3] = 255
                    sub_i[ok] = c2
                else:
                    # Glow over the part, or on its own where the part is clear (a flame): see-through at its edges,
                    # solid enough to hide what's behind it at its heart.
                    ok = inside & (col[..., 3] > 10) & (z >= sub_d - 1e-4)
                    if not ok.any():
                        continue
                    al = col[ok][:, 3:4] / 255.0
                    empty = sub_i[ok, 3:4] < 1
                    lit = sub_i[ok, :3] * (1 - al * 0.85) + col[ok][:, :3] * al * 0.85 + col[ok][:, :3] * al * 0.25
                    sub_i[ok, :3] = np.where(empty, np.minimum(255, col[ok][:, :3] * 1.1), lit)
                    sub_i[ok, 3] = np.maximum(sub_i[ok, 3], al[:, 0] * 255)
                    solid = ok & (col[..., 3] > 140)
                    sub_d[solid] = np.maximum(sub_d[solid], z[solid])
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").resize((size, size), Image.Resampling.LANCZOS)
    return out, frame


def backdrop(w, h, top="#DFF3E6", bottom="#9FD3C0"):
    a, b = np.array(Image.new("RGB", (1, 1), top)).reshape(3), np.array(Image.new("RGB", (1, 1), bottom)).reshape(3)
    ys = np.linspace(0, 1, h)[:, None, None]
    g = (a[None, None, :] * (1 - ys) + b[None, None, :] * ys).repeat(w, axis=1)
    return Image.fromarray(g.astype(np.uint8), "RGB").convert("RGBA")


def shadow(im, cx, y, rx, ry, alpha=70):
    sh = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(sh)
    for i in range(8):
        k = 1 - i / 8
        d.ellipse([cx - rx * k, y - ry * k, cx + rx * k, y + ry * k], fill=(20, 60, 50, int(alpha / 8)))
    im.alpha_composite(sh)


def lineup(out_path, t=0.35):
    cell, pad = 460, 30
    W, H = cell * len(ORDER) + pad * 2, cell + 170
    im = backdrop(W, H)
    f_big = ImageFont.truetype(FONT, 38)
    f_small = ImageFont.truetype(FONT, 20)
    for i, sid in enumerate(ORDER):
        cr = Creature(sid)
        pic, _ = render(cr, t, cell)
        x = pad + i * cell
        shadow(im, x + cell // 2, 40 + cell - 62, cell * 0.24, 16)
        im.alpha_composite(pic, (x, 40))
        tname, tcol = TYPES[sid]
        d = ImageDraw.Draw(im)
        tw_ = d.textlength(cr.name, font=f_big)
        d.text((x + (cell - tw_) / 2, cell + 40), cr.name, font=f_big, fill="#1F3A35")
        tw2 = d.textlength(tname, font=f_small) + 28
        d.rounded_rectangle([x + (cell - tw2) / 2, cell + 92, x + (cell + tw2) / 2, cell + 124], radius=16, fill=tcol)
        d.text((x + (cell - tw2) / 2 + 14, cell + 95), tname, font=f_small, fill="white")
    im.convert("RGB").save(out_path, quality=92)


def film(out_path, fps=24):
    """All five together: standing (turning round slowly), walking, then calling out."""
    cell = 300
    W, H = cell * len(ORDER), cell + 70
    crs = [Creature(s) for s in ORDER]
    frames_dir = out_path + "_frames"
    os.makedirs(frames_dir, exist_ok=True)
    f_name = ImageFont.truetype(FONT, 26)
    f_cap = ImageFont.truetype(FONT, 22)
    # A frame for each so they keep still in the picture.
    fixed = [render(c, 0, cell // 2, yaw=-30)[1] for c in crs]
    fixed = [(fx, fy, sp * 1.18) for fx, fy, sp in fixed]
    plan = [("STANDING", "idle", None, 3.0, True), ("WALKING", "walk", None, 2.5, False), ("CALLING OUT", "idle", "cry", 1.6, False),
            ("HIT", "idle", "recoil", 0.9, False)]
    n = 0
    for caption, pose, action, secs, spin in plan:
        for k in range(int(secs * fps)):
            t = k / fps
            im = backdrop(W, H)
            d = ImageDraw.Draw(im)
            for i, c in enumerate(crs):
                yaw = -30 + (math.sin(t / secs * 2 * math.pi) * 35 if spin else 0)
                pic, _ = render(c, t, cell, pose=pose, action=action, action_t=t, yaw=yaw, frame=fixed[i], ss=2)
                x = i * cell
                shadow(im, x + cell // 2, cell - 40, cell * 0.22, 10)
                im.alpha_composite(pic, (x, 8))
                tw_ = d.textlength(c.name, font=f_name)
                d.text((x + (cell - tw_) / 2, cell + 8), c.name, font=f_name, fill="#1F3A35")
            cw = d.textlength(caption, font=f_cap) + 30
            d.rounded_rectangle([(W - cw) / 2, 10, (W + cw) / 2, 44], radius=17, fill="#123C38")
            d.text(((W - cw) / 2 + 15, 13), caption, font=f_cap, fill="white")
            im.convert("RGB").save(os.path.join(frames_dir, f"{n:04d}.png"))
            n += 1
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i", os.path.join(frames_dir, "%04d.png"), "-c:v", "libx264",
                    "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p", out_path], check=True)


def portraits_for_screens():
    """Their pictures for the party, the Pokédex and the summaries (as tools/portraits.py makes Cobblemon's), at the same view."""
    out = os.path.join(ROOT, "textures", "gui", "mon_r")
    os.makedirs(out, exist_ok=True)
    for sid in ALL:
        pic, _ = render(Creature(sid), 0, 128, yaw=-26, pitch=12, ss=3, fill=0.86)
        pic.save(os.path.join(out, sid + ".png"), optimize=True)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(out, exist_ok=True)
    portraits_for_screens()
    lineup(os.path.join(out, "retromon_creatures.jpg"))
    if "--film" in sys.argv:
        film(os.path.join(out, "retromon_creatures.mp4"))


def families_sheet(out_path, sizes_path):
    """Every family in a row, each creature with its type, its height in blocks beside Cobblemon's, and its base stats."""
    sizes = json.load(open(sizes_path))
    rows = {}
    for line in open(os.path.join(ROOT, "..", "..", "retromon", "data", "species.tsv"), encoding="utf-8"):
        if line.startswith("#") or not line.strip():
            continue
        f = line.rstrip("\n").split("\t")
        rows[f[1]] = {"name": f[2], "types": [t for t in (f[3], f[4]) if t != "-"], "bst": sum(int(x) for x in f[5:11]),
                      "stats": [int(x) for x in (f[5], f[6], f[7], f[9], f[10], f[8])]}
    fams = [ALL[0:3], ALL[3:6], ALL[6:9], ALL[9:12], ALL[12:14]]
    tcol = {"GRASS": "#4E9F45", "POISON": "#8E54B8", "FIRE": "#E0602A", "FLYING": "#7A88D8", "WATER": "#3479C9", "NORMAL": "#938C74"}
    cw, ch = 560, 500
    W, H = cw * 3 + 40, ch * len(fams) + 40
    im = backdrop(W, H, "#EAF4EF", "#CFE6DC")
    d = ImageDraw.Draw(im)
    f_name = ImageFont.truetype(FONT, 30)
    f_big = ImageFont.truetype(FONT, 34)
    f_sub = ImageFont.truetype(FONT.replace("Bold", "Medium") if os.path.exists(FONT.replace("Bold", "Medium")) else FONT, 18)
    f_small = ImageFont.truetype(FONT, 16)
    for r, fam in enumerate(fams):
        for c, sid in enumerate(fam):
            x, y = 20 + c * cw, 20 + r * ch
            d.rounded_rectangle([x + 6, y + 6, x + cw - 6, y + ch - 6], radius=22, fill="#FFFFFF")
            cr = Creature(sid)
            pic, _ = render(cr, 0.3, 300, yaw=-35, pitch=12, ss=2)
            shadow(im, x + 150 + 10, y + 300 - 32, 90, 10, 50)
            im.alpha_composite(pic, (x + 10, y + 6))
            info = rows[sid]
            tx = x + 320
            d.text((tx, y + 24), cr.name, font=f_name, fill="#10302B")
            d.text((tx, y + 66), "for " + info["name"].title(), font=f_sub, fill="#4E6E68")
            ty = y + 100
            for t in info["types"]:
                w = d.textlength(t, font=f_small) + 22
                d.rounded_rectangle([tx, ty, tx + w, ty + 26], radius=13, fill=tcol.get(t, "#938C74"))
                d.text((tx + 11, ty + 3), t, font=f_small, fill="white")
                tx += w + 6
            tx = x + 320
            s = sizes.get(sid, {})
            ours, theirs = s.get("ours", [0, 0, 0]), s.get("theirs", [0, 0, 0])
            d.text((tx, y + 142), "HEIGHT (BLOCKS)", font=f_small, fill="#4E6E68")
            d.text((tx, y + 162), f"{ours[0]:.2f}", font=f_big, fill="#178A69")
            d.text((tx, y + 206), f"Cobblemon's: {theirs[0]:.2f}", font=f_small, fill="#6A7D9A")
            d.text((tx, y + 232), f"Length {ours[1]:.2f} · width {ours[2]:.2f}", font=f_small, fill="#4E6E68")
            labels = ["HP", "ATK", "DEF", "SPA", "SPD", "SPE"]
            by = y + 330
            d.text((x + 24, by - 6), f"BASE STATS {info['bst']}", font=f_small, fill="#4E6E68")
            for i, (lab, v) in enumerate(zip(labels, info["stats"])):
                yy = by + 22 + i * 22
                d.text((x + 24, yy), lab, font=f_small, fill="#4E6E68")
                d.text((x + 70, yy), str(v), font=f_small, fill="#10302B")
                d.rounded_rectangle([x + 110, yy + 5, x + cw - 30, yy + 15], radius=5, fill="#E3EFEA")
                d.rounded_rectangle([x + 110, yy + 5, x + 110 + max(10, (cw - 140) * min(1, v / 160)), yy + 15], radius=5, fill="#2E9E7B")
    im.convert("RGB").save(out_path, quality=90)
