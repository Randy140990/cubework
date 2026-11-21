#!/usr/bin/env python3
"""Retromon's people, second pass: 64x64 Minecraft skins (classic 4-pixel arms) painted with care.

Every part is shaded as a block lit from the top front: lighter at the top and on the front, darker down the sides
and at the back and low down. Clothes have folds, seams, hems, cuffs, collars, pockets and soles; faces have whites,
irises, brows, a nose shadow and a mouth; hair is two layers (the head itself, and the outer layer standing off it
for volume: fringes, spikes, buns, ponytails, long hair down the back). Coats, aprons, skirts and jackets are on the
outer layer so they stand off the body.

    python3 tools/skins2.py OUT_DIR [--only name,name]
The people are in PEOPLE at the bottom: each a short description.
"""
import os, sys, random
from PIL import Image


def C(h):
    h = h.lstrip("#"); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def sh(c, k):
    """Lighter (k > 0) or darker (k < 0), keeping the hue."""
    if k >= 0: return tuple(min(255, int(v + (255 - v) * k)) for v in c[:3])
    return tuple(max(0, int(v * (1 + k))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def faces(ox, oy, w, h, d):
    """The six faces of a part in the skin layout: (x, y, w, h) each."""
    return {"top": (ox + d, oy, w, d), "bottom": (ox + d + w, oy, w, d), "right": (ox, oy + d, d, h), "front": (ox + d, oy + d, w, h),
            "left": (ox + d + w, oy + d, d, h), "back": (ox + 2 * d + w, oy + d, w, h)}


# Each part: base (x, y), outer layer (x, y), size (w, h, d).
PARTS = {"head": ((0, 0), (32, 0), (8, 8, 8)), "body": ((16, 16), (16, 32), (8, 12, 4)), "rarm": ((40, 16), (40, 32), (4, 12, 4)),
         "larm": ((32, 48), (48, 48), (4, 12, 4)), "rleg": ((0, 16), (0, 32), (4, 12, 4)), "lleg": ((16, 48), (0, 48), (4, 12, 4))}
INNER = {"rarm": "left", "larm": "right", "rleg": "left", "lleg": "right"}   # the side facing the body: in shadow
SIDE_K = {"front": 0.0, "right": -0.07, "left": -0.07, "back": -0.13, "top": 0.06, "bottom": -0.2}


class Skin:
    def __init__(self, seed):
        self.im = Image.new("RGBA", (64, 64), (0, 0, 0, 0)); self.p = self.im.load(); self.r = random.Random(seed)

    def face(self, part, side, outer=False):
        base, over, (w, h, d) = PARTS[part]
        o = over if outer else base
        return faces(o[0], o[1], w, h, d)[side]

    def px(self, x, y, c, a=255):
        if 0 <= x < 64 and 0 <= y < 64: self.p[x, y] = (c[0], c[1], c[2], a)

    def get(self, x, y):
        return self.p[x, y]

    def paint(self, part, col, rows=None, sides=("top", "bottom", "right", "front", "left", "back"), outer=False, grad=(0.12, -0.18), noise=0.025,
              cols=None):
        """Fills a part's faces (rows: from..to down the sides, top and bottom only when rows start at 0 / end at h)."""
        _, _, (w, h, d) = PARTS[part]
        r0, r1 = rows or (0, h)
        for s in sides:
            x, y, fw, fh = self.face(part, s, outer)
            if s in ("top", "bottom"):
                if (s == "top" and r0 > 0) or (s == "bottom" and r1 < h): continue
                for j in range(fh):
                    for i in range(fw):
                        self.px(x + i, y + j, sh(col, SIDE_K[s] + (self.r.random() - 0.5) * noise * 2))
                continue
            inner = INNER.get(part) == s
            for j in range(r0, r1):
                t = j / max(1, h - 1)
                k = SIDE_K[s] + grad[0] + (grad[1] - grad[0]) * t - (0.09 if inner else 0)
                for i in range(fw):
                    if cols and not (cols[0] <= i < cols[1]): continue
                    e = -0.05 if (s in ("front", "back") and (i == 0 or i == fw - 1)) else (0.03 if s == "front" and i == fw // 2 - 1 else 0)
                    self.px(x + i, y + j, sh(col, k + e + (self.r.random() - 0.5) * noise * 2 * (0.5 if sum(col) > 600 else 1)))

    def around(self, part, row, col, outer=False, sides=("right", "front", "left", "back")):
        """One row all the way round a part."""
        for s in sides:
            x, y, fw, fh = self.face(part, s, outer)
            for i in range(fw): self.px(x + i, y + row, sh(col, SIDE_K[s]))

    def front(self, part, i, j, col, outer=False, side="front"):
        x, y, fw, fh = self.face(part, side, outer)
        if 0 <= i < fw and 0 <= j < fh: self.px(x + i, y + j, col)

    def save(self, path):
        self.im.save(path)


# ---------- the person ----------

TONES = {"pale": C("F5DACA"), "fair": C("EEC3A1"), "light": C("E2AE86"), "tan": C("C88E62"), "medium": C("B07448"), "brown": C("8E5A36"),
         "deep": C("6B4128"), "dark": C("4E2E1C")}
EYES = {"brown": C("5A3A22"), "dark": C("2D2430"), "blue": C("3A6FD0"), "green": C("3A8A4A"), "grey": C("6E7A88"), "hazel": C("8A6A34"),
        "amber": C("B87A22"), "violet": C("6A4AA8"), "teal": C("2A9A92")}


def person(spec, seed):
    """spec: a dict (see PEOPLE below)."""
    S = Skin(seed)
    tone = TONES[spec.get("tone", "fair")]
    hairc = C(spec.get("hairc", "5A3A22"))
    # Skin everywhere first (hands, neck, legs show where clothes don't cover).
    for part in PARTS: S.paint(part, tone, grad=(0.04, -0.06), noise=0.015)
    # The head: a little shadow under the chin and round the back.
    S.around("head", 7, sh(tone, -0.08))
    face(S, spec, tone, hairc)
    legs(S, spec, tone)
    top(S, spec, tone)
    hair(S, spec, hairc, tone)
    extras(S, spec, tone, hairc)
    more(S, spec, tone, hairc)
    return S


def face(S, spec, tone, hairc):
    x0, y0 = 8, 8   # the head's front face
    eye = EYES[spec.get("eyes", "brown")]
    fem = spec.get("fem", False)
    white = (250, 248, 244)
    style = spec.get("eyestyle", "round")
    # Eyes on row 4: white on the outside, the iris inside (looking at you), a dark top lid.
    for (wx, ix) in ((9, 10), (14, 13)):
        S.px(x0 + wx - 8, y0 + 4, white); S.px(x0 + ix - 8, y0 + 4, eye)
        S.px(x0 + ix - 8, y0 + 3, sh(eye, -0.45) if style != "narrow" else sh(hairc, -0.3))
        if style != "narrow": S.px(x0 + wx - 8, y0 + 3, sh(tone, -0.12))
        if fem: S.px(x0 + (8 if wx == 9 else 15) - 8, y0 + 4, sh(hairc, -0.55))   # lashes at the outer corner
    # Brows, in the hair's colour (darker), over the eyes.
    brow = sh(hairc, -0.35) if spec.get("hairc") else C("4A3426")
    by = y0 + 2
    for bx in (9, 10, 13, 14):
        S.px(x0 + bx - 8, by, brow)
    if spec.get("brows") == "stern":
        S.px(x0 + 1, by - 0, sh(tone, -0.05)); S.px(x0 + 6, by - 0, sh(tone, -0.05)); S.px(x0 + 1, by + 1 - 0, brow); S.px(x0 + 6, by + 1, brow)
    # Nose shadow, mouth, cheeks.
    S.px(x0 + 3, y0 + 5, sh(tone, -0.12)); S.px(x0 + 4, y0 + 5, sh(tone, -0.06))
    mouth = C(spec.get("lips", "B05A50" if not fem else "C25060"))
    if spec.get("smile", True):
        S.px(x0 + 3, y0 + 6, mouth); S.px(x0 + 4, y0 + 6, mouth); S.px(x0 + 2, y0 + 6, sh(tone, -0.12)); S.px(x0 + 5, y0 + 6, sh(tone, -0.12))
    else:
        S.px(x0 + 3, y0 + 6, sh(mouth, -0.1)); S.px(x0 + 4, y0 + 6, sh(mouth, -0.1))
    if spec.get("blush") or fem: S.px(x0 + 1, y0 + 5, mix(tone, C("F08A90"), 0.35)); S.px(x0 + 6, y0 + 5, mix(tone, C("F08A90"), 0.35))
    if spec.get("freckles"): S.px(x0 + 2, y0 + 5, sh(tone, -0.18)); S.px(x0 + 5, y0 + 5, sh(tone, -0.18))
    if spec.get("beard"):
        b = C(spec.get("beardc", spec.get("hairc", "5A3A22")))
        for i in range(8):
            for j in (6, 7): S.px(x0 + i, y0 + j, sh(b, (S.r.random() - 0.5) * 0.12))
        S.px(x0 + 3, y0 + 6, sh(b, -0.3)); S.px(x0 + 4, y0 + 6, sh(b, -0.3))
        for s in ("right", "left"):
            x, y, w, h = S.face("head", s)
            for i in range(w):
                if (s == "right" and i >= 5) or (s == "left" and i <= 2): S.px(x + i, y + 6, b); S.px(x + i, y + 7, b)
    if spec.get("moustache"):
        m = C(spec.get("beardc", spec.get("hairc", "5A3A22")))
        for i in (2, 3, 4, 5): S.px(x0 + i, y0 + 5, m)
    if spec.get("glasses"):
        g = C(spec.get("glassc", "2A2A34"))
        for i in (1, 2, 5, 6): S.px(x0 + i, y0 + 3, g)
        S.px(x0 + 3, y0 + 4, g); S.px(x0 + 4, y0 + 4, g); S.px(x0 + 0, y0 + 4, g); S.px(x0 + 7, y0 + 4, g)
        lens = (210, 232, 240)
        S.px(x0 + 1, y0 + 4, lens if spec.get("lens") else (250, 248, 244)); S.px(x0 + 6, y0 + 4, lens if spec.get("lens") else (250, 248, 244))
    if spec.get("shades"):
        g = C("1E1E26")
        for i in range(1, 7): S.px(x0 + i, y0 + 3, g); S.px(x0 + i, y0 + 4, g)
        S.px(x0 + 2, y0 + 3, (90, 110, 140)); S.px(x0 + 5, y0 + 3, (90, 110, 140))


def legs(S, spec, tone):
    kind = spec.get("bottom", "trousers"); col = C(spec.get("botc", "3A4A6A"))
    shoe = C(spec.get("shoec", "3A3030"))
    for leg in ("rleg", "lleg"):
        if kind in ("trousers", "jeans", "combat", "overalls"):
            S.paint(leg, col, (0, 10), grad=(0.04, -0.1))
            # A seam down the outside, a crease at the knee, a turned hem.
            S.around(leg, 6, sh(col, -0.08), sides=("front",))
            x, y, w, h = S.face(leg, "right" if leg == "rleg" else "left")
            for j in range(10): S.px(x + (0 if leg == "rleg" else w - 1), y + j, sh(col, -0.16))
            S.around(leg, 9, sh(col, -0.18))
            if kind == "jeans":
                fx, fy, fw, fh = S.face(leg, "front")
                for j in range(0, 10, 3): S.px(fx + (1 if leg == "rleg" else 2), fy + j, sh(col, 0.12))
            if kind == "combat":
                # Patches of camouflage and a pocket on the thigh.
                for s in ("right", "front", "left", "back"):
                    x, y, w, h = S.face(leg, s)
                    for _ in range(5): S.px(x + S.r.randrange(w), y + S.r.randrange(9), sh(col, S.r.choice([-0.25, 0.18, -0.12])))
                x, y, w, h = S.face(leg, "right" if leg == "rleg" else "left")
                for j in (3, 4, 5):
                    for i in range(1, 3): S.px(x + i, y + j, sh(col, -0.08))
                S.px(x + 1, y + 3, sh(col, -0.25)); S.px(x + 2, y + 3, sh(col, -0.25))
        elif kind == "shorts":
            S.paint(leg, col, (0, 5), grad=(0.04, -0.08)); S.around(leg, 4, sh(col, -0.15))
        elif kind in ("skirt", "dress"):
            S.paint(leg, col, (0, 3 if kind == "skirt" else 6), grad=(0.04, -0.06))
        if spec.get("socks"):
            s = C(spec["socks"])
            for j in (7, 8, 9): S.around(leg, j, s)
        # Shoes: two rows and the sole, a lace highlight on the front.
        S.paint(leg, shoe, (10, 12), grad=(0.1, -0.15))
        S.paint(leg, sh(shoe, -0.35), (12, 12), sides=("bottom",))
        x, y, w, h = S.face(leg, "bottom"); [S.px(x + i, y + j, sh(shoe, -0.4)) for i in range(w) for j in range(h)]
        fx, fy, fw, fh = S.face(leg, "front"); S.px(fx + 1, fy + 10, sh(shoe, 0.35)); S.px(fx + 2, fy + 10, sh(shoe, 0.35))
        if spec.get("soles"): S.around(leg, 11, C(spec["soles"]))
    # Skirts and dress hems stand off the legs on the outer layer, flaring a little darker at the edge.
    if kind in ("skirt", "dress"):
        L = 4 if kind == "skirt" else 7
        for leg in ("rleg", "lleg"):
            S.paint(leg, col, (0, L), outer=True, grad=(0.06, -0.1), sides=("right", "front", "left", "back"))
            S.around(leg, L - 1, sh(col, -0.2), outer=True)


def top(S, spec, tone):
    kind = spec.get("top", "tee"); col = C(spec.get("topc", "3A6FD0"))
    sleeves = spec.get("sleeves", {"tee": 4, "tank": 0, "shirt": 12, "jumper": 12, "hoodie": 12, "dress": 4, "blouse": 12, "none": 0}.get(kind, 12))
    if kind == "bare":
        return
    S.paint("body", col, grad=(0.08, -0.12))
    for arm in ("rarm", "larm"):
        if sleeves: S.paint(arm, col, (0, sleeves), grad=(0.08, -0.08))
        if sleeves:
            S.around(arm, sleeves - 1, sh(col, -0.14))   # the cuff
            # The shoulder (the top of the arm) is part of the sleeve: covered.
            x, y, w, h = S.face(arm, "top"); [S.px(x + i, y + j, sh(col, 0.04)) for i in range(w) for j in range(h)]
    x, y, w, h = S.face("body", "front")
    # Folds: little darker creases under the arms and at the waist; the hem darker.
    for (i, j) in ((0, 3), (0, 4), (7, 3), (7, 4), (2, 9), (5, 10), (3, 6)): S.px(x + i, y + j, sh(col, -0.12))
    S.around("body", 11, sh(col, -0.2))
    neck = spec.get("neck", {"shirt": "collar", "blouse": "collar", "tee": "crew", "tank": "crew", "jumper": "crew", "hoodie": "hood"}.get(kind, "crew"))
    if neck == "crew":
        for i in (3, 4): S.px(x + i, y, sh(tone, -0.08))
        for i in (2, 5): S.px(x + i, y, sh(col, -0.2))
    elif neck == "v":
        for (i, j) in ((3, 0), (4, 0), (3, 1), (4, 1), (3, 2)): S.px(x + i, y + j, sh(tone, -0.06))
    elif neck == "collar":
        cc = C(spec.get("collarc", spec.get("topc", "F2F2F2"))); cc = sh(cc, 0.18)
        for (i, j) in ((1, 0), (2, 0), (2, 1), (5, 0), (6, 0), (5, 1)): S.px(x + i, y + j, cc)
        S.px(x + 3, y, sh(tone, -0.1)); S.px(x + 4, y, sh(tone, -0.1))
        if spec.get("buttons", True):
            for j in range(2, 11, 3): S.px(x + 4, y + j, sh(col, 0.3))
    elif neck == "hood":
        hc = sh(col, -0.1)
        bx, by, bw, bh = S.face("body", "back")
        for i in range(bw): S.px(bx + i, by, hc); S.px(bx + i, by + 1, hc)
        for i in (1, 2, 5, 6): S.px(x + i, y, hc)
        S.px(x + 3, y + 1, (240, 240, 240)); S.px(x + 4, y + 1, (240, 240, 240))   # the strings
        S.px(x + 3, y + 2, (240, 240, 240)); S.px(x + 4, y + 2, (240, 240, 240))
        for i in range(2, 6): S.px(x + i, y + 8, sh(col, -0.15))   # the front pocket
        S.px(x + 2, y + 7, sh(col, -0.15)); S.px(x + 5, y + 7, sh(col, -0.15))
    if kind == "tank":
        for arm in ("rarm", "larm"):
            pass
        for (i, j) in ((0, 0), (1, 0), (6, 0), (7, 0), (0, 1), (7, 1)): S.px(x + i, y + j, sh(tone, -0.05))
    if spec.get("stripe"):
        sc = C(spec["stripe"])
        for row in spec.get("stripe_rows", (4,)):
            S.around("body", row, sc)
            for arm in ("rarm", "larm"):
                if sleeves > row: S.around(arm, row, sc)
    if spec.get("zip"):
        for j in range(1, 11): S.px(x + 4, y + j, sh(col, -0.25))
    # A coat, jacket or apron, on the outer layer so it stands off the body.
    coat = spec.get("coat")
    if coat:
        cc = C(spec.get("coatc", "F4F4F2"))
        length = spec.get("coatlen", 12)
        S.paint("body", cc, (0, 12), outer=True, grad=(0.08, -0.08))
        for arm in ("rarm", "larm"):
            S.paint(arm, cc, (0, spec.get("coatsleeve", 12)), outer=True, grad=(0.08, -0.06))
            S.around(arm, spec.get("coatsleeve", 12) - 1, sh(cc, -0.12), outer=True)
            ox, oy, ow, oh = S.face(arm, "top", outer=True); [S.px(ox + i, oy + j, sh(cc, 0.05)) for i in range(ow) for j in range(oh)]
        ox, oy, ow, oh = S.face("body", "front", outer=True)
        # Open down the front: the shirt shows between the lapels.
        for j in range(12):
            for i in (3, 4): S.p[ox + i, oy + j] = (0, 0, 0, 0)
        for j in range(4): S.px(ox + 2, oy + j, sh(cc, -0.15)); S.px(ox + 5, oy + j, sh(cc, -0.15))
        S.px(ox + 1, oy + 7, sh(cc, -0.15)); S.px(ox + 6, oy + 7, sh(cc, -0.15))  # pockets
        if length > 12:
            for leg in ("rleg", "lleg"):
                S.paint(leg, cc, (0, length - 12), outer=True, grad=(0.0, -0.08), sides=("right", "left", "back", "front"))
                fx, fy, fw, fh = S.face(leg, "front", outer=True)
                inner = 3 if leg == "rleg" else 0
                for j in range(length - 12): S.p[fx + inner, fy + j] = (0, 0, 0, 0)
    apron = spec.get("apron")
    if apron:
        ac = C(apron)
        ox, oy, ow, oh = S.face("body", "front", outer=True)
        for j in range(3, 12):
            for i in range(1, 7): S.px(ox + i, oy + j, sh(ac, 0.04 - j * 0.008))
        S.px(ox + 1, oy + 2, ac); S.px(ox + 6, oy + 2, ac)
        for leg in ("rleg", "lleg"):
            fx, fy, fw, fh = S.face(leg, "front", outer=True)
            for j in range(4):
                for i in range(4): S.px(fx + i, fy + j, sh(ac, -0.04 - j * 0.02))


def hair(S, spec, hc, tone):
    style = spec.get("hair", "short")
    if style == "bald":
        x, y, w, h = S.face("head", "top"); [S.px(x + i, y + j, sh(tone, 0.08)) for i in range(w) for j in range(h)]
        for s in ("right", "left", "back"):
            x, y, w, h = S.face("head", s)
            for i in range(w):
                for j in (3, 4): S.px(x + i, y + j, sh(hc, -0.05 + (S.r.random() - 0.5) * 0.1))
        return
    # The head's own hair: the top, the back and the sides down to the ears, lighter on top.
    x, y, w, h = S.face("head", "top")
    for i in range(w):
        for j in range(h): S.px(x + i, y + j, sh(hc, 0.14 + (0.07 if i % 3 == 0 else -0.04 if i % 3 == 2 else 0) - abs(j - 3.5) * 0.02 + (S.r.random() - 0.5) * 0.06))
    down = {"short": 3, "spiky": 3, "swept": 3, "long": 8, "bob": 6, "ponytail": 4, "bun": 4, "pigtails": 6, "buzz": 2, "afro": 5, "braids": 8}.get(style, 3)
    for s in ("right", "left", "back"):
        x, y, w, h = S.face("head", s)
        dd = 8 if s == "back" and style in ("long", "bob", "ponytail", "bun", "pigtails", "afro", "braids") else down
        for j in range(dd):
            for i in range(w):
                if s == "right" and j >= 3 and i >= 5 and style not in ("long", "bob", "afro", "braids"): continue   # the ear shows in front
                if s == "left" and j >= 3 and i <= 2 and style not in ("long", "bob", "afro", "braids"): continue
                k = 0.05 - j * 0.03 + (0.06 if (i + j) % 3 == 0 else 0) - (0.08 if i % 2 else 0)
                S.px(x + i, y + j, sh(hc, k))
    # The fringe across the front.
    x0, y0 = 8, 8
    fr = {"short": [2, 2, 1, 1, 1, 1, 2, 2], "spiky": [2, 1, 2, 1, 2, 1, 2, 2], "swept": [3, 2, 2, 1, 1, 1, 1, 2], "long": [2, 2, 1, 1, 1, 1, 2, 2],
          "bob": [3, 2, 2, 2, 2, 2, 2, 3], "ponytail": [2, 1, 1, 1, 1, 1, 1, 2], "bun": [2, 1, 1, 1, 1, 1, 1, 2], "pigtails": [2, 2, 1, 1, 1, 1, 2, 2],
          "buzz": [1, 1, 1, 1, 1, 1, 1, 1], "afro": [2, 2, 2, 2, 2, 2, 2, 2], "braids": [2, 2, 1, 1, 1, 1, 2, 2]}[style]
    for i, n in enumerate(fr):
        for j in range(n): S.px(x0 + i, y0 + j, sh(hc, 0.06 - j * 0.06))
    if style in ("long", "bob", "pigtails", "braids"):
        for j in range(2, 7): S.px(x0, y0 + j, sh(hc, -0.05)); S.px(x0 + 7, y0 + j, sh(hc, -0.05))
    # The outer layer: volume.
    def hat(side, cells, k=0.0):
        x, y, w, h = S.face("head", side, outer=True)
        for (i, j) in cells: S.px(x + i, y + j, sh(hc, k + (0.06 if i % 3 == 0 else -0.05 if i % 3 == 2 else 0) + (S.r.random() - 0.5) * 0.06 - j * 0.02))
    allw = [(i, j) for i in range(8) for j in range(8)]
    if style in ("short", "swept", "ponytail", "bun", "pigtails", "long", "bob", "braids", "afro", "spiky"):
        hat("top", allw, 0.12)
        hat("back", [(i, j) for i in range(8) for j in range({"short": 3, "swept": 3, "spiky": 3}.get(style, 8 if style in ("long", "bob", "afro", "braids") else 5))], -0.04)
        sd = {"short": 2, "swept": 2, "spiky": 2, "ponytail": 3, "bun": 3, "pigtails": 3, "long": 8, "bob": 6, "braids": 8, "afro": 6}[style]
        hat("right", [(i, j) for i in range(8) for j in range(sd) if not (j >= 2 and i >= 5 and style in ("short", "swept", "spiky"))], -0.02)
        hat("left", [(i, j) for i in range(8) for j in range(sd) if not (j >= 2 and i <= 2 and style in ("short", "swept", "spiky"))], -0.02)
        hat("front", [(i, 0) for i in range(8)] + ([(0, 1), (7, 1), (1, 1), (6, 1)] if style not in ("buzz",) else []), 0.08)
    if style == "swept":
        hat("front", [(0, 1), (1, 1), (2, 1), (0, 2), (1, 2)], 0.05)
    if style == "spiky":
        x, y, w, h = S.face("head", "top", outer=True)
        for i in range(8):
            if i % 2: S.p[x + i, y] = (0, 0, 0, 0); S.p[x + i, y + 7] = (0, 0, 0, 0)
        hat("front", [(1, 1), (3, 1), (5, 1), (6, 1)], 0.1)
    if style == "afro":
        hat("front", [(i, j) for i in range(8) for j in range(2)] + [(0, 2), (7, 2), (0, 3), (7, 3)], 0.02)
    if style in ("long", "braids"):
        # Down the back, over the shoulders.
        ox, oy, ow, oh = S.face("body", "back", outer=True)
        n = 7 if style == "long" else 9
        for j in range(n):
            for i in range(8):
                if style == "braids" and i not in (1, 2, 5, 6): continue
                S.px(ox + i, oy + j, sh(hc, -0.02 - j * 0.03 + (0.06 if (i + j) % 2 == 0 and style == "braids" else 0)))
    if style == "ponytail":
        ox, oy, ow, oh = S.face("body", "back", outer=True)
        for j in range(6):
            for i in (3, 4) if j > 0 else (2, 3, 4, 5): S.px(ox + i, oy + j, sh(hc, -0.02 - j * 0.04))
        hat("back", [(3, 5), (4, 5), (3, 6), (4, 6), (3, 7), (4, 7)], -0.12)   # the tie
        if spec.get("tiec"):
            x, y, w, h = S.face("head", "back", outer=True); S.px(x + 3, y + 4, C(spec["tiec"])); S.px(x + 4, y + 4, C(spec["tiec"]))
    if style == "bun":
        hat("top", [(3, 4), (4, 4), (3, 5), (4, 5), (2, 5), (5, 5)], 0.18)
        hat("back", [(2, 0), (3, 0), (4, 0), (5, 0), (2, 1), (3, 1), (4, 1), (5, 1)], 0.1)
    if style == "pigtails":
        hat("right", [(i, j) for i in (2, 3, 4) for j in range(3, 8)], -0.05)
        hat("left", [(i, j) for i in (3, 4, 5) for j in range(3, 8)], -0.05)
        if spec.get("tiec"):
            x, y, w, h = S.face("head", "right", outer=True); [S.px(x + i, y + 3, C(spec["tiec"])) for i in (2, 3, 4)]
            x, y, w, h = S.face("head", "left", outer=True); [S.px(x + i, y + 3, C(spec["tiec"])) for i in (3, 4, 5)]


def extras(S, spec, tone, hc):
    hatk = spec.get("hat")
    if hatk:
        col = C(spec.get("hatc", "D8343C"))
        def ov(side, cells, k=0.0):
            x, y, w, h = S.face("head", side, outer=True)
            for (i, j) in cells: S.px(x + i, y + j, sh(col, k + (S.r.random() - 0.5) * 0.06))
        allt = [(i, j) for i in range(8) for j in range(8)]
        if hatk in ("cap", "beanie", "hardhat", "police", "nurse", "chef", "sunhat", "sailor", "bandana", "visor", "headscarf"):
            rows = {"cap": 3, "beanie": 3, "hardhat": 3, "police": 3, "nurse": 2, "chef": 3, "sunhat": 2, "sailor": 2, "bandana": 2, "visor": 1, "headscarf": 4}[hatk]
            if hatk not in ("visor",): ov("top", allt, 0.1)
            for s in ("front", "back", "right", "left"): ov(s, [(i, j) for i in range(8) for j in range(rows)], -0.02)
            if hatk in ("cap", "police", "visor"):
                # The brim: a darker band across the front, shading the eyes.
                b = sh(col, -0.25)
                x, y, w, h = S.face("head", "front", outer=True)
                for i in range(8): S.px(x + i, y + rows - 1, b)
                if spec.get("badgec"): S.px(x + 3, y + 1, C(spec["badgec"])); S.px(x + 4, y + 1, C(spec["badgec"]))
                if spec.get("hatlogo"): lc = C(spec["hatlogo"]); S.px(x + 3, y + 0, lc); S.px(x + 4, y + 0, lc); S.px(x + 3, y + 1, lc); S.px(x + 4, y + 1, lc)
            if hatk == "beanie":
                x, y, w, h = S.face("head", "front", outer=True)
                for i in range(8): S.px(x + i, y + 2, sh(col, -0.18))
                xt, yt, wt, ht = S.face("head", "top", outer=True); S.px(xt + 3, yt + 3, sh(col, 0.3)); S.px(xt + 4, yt + 4, sh(col, 0.3))
            if hatk == "hardhat":
                x, y, w, h = S.face("head", "front", outer=True); [S.px(x + i, y + 2, sh(col, -0.2)) for i in range(8)]
                S.px(x + 3, y + 0, (255, 250, 210)); S.px(x + 4, y + 0, (255, 250, 210))
            if hatk == "nurse":
                x, y, w, h = S.face("head", "front", outer=True)
                rc = C(spec.get("crossc", "D8343C")); S.px(x + 3, y, rc); S.px(x + 4, y, rc); S.px(x + 3, y + 1, rc); S.px(x + 4, y + 1, rc)
            if hatk == "chef":
                xt, yt, wt, ht = S.face("head", "top", outer=True)
            if hatk == "sunhat":
                b = sh(col, -0.15)
                for s in ("front", "back", "right", "left"):
                    x, y, w, h = S.face("head", s, outer=True)
                    for i in range(8): S.px(x + i, y + 2, b)
                if spec.get("bandc"):
                    for s in ("front", "back", "right", "left"):
                        x, y, w, h = S.face("head", s, outer=True)
                        for i in range(8): S.px(x + i, y + 1, C(spec["bandc"]))
    # Straps for a bag: diagonal across the front, the bag on the back (outer layer).
    bag = spec.get("bag")
    if bag:
        bc = C(spec.get("bagc", "7A5030"))
        x, y, w, h = S.face("body", "front", outer=True)
        if bag == "backpack":
            for j in range(0, 9): S.px(x + 1, y + j, sh(bc, -0.1)); S.px(x + 6, y + j, sh(bc, -0.1))
            ox, oy, ow, oh = S.face("body", "back", outer=True)
            for j in range(1, 10):
                for i in range(1, 7): S.px(ox + i, oy + j, sh(bc, 0.05 - j * 0.02))
            for i in range(2, 6): S.px(ox + i, oy + 5, sh(bc, -0.2))
            S.px(ox + 3, oy + 6, sh(bc, 0.3)); S.px(ox + 4, oy + 6, sh(bc, 0.3))
        else:
            for j in range(8): S.px(x + j % 8, y + j, sh(bc, -0.1))
            rx, ry, rw, rh = S.face("body", "left", outer=True)
            for j in range(7, 11):
                for i in range(4): S.px(rx + i, ry + j, sh(bc, 0.05 - (j - 7) * 0.04))
    if spec.get("belt"):
        bl = C(spec["belt"]); S.around("body", 10, bl)
        x, y, w, h = S.face("body", "front"); S.px(x + 3, y + 10, (220, 200, 120)); S.px(x + 4, y + 10, (220, 200, 120))
    if spec.get("tie"):
        t = C(spec["tie"]); x, y, w, h = S.face("body", "front")
        for j in range(1, 8): S.px(x + 3, y + j, sh(t, -0.05 * (j % 2))); S.px(x + 4, y + j, sh(t, -0.05 * ((j + 1) % 2)))
        S.px(x + 3, y + 8, sh(t, -0.2)); S.px(x + 4, y + 8, sh(t, -0.2))
    if spec.get("scarf"):
        sc = C(spec["scarf"]); S.around("body", 0, sc); S.around("body", 1, sh(sc, -0.1))
        x, y, w, h = S.face("body", "front"); [S.px(x + 5, y + j, sh(sc, -0.06 * j)) for j in range(2, 6)]
    if spec.get("badge"):
        b = C(spec["badge"]); x, y, w, h = S.face("body", "front", outer=bool(spec.get("coat"))); S.px(x + 1, y + 2, b); S.px(x + 2, y + 2, sh(b, 0.2)); S.px(x + 1, y + 3, sh(b, -0.2))
    if spec.get("gloves"):
        g = C(spec["gloves"])
        for arm in ("rarm", "larm"):
            for j in (9, 10, 11): S.around(arm, j, g)
            x, y, w, h = S.face(arm, "bottom"); [S.px(x + i, y + j, sh(g, -0.2)) for i in range(w) for j in range(h)]
    if spec.get("letter"):
        # The team's mark on the chest (their own mark, not anyone else's): big on a shirt, small on an open jacket.
        lc = C(spec["letter"])
        if spec.get("coat"):
            x, y, w, h = S.face("body", "front", outer=True)
            for (i, j) in ((6, 2), (5, 3), (7, 3), (6, 4)): S.px(x + i, y + j, lc)
            S.px(x + 6, y + 3, sh(lc, -0.45))
        else:
            x, y, w, h = S.face("body", "front")
            for (i, j) in spec.get("mark", [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5)]): S.px(x + i, y + j, lc)
    if spec.get("pendant"):
        pc = C(spec["pendant"]); x, y, w, h = S.face("body", "front"); S.px(x + 3, y + 2, pc); S.px(x + 4, y + 2, pc); S.px(x + 3, y + 3, sh(pc, -0.2))
    if spec.get("sash"):
        sc = C(spec["sash"]); x, y, w, h = S.face("body", "front")
        for i in range(8): S.px(x + i, y + i + 1, sc); S.px(x + i, y + i + 2, sh(sc, -0.15))
    if spec.get("wristband"):
        wb = C(spec["wristband"])
        for arm in ("rarm", "larm"): S.around(arm, 9, wb)
    if spec.get("earrings"):
        e = C(spec["earrings"])
        x, y, w, h = S.face("head", "right"); S.px(x + 6, y + 5, e)
        x, y, w, h = S.face("head", "left"); S.px(x + 1, y + 5, e)


# ---------- more variety (the 15:14 notes): patterns, two-tone sleeves, closed and quilted jackets, chest motifs,
# hoods up, caps backwards, bucket hats, berets, headbands, bobble hats, headphones, joggers ----------
MOTIFS = {
    "heart": [".x.x.", "xxxxx", "xxxxx", ".xxx.", "..x.."], "star": ["..x..", ".xxx.", "xxxxx", ".x.x.", "x...x"],
    "ball": [".xxxx.", "xxxxxx", "oo..oo", "oooooo", ".oooo."], "bolt": ["..xx", ".xx.", "xxxx", ".xx.", "xx.."],
    "leaf": ["...xx", "..xxx", ".xxx.", "xxx..", "x...."], "flame": ["..x..", ".xx..", ".xxx.", "xxxxx", ".xxx."],
    "drop": [".x..", ".xx.", "xxxx", "xxxx", ".xx."], "moon": [".xxx.", "xx...", "xx...", "xx...", ".xxx."],
    "note": ["..xx", "..x.", "..x.", "xxx.", "xx.."], "seven": ["xxxx", "...x", "..x.", ".x..", ".x.."],
    "chevron": ["x....x", ".x..x.", "..xx.."], "crown": ["x.xx.x", "xxxxxx", "xxxxxx"], "paw": ["x.x.x", ".....", ".xxx.", "xxxxx", ".xxx."],
    "flower": [".x.x.", "xxoxx", ".xxx.", "..x..", ".xx.."], "smile": [".xxxx.", "xoxxox", "xxxxxx", "xoooox", ".xxxx."], "wave": ["......", ".x..x.", "x.xx.x", "......"],
}


def more(S, spec, tone, hc):
    col = C(spec.get("topc", "3A6FD0"))
    kind = spec.get("top", "tee")
    sleeves = spec.get("sleeves", {"tee": 4, "tank": 0, "shirt": 12, "jumper": 12, "hoodie": 12, "dress": 4, "blouse": 12, "none": 0}.get(kind, 12))
    # Patterns over the top: checks, dots, stripes down, or a yoke of another colour across the shoulders.
    pat = spec.get("pattern")
    if pat and kind != "bare":
        pc = C(spec.get("patc", "F4F4F4"))
        for s_ in ("front", "back", "right", "left"):
            x, y, w, h = S.face("body", s_)
            for j in range(h):
                for i in range(w):
                    if pat == "check" and (i % 3 == 0 or j % 3 == 0): S.px(x + i, y + j, mix(S.get(x + i, y + j)[:3], pc, 0.45 if (i % 3 == 0 and j % 3 == 0) else 0.28))
                    elif pat == "dots" and (i + (j // 2) * 2) % 4 == 1 and j % 2 == 1: S.px(x + i, y + j, pc)
                    elif pat == "vstripe" and i % 3 == 1: S.px(x + i, y + j, mix(S.get(x + i, y + j)[:3], pc, 0.7))
                    elif pat == "yoke" and j < 4: S.px(x + i, y + j, sh(pc, 0.06 - j * 0.02))
        if pat == "yoke":
            for arm in ("rarm", "larm"):
                if sleeves: S.paint(arm, pc, (0, min(4, sleeves)), grad=(0.08, -0.04))
    # Two-tone: sleeves in another colour (a raglan tee, a baseball top).
    if spec.get("sleevec") and sleeves and kind != "bare":
        sc = C(spec["sleevec"])
        for arm in ("rarm", "larm"):
            S.paint(arm, sc, (0, sleeves), grad=(0.08, -0.08)); S.around(arm, sleeves - 1, sh(sc, -0.14))
            x, y, w, h = S.face(arm, "top"); [S.px(x + i, y + j, sh(sc, 0.04)) for i in range(w) for j in range(h)]
    # A closed jacket: done up, a zip down the middle; quilted (a puffer) with seams across; or sleeves in another colour.
    if spec.get("coat") and spec.get("closed"):
        cc = C(spec.get("coatc", "F4F4F2"))
        ox, oy, ow, oh = S.face("body", "front", outer=True)
        for j in range(12):
            for i in (3, 4): S.px(ox + i, oy + j, sh(cc, 0.06 - j * 0.012))
        for j in range(1, 12): S.px(ox + 4, oy + j, sh(cc, -0.28))
        S.px(ox + 4, oy + 1, (220, 220, 214))
        if spec.get("puffer"):
            for row in (3, 6, 9):
                S.around("body", row, sh(cc, -0.16), outer=True)
                for arm in ("rarm", "larm"): S.around(arm, row, sh(cc, -0.16), outer=True)
        for arm in ("rarm", "larm"): S.around(arm, 11, sh(cc, -0.22), outer=True)
        S.around("body", 11, sh(cc, -0.22), outer=True)
    if spec.get("coat") and spec.get("coatsleevec"):
        sc = C(spec["coatsleevec"])
        for arm in ("rarm", "larm"):
            S.paint(arm, sc, (0, spec.get("coatsleeve", 12)), outer=True, grad=(0.08, -0.06))
            S.around(arm, spec.get("coatsleeve", 12) - 1, sh(sc, -0.15), outer=True)
    # The mark of a team or a motif on the chest: big on a top, or on a jacket done up.
    motif = spec.get("motif")
    if motif:
        m = MOTIFS[motif]; mc = C(spec.get("motifc", "F4F4F4")); m2 = C(spec.get("motif2", "F4F4F4"))
        outer = bool(spec.get("coat") and spec.get("closed"))
        x, y, w, h = S.face("body", "front", outer=outer)
        left = (8 - len(m[0])) // 2 + spec.get("motifx", 0); top = spec.get("motify", 2)
        for j, row in enumerate(m):
            for i, ch in enumerate(row):
                if ch == "x": S.px(x + left + i, y + top + j, sh(mc, -0.04 * (j % 2)))
                elif ch == "o": S.px(x + left + i, y + top + j, m2)
    if spec.get("bigmark"):
        # A team's own mark, big on a jacket done up (the ring cut through).
        lc = C(spec["bigmark"]); x, y, w, h = S.face("body", "front", outer=bool(spec.get("coat") and spec.get("closed")))
        for (i, j) in [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5), (4, 3), (3, 4)]: S.px(x + i, y + j, lc)
    # Joggers: a cuff at the ankle and a stripe down the side.
    if spec.get("bottom") == "joggers" or spec.get("jog"):
        bc = C(spec.get("botc", "3A4A6A"))
        for leg in ("rleg", "lleg"):
            S.around(leg, 9, sh(bc, -0.25))
            if spec.get("stripec"):
                x, y, w, h = S.face(leg, "right" if leg == "rleg" else "left")
                for j in range(9): S.px(x + 1, y + j, C(spec["stripec"])); S.px(x + 2, y + j, sh(C(spec["stripec"]), -0.1))
    # Headwear on the hat layer.
    def ov(side, cells, c, k=0.0):
        x, y, w, h = S.face("head", side, outer=True)
        for (i, j) in cells: S.px(x + i, y + j, sh(c, k + (S.r.random() - 0.5) * 0.06))
    def clear(side, cells):
        x, y, w, h = S.face("head", side, outer=True)
        for (i, j) in cells: S.p[x + i, y + j] = (0, 0, 0, 0)
    allt = [(i, j) for i in range(8) for j in range(8)]
    hw = spec.get("hw")
    hcol = C(spec.get("hwc", spec.get("topc", "3A6FD0")))
    if hw == "hood":
        # The hood up over the head, framing the face.
        for side in ("top", "back", "right", "left"): ov(side, allt, hcol, -0.04 if side != "top" else 0.06)
        x, y, w, h = S.face("head", "front", outer=True)
        clear("front", allt)
        ov("front", [(i, j) for i in range(8) for j in range(2)] + [(i, j) for i in (0, 7) for j in range(2, 8)], hcol, -0.1)
        ov("front", [(i, 2) for i in (1, 6)], hcol, -0.22)
    elif hw in ("capback", "cap"):
        for side in ("front", "back", "right", "left"): clear(side, allt)
        ov("top", allt, hcol, 0.1)
        for side in ("front", "back", "right", "left"): ov(side, [(i, j) for i in range(8) for j in range(3)], hcol, -0.02)
        brim = "back" if hw == "capback" else "front"
        ov(brim, [(i, 2) for i in range(8)], hcol, -0.28)
        if hw == "capback": clear("front", [(3, 2), (4, 2)]); ov("front", [(3, 2), (4, 2)], hcol, -0.4)
        if spec.get("hwlogo"): ov("front" if hw == "cap" else "back", [(3, 0), (4, 0), (3, 1), (4, 1)], C(spec["hwlogo"]))
    elif hw == "bucket":
        for side in ("front", "back", "right", "left"): clear(side, allt)
        ov("top", allt, hcol, 0.08)
        for side in ("front", "back", "right", "left"): ov(side, [(i, j) for i in range(8) for j in range(3)], hcol, -0.02); ov(side, [(i, 3) for i in range(8)], hcol, -0.22)
        if spec.get("bandc"):
            for side in ("front", "back", "right", "left"): ov(side, [(i, 2) for i in range(8)], C(spec["bandc"]), 0.0)
    elif hw == "beret":
        ov("top", allt, hcol, 0.06); ov("top", [(3, 3), (4, 3), (3, 4), (4, 4)], hcol, 0.2)
        for side in ("front", "back", "right", "left"): ov(side, [(i, j) for i in range(8) for j in range(2)], hcol, -0.06)
        ov("left", [(i, 2) for i in range(1, 7)], hcol, -0.12)
    elif hw == "headband":
        for side in ("front", "back", "right", "left"): ov(side, [(i, 2) for i in range(8)], hcol, 0.0)
    elif hw == "bobble":
        for side in ("front", "back", "right", "left"): clear(side, allt)
        ov("top", allt, hcol, 0.08)
        for side in ("front", "back", "right", "left"): ov(side, [(i, j) for i in range(8) for j in range(3)], hcol, -0.02); ov(side, [(i, 2) for i in range(8)], hcol, -0.2)
        ov("top", [(3, 3), (4, 3), (3, 4), (4, 4), (2, 3), (5, 4)], C(spec.get("bobc", "F4F4F4")), 0.05)
        for side in ("front", "back", "right", "left"): ov(side, [(i, 0) for i in range(0, 8, 2)], C(spec.get("bobc", "F4F4F4")), -0.05)
    if spec.get("phones"):
        pc = C(spec["phones"])
        ov("top", [(i, j) for i in (3, 4) for j in range(8)], pc, -0.1)
        ov("right", [(i, j) for i in range(2, 6) for j in range(3, 6)], pc, 0.0); ov("left", [(i, j) for i in range(2, 6) for j in range(3, 6)], pc, 0.0)
        ov("right", [(3, 4), (4, 4)], pc, 0.25); ov("left", [(3, 4), (4, 4)], pc, 0.25)


# ---------- everyone ----------
# Skin tones by name (pale, fair, light, tan, medium, brown, deep, dark); colours as hex.
PEOPLE = {}


def P(key, **k):
    PEOPLE[key] = k


# Story (our own people for the roles; not anyone else's characters).
P("mum", tone="light", hair="bob", hairc="6B4428", eyes="hazel", fem=True, top="blouse", topc="E9967A", collarc="FBEFE6", bottom="skirt", botc="4A5F8A", shoec="7A4E2E", apron="F6EBD8")
P("daisy", tone="fair", hair="long", hairc="8A5A30", eyes="green", fem=True, top="jumper", topc="7FB77E", bottom="skirt", botc="EFE3C8", shoec="8A5A36", socks="F4F1EA", earrings="F2C12E")
P("scientist", tone="medium", hair="short", hairc="2E2622", eyes="dark", top="shirt", topc="9EC3E6", collarc="FFFFFF", bottom="trousers", botc="4A4E5E", coat="lab", coatc="F6F7F5", coatlen=16, glasses=True, shoec="2E2E36", badge="58A6D8")
P("scientist_f", tone="fair", hair="ponytail", hairc="3A2A24", eyes="blue", fem=True, top="shirt", topc="F2C6D6", collarc="FFFFFF", bottom="trousers", botc="4A4E5E", coat="lab", coatc="F6F7F5", coatlen=16, glasses=True, shoec="3A3A44", tiec="E5525C")
# Trainers and townsfolk.
P("youngster", tone="light", hair="short", hairc="4A3020", top="tee", topc="F2B53A", bottom="shorts", botc="3A6FD0", shoec="D8343C", socks="F4F4F4", hat="cap", hatc="3A6FD0")
P("lass", tone="fair", hair="long", hairc="2E2622", fem=True, eyes="brown", top="blouse", topc="F5A9C4", collarc="FFFFFF", bottom="skirt", botc="3A4A7A", shoec="3A2A2A", socks="FFFFFF")
P("bug_catcher", tone="fair", hair="short", hairc="7A5030", top="tee", topc="F4EED0", bottom="shorts", botc="6E8A4A", hat="cap", hatc="6EA04A", shoec="6E5A44", socks="FFFFFF", bag="satchel", bagc="9A7A4A")
P("camper", tone="tan", hair="short", hairc="5A3A22", top="tee", topc="E58A3A", bottom="shorts", botc="A89464", hat="cap", hatc="3F7A44", bag="backpack", bagc="7A5030", shoec="6E4A2E", socks="E8E0CC")
P("picnicker", tone="fair", hair="pigtails", hairc="D87A3A", fem=True, freckles=True, top="tee", topc="8ACB8A", bottom="shorts", botc="4A6A9A", hat="sunhat", hatc="F6E3A0", bandc="E5525C", shoec="E5525C", tiec="FFFFFF")
P("hiker", tone="tan", hair="short", hairc="4A3020", beard=True, top="shirt", topc="B25A3A", collarc="B25A3A", bottom="trousers", botc="7A6A4A", bag="backpack", bagc="5A6A3A", shoec="5A3A22", hat="beanie", hatc="3A5A7A")
P("hiker_f", tone="medium", hair="ponytail", hairc="3A2620", fem=True, top="tee", topc="D86A4A", bottom="shorts", botc="7A6A4A", bag="backpack", bagc="4A6A8A", shoec="5A3A22", socks="C8C0A8", tiec="F2C12E")
P("super_nerd", tone="pale", hair="swept", hairc="2A2A2A", glasses=True, top="shirt", topc="F2F2EE", collarc="FFFFFF", bottom="trousers", botc="7A5A3A", tie="3A6FD0", shoec="3A2A2A")
P("pokemaniac", tone="light", hair="short", hairc="3A2A20", glasses=True, top="hoodie", topc="5DA05A", bottom="trousers", botc="6A5A4A", bag="satchel", bagc="3A6A9A", shoec="3A3A3A")
P("engineer", tone="medium", hair="short", hairc="3A2620", top="tee", topc="E8A23A", bottom="overalls", botc="3D5F8F", hat="hardhat", hatc="F2C12E", gloves="C89A4A", shoec="5A3A22")
P("swimmer", tone="tan", hair="buzz", hairc="2A2420", top="bare", bottom="shorts", botc="2A7AC8", shoec="C88E62")
P("swimmer_f", tone="light", hair="ponytail", hairc="2A2420", fem=True, top="tank", topc="2AA5C8", bottom="shorts", botc="2AA5C8", shoec="E2AE86", tiec="F2C12E")
P("fisher", tone="fair", hair="short", hairc="6A5A4A", beard=True, beardc="8A7A6A", top="shirt", topc="5A7A9A", collarc="5A7A9A", bottom="trousers", botc="3A4A3A", hat="cap", hatc="E8D8A8", shoec="3A3A2A", coat="vest", coatc="C8A84A", coatsleeve=1)
P("fisher_f", tone="fair", hair="ponytail", hairc="2A2420", fem=True, top="shirt", topc="F2C12E", collarc="F2C12E", bottom="trousers", botc="3D5F8F", hat="sunhat", hatc="F2E2A0", shoec="3A3A2A")
P("sailor", tone="tan", hair="short", hairc="2A2420", top="shirt", topc="F4F4F4", collarc="2E4A8A", bottom="trousers", botc="F4F4F4", hat="sailor", hatc="F4F4F4", stripe="2E4A8A", stripe_rows=(2, 3), shoec="2A2A2A")
P("gentleman", tone="pale", hair="swept", hairc="C8C8C8", moustache=True, beardc="D8D8D8", top="shirt", topc="F4F4F4", bottom="trousers", botc="2A2A34", coat="suit", coatc="3A3A48", coatlen=14, tie="8A2A3A", shoec="1A1A1A")
# The Trainer School's teacher (Let's Go's): dark hair, an orange jacket, a green dress.
P("school_teacher", tone="light", hair="long", hairc="2A1E1A", eyes="brown", fem=True, top="blouse", topc="F6F1E6", collarc="FFFFFF", bottom="dress", botc="4E9A5A", coat="blazer", coatc="E8823A", coatsleeve=12, shoec="5A3A22", earrings="F2C12E", lips="C85A5A")
P("lady", tone="fair", hair="bun", hairc="E8C870", fem=True, top="blouse", topc="F2D2E2", bottom="dress", botc="E8A2C2", shoec="F4F4F4", earrings="F2C12E", lips="D04070")
P("rich_boy", tone="pale", hair="swept", hairc="E8C870", top="shirt", topc="F4F4F4", bottom="shorts", botc="2E3A6A", coat="blazer", coatc="2E3A6A", coatsleeve=12, tie="C83A3A", socks="F4F4F4", shoec="3A2A1A")
P("beauty", tone="medium", hair="long", hairc="2A1A1A", fem=True, top="tank", topc="C83A6A", bottom="dress", botc="C83A6A", shoec="1A1A1A", earrings="F2C12E", lips="C03050")
P("biker", tone="light", hair="spiky", hairc="2A2A2A", shades=True, top="tank", topc="2A2A2A", bottom="jeans", botc="3A4A6A", coat="leather", coatc="3A2A22", coatsleeve=12, gloves="2A2A2A", shoec="1A1A1A")
P("cue_ball", tone="brown", hair="bald", hairc="2A2420", shades=True, top="tank", topc="F4F4F4", bottom="trousers", botc="2A2A34", belt="C83A3A", shoec="1A1A1A")
P("channeler", tone="pale", hair="long", hairc="5A5A6A", fem=True, smile=False, top="blouse", topc="E8E8F0", bottom="dress", botc="9A8AB0", shoec="3A3A4A", sash="6A4AA8")
P("gambler", tone="fair", hair="swept", hairc="3A2A20", moustache=True, top="shirt", topc="F4F4F4", bottom="trousers", botc="3A3A3A", coat="vest", coatc="6A2A3A", coatsleeve=1, tie="F2C12E", shoec="1A1A1A")
P("rocker", tone="light", hair="spiky", hairc="C83A3A", top="tank", topc="2A2A2A", bottom="jeans", botc="2A2A3A", coat="leather", coatc="2A2A2A", wristband="C8C8C8", shoec="1A1A1A")
P("juggler", tone="fair", hair="short", hairc="D87A3A", top="shirt", topc="E5525C", stripe="F2C12E", stripe_rows=(3, 7), bottom="trousers", botc="3A4A9A", shoec="1A1A1A", collarc="F2C12E")
P("tamer", tone="tan", hair="short", hairc="2A2420", moustache=True, top="shirt", topc="A84A2A", collarc="A84A2A", bottom="trousers", botc="B09A6A", hat="cap", hatc="7A5A3A", belt="5A3A22", shoec="3A2A1A")
P("bird_keeper", tone="fair", hair="short", hairc="7A5A3A", top="shirt", topc="E8D88A", collarc="E8D88A", bottom="trousers", botc="5A7A4A", hat="cap", hatc="5A8A4A", bag="satchel", bagc="8A6A3A", shoec="5A3A22")
P("black_belt", tone="brown", hair="short", hairc="1A1A1A", top="jumper", topc="F4F4F0", sleeves=12, bottom="trousers", botc="F4F4F0", belt="1A1A1A", shoec="B07448", scarf=None)
P("crush_girl", tone="medium", hair="ponytail", hairc="1A1A1A", fem=True, top="tank", topc="D84A4A", bottom="shorts", botc="2A2A3A", gloves="F4F4F4", shoec="2A2A2A", tiec="D84A4A")
P("psychic_m", tone="fair", hair="swept", hairc="2A2A3A", top="shirt", topc="5A4A8A", collarc="5A4A8A", bottom="trousers", botc="3A2A5A", sash="F2C12E", shoec="1A1A1A")
P("psychic_f", tone="pale", hair="long", hairc="6A4AA8", fem=True, top="blouse", topc="9A5AA8", bottom="dress", botc="5A3A7A", shoec="2A2A3A", earrings="C8C8F0")
P("burglar", tone="light", hair="short", hairc="2A2A2A", top="jumper", topc="2A2A34", stripe="F4F4F4", stripe_rows=(2, 5, 8), bottom="trousers", botc="2A2A34", hat="beanie", hatc="2A2A34", shoec="1A1A1A", shades=True)
P("cooltrainer_m", tone="medium", hair="spiky", hairc="3A6A4A", top="tee", topc="2A3A4A", bottom="trousers", botc="2A2A34", coat="jacket", coatc="2AA5A0", coatsleeve=12, shoec="F4F4F4", soles="2AA5A0", wristband="2AA5A0")
P("cooltrainer_f", tone="fair", hair="ponytail", hairc="D8743A", fem=True, top="tee", topc="2A3A4A", bottom="skirt", botc="2A2A34", coat="jacket", coatc="2AA5A0", coatsleeve=12, shoec="F4F4F4", tiec="2AA5A0")
P("twins", tone="fair", hair="pigtails", hairc="5A3A22", fem=True, blush=True, top="tee", topc="F2C12E", bottom="skirt", botc="D84A6A", shoec="D84A6A", socks="FFFFFF", tiec="D84A6A")
P("ruin_maniac", tone="tan", hair="short", hairc="5A4A3A", beard=True, top="shirt", topc="B8A06A", collarc="B8A06A", bottom="trousers", botc="8A7A5A", hat="sunhat", hatc="A8946A", bag="satchel", bagc="6A4A2A", shoec="4A3A2A")
P("painter", tone="light", hair="bun", hairc="A85A2A", fem=True, top="tee", topc="F4F4F4", bottom="trousers", botc="4A5A8A", apron="E8C88A", hat="beanie", hatc="C83A3A", shoec="3A3A3A")
P("aroma_lady", tone="medium", hair="long", hairc="6A3A1A", fem=True, top="blouse", topc="B8D89A", bottom="dress", botc="7AA86A", shoec="6A4A2A", earrings="F2A2C2")
P("breeder_m", tone="tan", hair="short", hairc="4A3020", top="shirt", topc="F4EED8", collarc="F4EED8", bottom="trousers", botc="6A8A5A", apron="8AB87A", shoec="5A3A22")
P("breeder_f", tone="fair", hair="ponytail", hairc="7A4A2A", fem=True, top="shirt", topc="F4EED8", collarc="F4EED8", bottom="skirt", botc="6A8A5A", apron="F2A8B8", shoec="5A3A22")
P("ranger_m", tone="medium", hair="short", hairc="3A2A20", top="shirt", topc="5A7A4A", collarc="5A7A4A", bottom="trousers", botc="A8946A", hat="cap", hatc="5A7A4A", bag="backpack", bagc="6A4A2A", shoec="4A3020", badge="F2C12E")
P("ranger_f", tone="fair", hair="ponytail", hairc="A84A2A", fem=True, top="shirt", topc="5A7A4A", collarc="5A7A4A", bottom="shorts", botc="A8946A", hat="cap", hatc="5A7A4A", shoec="4A3020", badge="F2C12E", socks="E8E0CC")
P("gameboy_kid", tone="light", hair="short", hairc="2A2420", top="hoodie", topc="5A8AD8", bottom="shorts", botc="3A3A4A", shoec="F4F4F4", soles="D84A4A")
P("girl", tone="fair", hair="bob", hairc="4A3020", fem=True, top="tee", topc="F28AB2", bottom="skirt", botc="3A6FD0", shoec="3A3A3A", socks="FFFFFF")
P("little_girl", tone="light", hair="pigtails", hairc="E8C870", fem=True, blush=True, top="dress", topc="F4A2C2", bottom="dress", botc="F4A2C2", shoec="D84A6A", socks="FFFFFF", tiec="FFFFFF")
P("little_boy", tone="brown", hair="short", hairc="1A1A1A", top="tee", topc="F2C12E", bottom="shorts", botc="3A6FD0", shoec="D84A4A", socks="FFFFFF")
P("boy", tone="dark", hair="buzz", hairc="1A1A1A", top="tee", topc="4AA86A", bottom="shorts", botc="3A4A6A", shoec="F4F4F4", bag="backpack", bagc="D84A4A")
P("toddler_girl", tone="fair", hair="pigtails", hairc="E8C870", fem=True, blush=True, top="dress", topc="F8C2D8", bottom="dress", botc="F8C2D8", shoec="F4F4F4", tiec="F2C12E")
P("teen_boy", tone="medium", hair="spiky", hairc="1A1A1A", top="hoodie", topc="3A6FD0", bottom="jeans", botc="3A4A6A", shoec="F4F4F4", soles="3A6FD0")
P("teen_girl", tone="deep", hair="afro", hairc="1A1210", fem=True, top="tee", topc="F2C12E", bottom="jeans", botc="4A5A8A", shoec="F4F4F4", earrings="F4F4F4")
P("student_m", tone="fair", hair="short", hairc="2A2420", top="shirt", topc="F4F4F4", bottom="trousers", botc="3A3A4A", coat="blazer", coatc="2E3A6A", tie="C83A3A", bag="satchel", bagc="3A2A1A", shoec="1A1A1A")
P("student_f", tone="medium", hair="long", hairc="3A2A20", fem=True, top="shirt", topc="F4F4F4", bottom="skirt", botc="3A3A6A", coat="blazer", coatc="2E3A6A", tie="C83A3A", socks="FFFFFF", shoec="1A1A1A")
P("brunette_girl", tone="fair", hair="long", hairc="6A4428", fem=True, top="jumper", topc="E8C8A8", bottom="jeans", botc="4A5A7A", shoec="8A5A36")
P("young_mum", tone="medium", hair="ponytail", hairc="4A3020", fem=True, top="tee", topc="A8C8E8", bottom="jeans", botc="4A5A7A", shoec="F4F4F4", bag="satchel", bagc="C88A5A")
P("young_dad", tone="fair", hair="short", hairc="6A4428", beard=True, top="shirt", topc="7AA87A", collarc="7AA87A", bottom="jeans", botc="3A4A6A", shoec="5A3A22")
P("middle_aged_man", tone="light", hair="short", hairc="5A4A3A", top="jumper", topc="8A6A4A", bottom="trousers", botc="4A4A4A", shoec="3A2A1A", glasses=True)
P("middle_aged_woman", tone="fair", hair="bob", hairc="8A5A3A", fem=True, top="blouse", topc="C86A8A", bottom="skirt", botc="5A5A7A", shoec="3A2A2A", earrings="F2C12E")
P("businessman", tone="fair", hair="swept", hairc="3A2A20", top="shirt", topc="F4F4F4", bottom="trousers", botc="3A3A44", coat="suit", coatc="3A3A44", coatlen=13, tie="3A6FD0", shoec="1A1A1A", bag="satchel", bagc="3A2A1A")
P("office_worker", tone="dark", hair="bun", hairc="1A1210", fem=True, top="blouse", topc="F4F4F4", bottom="skirt", botc="4A4A5A", coat="blazer", coatc="4A4A5A", shoec="1A1A1A", earrings="F2C12E")
P("balding_guy", tone="tan", hair="bald", hairc="6A5A4A", moustache=True, beardc="6A5A4A", top="shirt", topc="B8C8D8", collarc="B8C8D8", bottom="trousers", botc="5A4A3A", shoec="3A2A1A")
P("gramps", tone="fair", hair="bald", hairc="D8D8D8", beard=True, beardc="E8E8E8", glasses=True, top="jumper", topc="9A8A6A", bottom="trousers", botc="5A5A5A", shoec="3A2A1A")
P("granny", tone="light", hair="bun", hairc="E0E0E0", fem=True, glasses=True, top="jumper", topc="B87A9A", bottom="skirt", botc="6A5A7A", shoec="3A2A2A")
P("grandad", tone="deep", hair="short", hairc="E0E0E0", moustache=True, beardc="E8E8E8", top="jumper", topc="8A6A4A", bottom="trousers", botc="4A4A4A", hat="cap", hatc="5A5A5A", shoec="2A2A2A")
P("nan", tone="medium", hair="bun", hairc="E8E8E8", fem=True, glasses=True, top="blouse", topc="9A5A8A", bottom="dress", botc="7A4A6A", shoec="3A2A2A")
P("fisherman_old", tone="light", hair="short", hairc="E8E8E8", beard=True, beardc="F0F0F0", top="jumper", topc="F2C12E", bottom="trousers", botc="F2C12E", hat="sunhat", hatc="F2C12E", shoec="3A3A3A")
P("farmer", tone="tan", hair="short", hairc="6A4428", top="shirt", topc="C84A3A", collarc="C84A3A", bottom="overalls", botc="3D5F8F", hat="sunhat", hatc="E8D08A", shoec="5A3A22")
P("farmer_f", tone="medium", hair="pigtails", hairc="A85A2A", fem=True, freckles=True, top="shirt", topc="F4EED8", bottom="overalls", botc="3D5F8F", hat="sunhat", hatc="E8D08A", shoec="5A3A22", tiec="C84A3A")
P("gardener", tone="deep", hair="short", hairc="C8C8C8", top="shirt", topc="7AA86A", collarc="7AA86A", bottom="trousers", botc="6A5A3A", apron="5A7A3A", gloves="8AB87A", hat="sunhat", hatc="E8D08A", shoec="4A3020")
P("jogger", tone="dark", hair="buzz", hairc="1A1A1A", top="tank", topc="F2A23A", bottom="shorts", botc="2A2A34", shoec="F4F4F4", soles="F2A23A", wristband="F4F4F4")
P("jogger_f", tone="fair", hair="ponytail", hairc="E8C870", fem=True, top="tank", topc="5AD8B8", bottom="shorts", botc="2A2A34", shoec="F4F4F4", soles="E85A9A", tiec="E85A9A")
P("tourist", tone="pale", hair="short", hairc="D87A3A", freckles=True, top="shirt", topc="5AC8E8", collarc="5AC8E8", stripe="F2C12E", stripe_rows=(4,), bottom="shorts", botc="C8B88A", hat="sunhat", hatc="F4F0D8", bag="satchel", bagc="2A2A2A", shoec="8A6A4A", socks="F4F4F4")
P("backpacker", tone="tan", hair="long", hairc="E8C870", top="tee", topc="C86A3A", bottom="shorts", botc="7A6A4A", bag="backpack", bagc="3A6A3A", shoec="5A3A22")
P("skater", tone="fair", hair="swept", hairc="6A4428", top="hoodie", topc="2A2A34", bottom="jeans", botc="4A5A7A", hat="beanie", hatc="C83A3A", shoec="F4F4F4", soles="1A1A1A")
P("musician", tone="medium", hair="long", hairc="1A1A1A", shades=True, top="tee", topc="6A2A3A", bottom="jeans", botc="2A2A34", coat="leather", coatc="2A2A2A", shoec="1A1A1A")
P("dancer", tone="deep", hair="bun", hairc="1A1210", fem=True, top="tank", topc="E86AA8", bottom="dress", botc="E86AA8", shoec="F4D8E8", earrings="F2C12E")
P("delivery", tone="fair", hair="short", hairc="4A3020", top="shirt", topc="D8883A", collarc="D8883A", bottom="trousers", botc="6A4A2A", hat="cap", hatc="D8883A", bag="satchel", bagc="7A5030", shoec="3A2A1A")
P("postman", tone="medium", hair="short", hairc="1A1A1A", top="shirt", topc="C83A3A", collarc="C83A3A", bottom="trousers", botc="2E3A6A", hat="cap", hatc="C83A3A", bag="satchel", bagc="7A5030", shoec="1A1A1A")
P("builder", tone="tan", hair="short", hairc="4A3020", top="tee", topc="F28A2A", stripe="E8E8E8", stripe_rows=(6,), bottom="jeans", botc="3A4A6A", hat="hardhat", hatc="F2C12E", gloves="C89A4A", shoec="5A3A22")
P("doctor", tone="dark", hair="short", hairc="1A1A1A", glasses=True, top="shirt", topc="7AB8E8", collarc="FFFFFF", bottom="trousers", botc="3A3A4A", coat="lab", coatc="F6F7F5", coatlen=15, shoec="1A1A1A", badge="58A6D8")
P("doctor_f", tone="fair", hair="bun", hairc="A85A2A", fem=True, top="shirt", topc="7AB8E8", collarc="FFFFFF", bottom="trousers", botc="3A3A4A", coat="lab", coatc="F6F7F5", coatlen=15, shoec="1A1A1A", badge="58A6D8")
P("librarian", tone="light", hair="bun", hairc="A8A8A8", fem=True, glasses=True, top="jumper", topc="8A6AA8", bottom="skirt", botc="4A3A5A", shoec="3A2A2A", scarf="E8C88A")
P("teacher", tone="medium", hair="short", hairc="3A2A20", glasses=True, top="shirt", topc="E8E0C8", collarc="E8E0C8", bottom="trousers", botc="6A5A4A", coat="cardigan", coatc="7A8A5A", tie="7A3A3A", shoec="3A2A1A")
P("baker", tone="fair", hair="short", hairc="E8C870", top="shirt", topc="F4F4F0", collarc="F4F4F0", bottom="trousers", botc="8A7A6A", apron="F8F6F0", hat="chef", hatc="FAFAFA", shoec="3A3A3A")
P("shopkeeper", tone="deep", hair="short", hairc="1A1A1A", top="shirt", topc="5AA88A", collarc="5AA88A", bottom="trousers", botc="5A4A3A", apron="F4EED8", shoec="3A2A1A")
P("shopkeeper_f", tone="fair", hair="bun", hairc="6A4428", fem=True, top="blouse", topc="E8A2B8", bottom="skirt", botc="5A4A3A", apron="F4EED8", shoec="3A2A1A")
P("lifeguard", tone="medium", hair="short", hairc="E8C870", shades=True, top="tank", topc="D83A3A", bottom="shorts", botc="D83A3A", shoec="B07448")
P("photographer", tone="fair", hair="swept", hairc="2A2A2A", top="shirt", topc="8A7A5A", collarc="8A7A5A", bottom="trousers", botc="3A3A3A", hat="beanie", hatc="3A3A3A", bag="satchel", bagc="1A1A1A", shoec="1A1A1A", coat="vest", coatc="5A6A4A", coatsleeve=1)
P("reporter", tone="dark", hair="long", hairc="1A1210", fem=True, top="blouse", topc="F4F4F4", bottom="skirt", botc="3A3A44", coat="blazer", coatc="C83A3A", shoec="1A1A1A", earrings="F2C12E")
P("waitress", tone="fair", hair="ponytail", hairc="A85A2A", fem=True, top="shirt", topc="1A1A1A", collarc="FFFFFF", bottom="skirt", botc="1A1A1A", apron="FFFFFF", shoec="1A1A1A", tiec="FFFFFF")
P("waiter", tone="medium", hair="swept", hairc="1A1A1A", top="shirt", topc="F4F4F4", bottom="trousers", botc="1A1A1A", coat="vest", coatc="1A1A1A", coatsleeve=1, tie="1A1A1A", shoec="1A1A1A")
P("cook", tone="tan", hair="short", hairc="2A2420", moustache=True, top="shirt", topc="F6F6F2", collarc="F6F6F2", bottom="trousers", botc="3A3A3A", hat="chef", hatc="FAFAFA", apron="F8F6F0", shoec="1A1A1A")
P("cook_f", tone="medium", hair="bun", hairc="1A1A1A", fem=True, top="shirt", topc="F6F6F2", collarc="F6F6F2", bottom="trousers", botc="3A3A3A", hat="chef", hatc="FAFAFA", apron="F8F6F0", shoec="1A1A1A")
P("sailor_f", tone="fair", hair="ponytail", hairc="1A1A1A", fem=True, top="shirt", topc="F4F4F4", collarc="2E4A8A", bottom="skirt", botc="2E4A8A", hat="sailor", hatc="F4F4F4", shoec="1A1A1A", tiec="2E4A8A")
P("clerk", tone="light", hair="short", hairc="6A4428", top="shirt", topc="3A7AD0", collarc="F4F4F4", bottom="trousers", botc="2E3A5A", apron="F4F4F4", shoec="1A1A1A", badge="F2C12E")
P("clerk_f", tone="medium", hair="ponytail", hairc="1A1A1A", fem=True, top="shirt", topc="3A7AD0", collarc="F4F4F4", bottom="skirt", botc="2E3A5A", apron="F4F4F4", shoec="1A1A1A", badge="F2C12E", tiec="3A7AD0")
P("nurse_m", tone="fair", hair="short", hairc="6A4428", top="shirt", topc="F8C8D8", collarc="FFFFFF", bottom="trousers", botc="FFFFFF", shoec="FFFFFF", hat="nurse", hatc="FFFFFF", badge="E5525C")
P("officer_m", tone="medium", hair="short", hairc="1A1A1A", top="shirt", topc="2E4A8A", collarc="2E4A8A", bottom="trousers", botc="2E3A5A", hat="police", hatc="2E3A5A", badgec="F2C12E", badge="F2C12E", belt="1A1A1A", shoec="1A1A1A")
P("link_receptionist", tone="fair", hair="bob", hairc="3A2A20", fem=True, top="blouse", topc="58B8A8", collarc="FFFFFF", bottom="skirt", botc="2E5A5A", shoec="1A1A1A", scarf="F2C12E")
P("game_corner_clerk", tone="light", hair="swept", hairc="2A2A2A", top="shirt", topc="F4F4F4", bottom="trousers", botc="1A1A1A", coat="vest", coatc="B82A3A", coatsleeve=1, tie="F2C12E", shoec="1A1A1A")
P("silph_worker_m", tone="fair", hair="short", hairc="3A2A20", top="shirt", topc="F4F4F4", bottom="trousers", botc="4A5A7A", coat="suit", coatc="5A6A8A", tie="3A6FD0", shoec="1A1A1A", badge="F2C12E")
P("silph_worker_f", tone="medium", hair="bun", hairc="3A2A20", fem=True, top="blouse", topc="F4F4F4", bottom="skirt", botc="4A5A7A", coat="blazer", coatc="5A6A8A", shoec="1A1A1A", badge="F2C12E")
P("guard", tone="tan", hair="short", hairc="1A1A1A", top="shirt", topc="4A5A3A", collarc="4A5A3A", bottom="trousers", botc="3A4A2A", hat="police", hatc="3A4A2A", belt="1A1A1A", shoec="1A1A1A", badge="C8C8C8")
P("safari_zone_worker", tone="tan", hair="short", hairc="5A3A22", top="shirt", topc="C8B87A", collarc="C8B87A", bottom="shorts", botc="8A7A4A", hat="sunhat", hatc="C8B87A", shoec="5A3A22", socks="E8E0CC", badge="5A8A4A")
P("bike_shop_clerk", tone="fair", hair="short", hairc="5A3A22", top="tee", topc="3A7AD0", bottom="trousers", botc="3A7AD0", hat="cap", hatc="F4F4F4", gloves="2A2A2A", shoec="1A1A1A")
P("gym_guide", tone="medium", hair="spiky", hairc="2A2420", top="tee", topc="F4F4F4", bottom="trousers", botc="3A3A44", coat="jacket", coatc="E8A23A", shoec="F4F4F4", wristband="E8A23A")
# The story's named roles, in our own looks (not the games' or the show's designs). The files keep the role names
# the mod already uses, so what the Person Viewer shows is what's in game.
EMBLEM = [(3, 2), (4, 2), (2, 3), (5, 3), (2, 4), (5, 4), (3, 5), (4, 5), (4, 3), (3, 4)]   # the villains' own mark: a ring cut through
P("oak", tone="fair", hair="short", hairc="D8D8D4", beard=True, beardc="F0F0F0", eyes="brown", top="jumper", topc="2A8A8A", bottom="trousers", botc="6A5038", coat="lab", coatc="F6F7F5", coatlen=17, shoec="3A2A1A", badge="F2C12E")
P("blue", tone="light", hair="swept", hairc="2A3A6A", eyes="grey", brows="stern", top="tee", topc="1A1A24", bottom="jeans", botc="3A3A48", coat="jacket", coatc="E07A2A", coatsleeve=12, shoec="F4F4F4", soles="E07A2A", wristband="E07A2A")
P("red", tone="fair", hair="short", hairc="2A2420", eyes="dark", smile=False, top="hoodie", topc="34343E", stripe="B83A3A", stripe_rows=(5,), bottom="trousers", botc="1E1E26", hat="cap", hatc="2E3A6A", shoec="F4F4F4", soles="B83A3A")
P("nurse", tone="fair", hair="bun", hairc="8A4A2A", eyes="teal", fem=True, top="blouse", topc="9ED8C8", collarc="FFFFFF", bottom="skirt", botc="7AC0B0", hat="nurse", hatc="FFFFFF", crossc="2AA58A", shoec="F8F8F8", apron="FFFFFF", socks="FFFFFF")
P("officer_jenny", tone="brown", hair="bob", hairc="1A1A1A", eyes="dark", fem=True, top="shirt", topc="3A5AA8", collarc="3A5AA8", bottom="trousers", botc="243A6A", hat="police", hatc="243A6A", badgec="F2C12E", badge="F2C12E", belt="1A1A1A", gloves="F4F4F4", shoec="1A1A1A")
# JESSIE and JAMES stand out from the grunts: red jackets done up, the mark in black (the grunts wear black with the mark in red).
P("jessie", tone="pale", hair="bob", hairc="D8D8E0", eyes="violet", fem=True, top="tee", topc="2A2A34", bottom="trousers", botc="2A2A34", coat="jacket", closed=True, coatc="C8323C", coatsleeve=12, bigmark="1E1E24", gloves="1E1E24", shoec="1A1A1A", lips="8A2A5A", earrings="1E1E24")
P("james", tone="light", hair="spiky", hairc="1A1A1A", eyes="green", top="tee", topc="2A2A34", bottom="trousers", botc="2A2A34", coat="jacket", closed=True, coatc="C8323C", coatsleeve=12, bigmark="1E1E24", gloves="1E1E24", shoec="1A1A1A")
P("giovanni", tone="tan", hair="swept", hairc="4A4A4A", eyes="grey", brows="stern", smile=False, top="shirt", topc="1A1A1A", collarc="1A1A1A", bottom="trousers", botc="2A2A2A", coat="suit", coatc="5A1A2A", coatlen=16, tie="C8A23A", shoec="1A1A1A")
P("rocket", tone="medium", hair="short", hairc="1A1A1A", top="tee", topc="2A2A34", bottom="trousers", botc="2A2A34", hat="beanie", hatc="2A2A34", gloves="D83A4A", letter="D83A4A", mark=EMBLEM, shoec="1A1A1A")
P("rocket_f", tone="fair", hair="ponytail", hairc="C83A3A", fem=True, top="tee", topc="2A2A34", bottom="skirt", botc="2A2A34", gloves="D83A4A", letter="D83A4A", mark=EMBLEM, shoec="1A1A1A", socks="2A2A34", tiec="D83A4A")
P("rocket_scientist", tone="pale", hair="swept", hairc="6A6A6A", glasses=True, top="tee", topc="2A2A34", bottom="trousers", botc="2A2A34", coat="lab", coatc="E8E8EC", coatlen=15, letter="D83A4A", mark=EMBLEM, shoec="1A1A1A")
# Gym leaders: one each, dressed for their type and their town.
P("leader_brock", tone="dark", hair="buzz", hairc="1A1A1A", eyes="dark", top="tee", topc="8A8A82", bottom="combat", botc="6A6258", hat="hardhat", hatc="F2C12E", gloves="B08A5A", shoec="4A3A2A", coat="vest", coatc="B8E04A", coatsleeve=1, stripe=None)
P("leader_misty", tone="light", hair="bob", hairc="2A6AA8", eyes="teal", fem=True, top="tank", topc="2AA5C8", bottom="shorts", botc="1A4A7A", coat="jacket", coatc="F4F4F4", coatsleeve=4, shoec="F4F4F4", soles="2AA5C8", wristband="2AA5C8")
P("leader_lt_surge", tone="brown", hair="spiky", hairc="F2D23A", eyes="amber", top="tee", topc="2A2A34", stripe="F2D23A", stripe_rows=(5,), bottom="trousers", botc="2A2A34", coat="jacket", coatc="F2D23A", gloves="2A2A34", shoec="F2D23A", soles="2A2A34")
P("leader_erika", tone="tan", hair="long", hairc="2A1A10", eyes="green", fem=True, top="blouse", topc="F4EED8", bottom="dress", botc="4A8A4A", apron="7AB86A", hat="sunhat", hatc="E8D08A", bandc="4A8A4A", shoec="6A4A2A", earrings="F28AB2")
P("leader_koga", tone="pale", hair="ponytail", hairc="6A2A8A", eyes="violet", top="jumper", topc="3A2A4A", bottom="trousers", botc="2A1A34", scarf="8A3AA8", gloves="8A3AA8", shoec="1A1A1A", tiec="8A3AA8", brows="stern", smile=False)
P("leader_sabrina", tone="medium", hair="long", hairc="1A1A1A", eyes="violet", fem=True, smile=False, top="blouse", topc="E8E0F4", bottom="dress", botc="5A3A8A", coat="cardigan", coatc="9A7AC8", shoec="1A1A1A", earrings="C8C8F0", pendant="C8A8F8")
P("leader_blaine", tone="fair", hair="spiky", hairc="E04A2A", eyes="amber", top="tank", topc="F4F4F4", bottom="trousers", botc="3A2A2A", coat="leather", coatc="A82A1A", gloves="1A1A1A", shoec="1A1A1A", soles="E04A2A")
PEOPLE["leader_giovanni"] = PEOPLE["giovanni"]
# The Elite Four.
P("lorelei", tone="pale", hair="long", hairc="C8E8F8", eyes="blue", fem=True, top="jumper", topc="E8F4FA", bottom="trousers", botc="5A8AB8", coat="jacket", coatc="8AC8E8", scarf="F4F4F4", shoec="E8F4FA")
P("bruno", tone="deep", hair="buzz", hairc="1A1A1A", eyes="dark", brows="stern", top="tank", topc="C83A3A", bottom="trousers", botc="2A2A2A", belt="1A1A1A", gloves="C83A3A", shoec="2A2A2A", wristband="F4F4F4")
P("agatha", tone="fair", hair="bob", hairc="B8B8C8", eyes="green", fem=True, smile=False, top="jumper", topc="1E4A44", bottom="dress", botc="16302C", coat="cardigan", coatc="2E6A5A", shoec="1A1A1A", pendant="8AE8C8", glasses=True)
P("lance", tone="medium", hair="swept", hairc="3A3A8A", eyes="amber", top="tee", topc="1A1A24", bottom="trousers", botc="1A1A24", coat="jacket", coatc="1A6A5A", coatlen=16, gloves="1A1A24", shoec="1A1A1A", badge="E8A23A")
# The rest of the story's people.
P("bill", tone="fair", hair="swept", hairc="C86A2A", freckles=True, glasses=True, top="shirt", topc="F4F0E0", bottom="trousers", botc="5A5A6A", coat="cardigan", coatc="5A8AC8", shoec="5A3A22")
P("mr_fuji", tone="light", hair="bald", hairc="E8E8E8", beard=True, beardc="F0F0F0", top="jumper", topc="8A6A9A", bottom="trousers", botc="4A3A4A", shoec="2A2A2A", scarf="C8B8D8")
P("copycat", tone="fair", hair="pigtails", hairc="2A2420", fem=True, blush=True, top="tee", topc="F2A2C2", bottom="skirt", botc="5A6AC8", shoec="D84A6A", socks="FFFFFF", tiec="F2C12E")
P("mr_psychic", tone="medium", hair="swept", hairc="1A1A1A", smile=False, top="shirt", topc="6A4AA8", collarc="6A4AA8", bottom="trousers", botc="2A1A3A", sash="C8A8F8", shoec="1A1A1A", pendant="F2C12E")
P("fan_club_chairman", tone="light", hair="bald", hairc="8A7A6A", moustache=True, beardc="8A7A6A", top="shirt", topc="F4F4F4", bottom="trousers", botc="3A3A44", coat="suit", coatc="C86A2A", tie="F2C12E", shoec="3A2A1A")
P("name_rater", tone="fair", hair="short", hairc="C8C8C8", glasses=True, top="shirt", topc="F4F4F4", bottom="trousers", botc="4A4A5A", coat="vest", coatc="6A5A8A", coatsleeve=1, tie="C83A3A", shoec="1A1A1A")
P("day_care_man", tone="tan", hair="short", hairc="D8D8D8", moustache=True, beardc="E0E0E0", top="shirt", topc="F2E2A0", collarc="F2E2A0", bottom="trousers", botc="6A5A3A", apron="7AB86A", shoec="4A3020")
P("old_man", tone="light", hair="bald", hairc="D8D8D8", beard=True, beardc="E8E8E8", top="jumper", topc="7A6A5A", bottom="trousers", botc="4A4A4A", shoec="3A2A1A", hat="cap", hatc="5A5A5A")
P("fishing_guru", tone="medium", hair="short", hairc="E8E8E8", beard=True, beardc="F0F0F0", top="jumper", topc="2E5A8A", bottom="trousers", botc="3A3A2A", hat="beanie", hatc="C83A3A", shoec="3A3A3A")
P("silph_president", tone="pale", hair="swept", hairc="A8A8A8", glasses=True, top="shirt", topc="F4F4F4", bottom="trousers", botc="2A2A34", coat="suit", coatc="2A2A34", coatlen=14, tie="F2C12E", shoec="1A1A1A", badge="F2C12E")
P("captain", tone="light", hair="short", hairc="E8E8E8", beard=True, beardc="F0F0F0", top="shirt", topc="F4F4F4", bottom="trousers", botc="F4F4F4", coat="suit", coatc="2E3A6A", hat="sailor", hatc="2E3A6A", shoec="1A1A1A", badge="F2C12E")
P("warden", tone="tan", hair="bald", hairc="D8D8D8", moustache=True, beardc="E0E0E0", smile=False, top="shirt", topc="C8B87A", collarc="C8B87A", bottom="trousers", botc="6A5A3A", hat="sunhat", hatc="A8946A", shoec="4A3020")
P("museum_guide", tone="brown", hair="short", hairc="1A1A1A", glasses=True, top="shirt", topc="F4F4F4", bottom="trousers", botc="3A3A44", coat="blazer", coatc="7A2A2A", tie="F2C12E", shoec="1A1A1A", badge="F2C12E")
P("tuber_m", tone="light", hair="short", hairc="2A2420", top="bare", bottom="shorts", botc="F2A23A", shoec="E2AE86")
P("tuber_f", tone="medium", hair="pigtails", hairc="1A1A1A", fem=True, top="tank", topc="F28AB2", bottom="shorts", botc="F28AB2", shoec="B07448", tiec="F2C12E")


def main():
    out = sys.argv[1]
    only = None
    if "--only" in sys.argv: only = set(sys.argv[sys.argv.index("--only") + 1].split(","))
    os.makedirs(out, exist_ok=True)
    for i, (k, spec) in enumerate(PEOPLE.items()):
        if only and k not in only: continue
        person(spec, hash(k) % 100000).save(os.path.join(out, k + ".png"))
    print(len(PEOPLE), "people painted")


if __name__ == "__main__":
    main()
