"""
Retromon's own creatures, part four: those that come after mons3.py, built the same way (cubes painted in 3D, our own
designs, the same species underneath).

Run: python3 tools/mons4.py [names]   (all if none; refreshes mons/names.json either way)
"""

import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from monlib import CLEAR, Cube, Mat, Model, OUT_MODELS, blob, cell, face_blob, hash01, mix, rgb, track, wave, keys, write, away, FX  # noqa: E402
from monanims import biped  # noqa: E402
from mons import leaf_mat, fx_for, open_mouth, shag_mat  # noqa: E402
from mons2 import fur, n  # noqa: E402
from mons3 import moves, bone_of, upright_legs, upright_arms  # noqa: E402


# ======================================================================================================
# The giant sloth: TORPOTH (in place of Snorlax)
# ======================================================================================================

def torpoth():
    """A giant ground sloth that sleeps where it likes, even across a road. Huge and pear-shaped, a shaggy coat of warm
    olive-brown gone green with moss along its back, a cream belly, a small pale face with a sloth's dark mask round
    heavy sleepy eyes, a round muzzle, tiny ears, a sapling sprouting from its crown, long arms with three great hooked
    claws, short thick legs and a stubby tail. Asleep (out in the world), it lies on its back, hands folded on its
    belly, breathing slow."""
    m = Model()
    FUR, FUR_L, FUR_D = "#7E6A48", "#A89270", "#4E402A"
    BELLY, BELLY_L, BELLY_D = "#E4D4B0", "#F4EAD2", "#C2AE86"
    MOSS, MOSS_L, MOSS_D = "#6E9A3C", "#9CC45A", "#486E28"
    FACE, MASK = "#D8C49A", "#4A3826"
    base = fur(FUR, FUR_L, FUR_D, speck=0.55)
    hip = 5

    def coat(at):
        # Moss in clumps along its back and over its shoulders (the top and back faces, and the sides' upper part).
        if at.face in ("top", "back") or (at.face in ("px", "nx") and at.y > hip + 12):
            k = cell(at.x, at.y, at.z, 2.2)
            if k > 0.42:
                c = mix(MOSS_D, MOSS_L, hash01("moss", round(at.x * 2), round(at.y * 2), round(at.z * 2)))
                return mix(c, MOSS, 0.4)
        # Its belly: a broad cream oval on the front.
        if at.face == "front" and (at.x / 6.0) ** 2 + ((at.y - (hip + 9)) / 8.5) ** 2 < 1:
            return mix(BELLY_D, BELLY_L, 0.35 + 0.5 * at.ly) if hash01("bl", round(at.x * 2), round(at.y * 2)) > 0.2 else rgb(BELLY)
        return base(at)
    furm = Mat(FUR, coat, noise=0.03)
    plain = Mat(FUR, base, noise=0.03)

    def face_paint(at):
        ey = hip + 21.3
        if at.face == "front":
            # The sloth's mask: a dark band through each eye, widening out to the sides, the pale face round it.
            if abs(at.x) > 0.6 and abs(at.y - ey + 0.15 * abs(at.x)) < 0.9 + 0.22 * abs(at.x):
                return rgb(MASK)
            if at.y > ey + 2.4:
                return mix(FACE, FUR, 0.35)
            return mix(FACE, "#F0E2C0", 0.3 * at.ly)
        if at.face in ("px", "nx"):
            # The band runs back along the sides of the head towards the ears.
            if at.z < 1 and abs(at.y - (ey - 0.4 + (at.z + 7.5) * 0.18)) < 1.0:
                return rgb(MASK)
            return mix(FACE, FUR, min(1.0, 0.3 + (at.z + 7.5) * 0.12))
        return None
    face = Mat(FACE, face_paint, noise=0.02)
    muzzle = Mat("#EDE0C2", lambda a: mix("#EDE0C2", "#C8B48C", 1 - a.ly) if a.face != "top" else rgb("#F4EAD2"), noise=0.02)
    nose = Mat("#2A1E16", lambda a: rgb("#5A4636") if a.face == "top" else None, noise=0.01, edge=0)
    jaw_m = Mat("#EDE0C2", open_mouth("#7A3A3A", "#D8C8B0"), noise=0.02)
    palm = Mat("#5A4A36", noise=0.02)
    pad = Mat("#3E3226", noise=0.02)
    toe = Mat("#EDE3CC", lambda a: rgb("#B8AA90") if a.face == "bottom" else None, noise=0.01)
    shag = shag_mat(FUR, FUR_L, FUR_D)
    stem = Mat("#6A5A30", noise=0.01, edge=0)
    sapling = leaf_mat("#8CCC58", "#3E7A2C", "#D0EEA0", "z")

    m.bone("root", None, [0, 0, 0])
    # The body: a wide round base and a narrower chest on it, the head sunk low between the shoulders.
    m.bone("body", "root", [0, hip + 7, 0], blob([-9, hip - 1, -7], [18, 12, 14], furm, b=3) + blob([-7.5, hip + 10, -5.5], [15, 9, 11], furm, b=2))
    bd = bone_of(m, "body")
    # Shaggy fringes hanging over its hips.
    for a in (-75, -40, 40, 75):
        r = math.radians(a)
        cx, cz = math.sin(r) * 8.6, math.cos(r) * 6.2
        bd.cubes.append(Cube([cx - 2.5, hip - 1.5, cz], [5, 5.5, 0], shag, [8, a, 0], [cx, hip + 4, cz]))
    hy = hip + 17
    m.bone("head", "body", [0, hy, -1.5], face_blob([-5, hy, -7.5], [10, 7.5, 9], face, b=2) +
           [Cube([-2.6, hy + 0.4, -9.0], [5.2, 3.0, 1.6], muzzle), Cube([-1.3, hy + 2.6, -9.4], [2.6, 1.2, 0.8], nose)])
    m.bone("jaw", "head", [0, hy + 0.6, -7.6], [Cube([-1.8, hy - 0.1, -8.8], [3.6, 0.7, 1.4], jaw_m)])
    m.eyes("head", x_inner=1.1, y=hy + 4.2, z=-7.5, w=2, h=2, iris="#2A1A10", pupil="#0A0604", skin=MASK, iris_light="#5A4030",
           grid=["DDDD", "DDDD", "LLLL", "PPWP"])
    m.pair("ear_right", "head", [-4.6, hy + 6, -2.5], [Cube([-5.6, hy + 5.0, -3.1], [1.6, 1.8, 1.4], plain)])
    # The sapling on its crown: a little stem and two leaves.
    m.bone("sprout", "head", [0, hy + 7.5, -3], [Cube([0, hy + 7.5, -3.4], [0, 2.6, 0.8], stem),
                                                  Cube([-3.0, hy + 9.6, -3.6], [3.0, 0, 1.8], sapling, [0, 0, -24], [0, hy + 9.8, -3]),
                                                  Cube([0, hy + 9.6, -3.6], [3.0, 0, 1.8], sapling, [0, 0, 24], [0, hy + 9.8, -3])])
    # Long, heavy arms that reach almost to the ground, three great hooked claws on each hand, long hair down them.
    upright_arms(m, 9.6, hip + 15.5, -1, 7, 7, 4.4, plain, palm, hw=5, hh=2, claws=3, rest=(-6, 0, 14), fore_rest=(-18, 0, 0), thick=5)
    for side, sx in (("forearm_right", -1), ("forearm_left", 1)):
        fb = bone_of(m, side)
        fb.cubes.append(Cube([sx * 11.9, hip + 1.6, -2.4], [0, 6.4, 3.4], shag))
    upright_legs(m, 5.2, hip, 0.5, 5, plain, pad, 6, toe_m=toe, ntoes=3, thigh_w=6)
    m.bone("tail", "body", [0, hip + 2, 6.4], blob([-2.5, hip - 0.5, 5.6], [5, 5, 5], plain), rotation=[30, 0, 0])
    fx_for(m, 1.6)
    moves(m, "BODY SLAM", "REST")
    ex = {"idle": {"sprout": track(rotation=[0, 0, wave(3.6, 8)])}, "battle": {"sprout": track(rotation=[0, 0, wave(1.8, 12)])}}
    A, info = biped("torpoth", m, walk=1.2, stride=16, k=1.5, slow=1.7, guard=22, extras=ex)
    # Asleep out in the world: lying (the world lays it on its back), hands folded on its belly, its legs at ease, its
    # head lolled to one side, its eyes shut, breathing slow and deep, a soft snore, the sapling nodding.
    P = 4.8
    sleep = {
        "body": track(scale=[wave(P, 0.012, 0, 1), 1, wave(P, 0.045, 0, 1)]),
        "head": track(rotation=[-6, 0, wave(P * 2, 3, 0, 14)]),
        "jaw": track(rotation=[wave(P, 4, 0, 5), 0, 0]),
        # (The arms hang along its sides, then each forearm comes in across the belly, the hands resting on it.)
        "arm_right": track(rotation=[-14, 0, -14]), "arm_left": track(rotation=[-14, 0, 14]),
        "forearm_right": track(rotation=[-2, 0, -48]), "forearm_left": track(rotation=[-2, 0, 48]),
        "leg_right": track(rotation=[-10, 0, 10]), "leg_left": track(rotation=[-10, 0, -10]),
        "foot_right": track(rotation=[24, 0, 0]), "foot_left": track(rotation=[24, 0, 0]),
        "eyelid_right": track(scale=[1, 1, 1]), "eyelid_left": track(scale=[1, 1, 1]),
        "sprout": track(rotation=[0, 0, wave(P, 6)]),
        **away(*FX)}
    sid = A.add("sleep", P, {k: v for k, v in sleep.items() if k in m.names})
    info["poses"]["sleep"] = {"anim": sid, "parts": []}
    write("snorlax", "TORPOTH", m, A, info, 3.4)


ALL4 = (torpoth,)


def refresh_names():
    """mons/names.json: every creature folder's name (for the names on screen and the POKeDEX)."""
    names = {}
    for sid in sorted(os.listdir(OUT_MODELS)):
        p = os.path.join(OUT_MODELS, sid, "info.json")
        if os.path.exists(p):
            names[sid] = json.load(open(p))["name"]
    json.dump(names, open(os.path.join(OUT_MODELS, "names.json"), "w"), indent=1)
    print(len(names), "creatures named")


def main(names):
    for f in ALL4:
        if not names or f.__name__ in names:
            f()
    refresh_names()


if __name__ == "__main__":
    main(set(sys.argv[1:]))
