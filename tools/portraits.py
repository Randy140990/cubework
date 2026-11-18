"""
Portraits of every Pokémon for Retromon's screens (party, Pokédex, summaries): each Bedrock model is drawn
from a three-quarter view, lit from the upper left, with its own texture, and saved as a 128 x 128 picture
with a clear background in assets/retromon/textures/gui/mon/<species>.png.

The geometry is built the way the mod builds it in game (BedrockModel.java into Minecraft's ModelPart), so
the textures land as they do on the Pokémon in the world.
"""

import json, math, os, sys

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon")
MODELS = os.path.join(ROOT, "pokemon")
OUT = os.path.join(ROOT, "textures", "gui", "mon")
SIZE, SS = 128, 2  # the picture, and the supersampling for smooth edges


def rot_zyx(x, y, z):
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def pose(offset, rot):
    m = np.eye(4)
    m[:3, :3] = rot_zyx(*rot)
    t = np.eye(4)
    t[:3, 3] = offset
    return t @ m


def vec(o, k):
    v = o.get(k)
    return [float(a) for a in v] if v else [0.0, 0.0, 0.0]


def box_quads(c, pivot, tex_w, tex_h):
    """A cube's six faces in its part's space, each as four corners with their texture spots (Minecraft's box UV)."""
    o, s = vec(c, "origin"), vec(c, "size")
    inf = float(c.get("inflate", 0))
    mirror = bool(c.get("mirror", False))
    u, v = (c.get("uv") or [0, 0])[:2]
    u, v = math.floor(u), math.floor(v)
    x0, y0, z0 = pivot[0] - (o[0] + s[0]), pivot[1] - (o[1] + s[1]), o[2] - pivot[2]
    w, h, d = s
    x1, y1, z1 = x0 + w + inf, y0 + h + inf, z0 + d + inf
    x0, y0, z0 = x0 - inf, y0 - inf, z0 - inf
    if mirror:
        x0, x1 = x1, x0
    V = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f, g, hh, i, j, k = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    l, m, n = v, v + d, v + d + h
    faces = [([5, 4, 0, 1], g, l, hh, m), ([2, 3, 7, 6], hh, m, i, l), ([0, 4, 7, 3], f, m, g, n),
             ([1, 0, 3, 2], g, m, hh, n), ([5, 1, 2, 6], hh, m, j, n), ([4, 5, 6, 7], j, m, k, n)]
    out = []
    for idx, u1, v1, u2, v2 in faces:
        pts = [V[q] for q in idx]
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        if mirror:
            pts, uvs = pts[::-1], uvs[::-1]
        out.append((pts, uvs))
    return out


def molang(expr):
    """A Molang number at the animation's first moment (anim_time 0); anything unknown counts as 0."""
    if isinstance(expr, (int, float)):
        return float(expr)
    e = str(expr).replace("query.", "q.").replace("variable.", "v.").replace("q.anim_time", "0").replace("q.life_time", "0")
    e = e.replace("math.", "m_")
    ns = {"m_sin": lambda d: math.sin(math.radians(d)), "m_cos": lambda d: math.cos(math.radians(d)), "m_abs": abs, "m_pi": math.pi,
          "m_clamp": lambda v, a, b: max(a, min(b, v)), "m_lerp": lambda a, b, t: a + (b - a) * t, "m_min": min, "m_max": max,
          "m_sqrt": math.sqrt, "m_floor": math.floor, "m_ceil": math.ceil, "m_round": round}
    try:
        return float(eval(e, {"__builtins__": {}}, ns))
    except Exception:
        return 0.0


def first_value(ch, default):
    """A channel's value at time 0: a fixed [x, y, z], or its first keyframe."""
    if isinstance(ch, dict):
        if not ch:
            return default
        if any(k in ch for k in ("post", "pre", "vector")):
            ch = ch.get("post", ch.get("pre", ch.get("vector")))
        else:
            first = min(ch.keys(), key=lambda k: float(k))
            v = ch[first]
            ch = v.get("post", v.get("pre", v.get("vector"))) if isinstance(v, dict) else v
    if not isinstance(ch, list):
        ch = [ch, ch, ch]
    return [molang(x) for x in ch[:3]]


def idle_pose(folder, info):
    """Each bone's turn, shift and scale at the start of its idle (how it stands in battle), as the mod plays it."""
    path = os.path.join(folder, "anim.json")
    if not os.path.exists(path):
        return {}
    anims = json.load(open(path)).get("animations", {})
    name = info.get("ground") or info.get("idle")
    a = anims.get(name) or anims.get(info.get("ground") or "") or {}
    out = {}
    for bone, chans in a.get("bones", {}).items():
        out[bone] = (first_value(chans.get("rotation", [0, 0, 0]), [0, 0, 0]), first_value(chans.get("position", [0, 0, 0]), [0, 0, 0]),
                     first_value(chans.get("scale", [1, 1, 1]), [1, 1, 1]))
    return out


def model_quads(geo, anim=None, hidden=()):
    anim = anim or {}
    hidden = set(hidden)
    g = geo["minecraft:geometry"][0]
    tex_w = g["description"].get("texture_width", 64)
    tex_h = g["description"].get("texture_height", 64)
    bones = g["bones"]
    children = {}
    roots = []
    for b in bones:
        if b.get("parent"):
            children.setdefault(b["parent"], []).append(b)
        else:
            roots.append(b)
    quads = []

    def build(b, parent_m, parent_pivot):
        # Parts hidden in its pose (Pidgey's open wings) and Ditto's faces are left out, with all they carry.
        if b["name"] in hidden or ("ditto" in b["name"] and not g["description"].get("identifier", "").endswith("ditto")):
            return
        pv, rot = vec(b, "pivot"), vec(b, "rotation")
        # Bedrock turns the other way round from Minecraft's in all three (see BedrockModel.java); moves are mirrored in x and y.
        r = (math.radians(rot[0]), math.radians(rot[1]), -math.radians(rot[2]))
        if parent_pivot is None:
            off = (-pv[0], 24 - pv[1], pv[2])
        else:
            off = (-(pv[0] - parent_pivot[0]), -(pv[1] - parent_pivot[1]), pv[2] - parent_pivot[2])
        ar, ap, sc = anim.get(b["name"], ([0, 0, 0], [0, 0, 0], [1, 1, 1]))
        r = (r[0] + math.radians(ar[0]), r[1] + math.radians(ar[1]), r[2] - math.radians(ar[2]))
        off = (off[0] - ap[0], off[1] - ap[1], off[2] + ap[2])
        m = parent_m @ pose(off, r) @ np.diag([sc[0], sc[1], sc[2], 1.0])
        for c in b.get("cubes", []):
            cr = vec(c, "rotation")
            if any(cr):
                cp = vec(c, "pivot") if c.get("pivot") else pv
                cm = m @ pose((-(cp[0] - pv[0]), -(cp[1] - pv[1]), cp[2] - pv[2]), (math.radians(cr[0]), math.radians(cr[1]), -math.radians(cr[2])))
                piv = cp
            else:
                cm, piv = m, pv
            for pts, uvs in box_quads(c, piv, tex_w, tex_h):
                P = np.array([cm @ np.array([p[0], p[1], p[2], 1.0]) for p in pts])[:, :3]
                quads.append((P, uvs))
        for ch in children.get(b["name"], []):
            build(ch, m, pv)

    for r in roots:
        build(r, np.eye(4), None)
    return quads, tex_w, tex_h


def view_matrix(yaw_deg=-26, pitch_deg=12):
    # In the world the model is flipped (x and y), then turned to face the camera, then a three-quarter turn and a look down.
    flip = np.diag([-1.0, -1.0, 1.0])
    face = rot_zyx(0, math.pi, 0)
    turn = rot_zyx(0, math.radians(yaw_deg), 0)
    tilt = rot_zyx(math.radians(pitch_deg), 0, 0)
    return tilt @ turn @ face @ flip


def render(species):
    folder = os.path.join(MODELS, species)
    info = json.load(open(os.path.join(folder, "info.json")))
    geo = json.load(open(os.path.join(folder, "model.geo.json")))
    tex_id = info.get("texture") or f"retromon:textures/pokemon/{species}.png"
    ns, path = tex_id.split(":", 1)
    tex = np.array(Image.open(os.path.join(ROOT, "..", ns, path)).convert("RGBA")).astype(np.float32)
    hidden = [t["part"] for t in info.get("parts", []) if t.get("visible") is False]
    quads, tw, th = model_quads(geo, idle_pose(folder, info), hidden)
    if not quads:
        return None
    th_img, tw_img = tex.shape[0], tex.shape[1]
    su, sv = tw_img / tw, th_img / th
    V = view_matrix()
    proj = [(np.array([V @ p for p in P]), uvs) for P, uvs in quads]
    allp = np.concatenate([P for P, _ in proj])
    mn, mx = allp.min(axis=0), allp.max(axis=0)
    span = max(mx[0] - mn[0], mx[1] - mn[1], 1e-3)
    N = SIZE * SS
    scale = N * 0.86 / span
    cx, cy = (mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2
    img = np.zeros((N, N, 4), np.float32)
    depth = np.full((N, N), -1e9, np.float32)
    light = np.array([-0.45, -0.65, 0.6])
    light /= np.linalg.norm(light)
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
        shade = 0.62 + 0.38 * max(0.0, float(nrm @ light))
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
            ok = inside & (col[..., 3] > 100) & (z > sub_d)
            if not ok.any():
                continue
            sub_d[ok] = z[ok]
            sub_i = img[y0:y1 + 1, x0:x1 + 1]
            c2 = col[ok].copy()
            c2[:, :3] *= shade
            c2[:, 3] = 255
            sub_i[ok] = c2
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    return out.resize((SIZE, SIZE), Image.Resampling.BOX)


def main():
    os.makedirs(OUT, exist_ok=True)
    only = set(sys.argv[1:])
    done = 0
    for sp in sorted(os.listdir(MODELS)):
        if only and sp not in only:
            continue
        if not os.path.exists(os.path.join(MODELS, sp, "model.geo.json")):
            continue
        try:
            im = render(sp)
        except Exception as e:
            print("failed", sp, e)
            continue
        if im is not None:
            im.save(os.path.join(OUT, sp + ".png"), optimize=True)
            done += 1
    print(done, "portraits")


if __name__ == "__main__":
    main()
