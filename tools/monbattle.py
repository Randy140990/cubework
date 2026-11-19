"""
Retromon's own creatures' battle animations, for every body (four legs, birds, hoppers): attacking, a signature move, a
critical hit, recall and send out (actions, played over whatever the creature is doing), and confused and fainted (poses).
Each creature adds its own move's effect (leaves, embers, water, wind, fangs) through its extras.
"""

from monlib import wave, keys, track, merge, only, away, HIDE


def _extra(extras, name):
    e = extras or {}
    return merge(e.get("all"), e.get(name))


def half_width(m):
    """How far the body reaches out to each side (so that, fainted and lying on its side, it rests on the ground)."""
    body = next((b for b in m.bones if b.name == "body"), None)
    xs = [abs(v) for c in (body.cubes if body else []) if not c.effect for v in (c.origin[0], c.origin[0] + c.size[0])]
    return max(xs) if xs else 4.0


def keys_lr(name, pts):
    """A part's rotation keys for its right side, mirrored for its left."""
    return {name + "_right": track(rotation=keys(pts)),
            name + "_left": track(rotation=keys({t: [v[0], -v[1], -v[2]] for t, v in pts.items()}))}


def attack(T, k, a, kind):
    """Charging in to strike: a wind-up, a lunge with the head driving in and the jaws (or beak) snapping, and back."""
    p = lambda f: T * f
    snap = keys({0: [0, 0, 0], p(.38): [0, 0, 0], p(.44): [32, 0, 0], p(.52): [0, 0, 0], T: [0, 0, 0]})
    t = {"body": track(position=keys({0: [0, 0, 0], p(.3): [0, -0.8 * k * a, 2.2 * k * a], p(.45): [0, 0.6 * k * a, -6 * k * a],
                                      p(.6): [0, 0, -4 * k * a], T: [0, 0, 0]}),
                       rotation=keys({0: [0, 0, 0], p(.3): [7 * a, 0, 0], p(.45): [-5 * a, 0, 0], p(.6): [3 * a, 0, 0], T: [0, 0, 0]})),
         "neck": track(rotation=keys({0: [0, 0, 0], p(.3): [8 * a, 0, 0], p(.45): [-10 * a, 0, 0], T: [0, 0, 0]})),
         "head": track(rotation=keys({0: [0, 0, 0], p(.3): [12 * a, 0, 0], p(.42): [-6 * a, 0, 0], p(.5): [16 * a, 0, 0], p(.7): [0, 0, 0],
                                      T: [0, 0, 0]})),
         "jaw": track(rotation=snap), "beak_lower": track(rotation=dict(snap)),
         "tail": track(rotation=keys({0: [0, 0, 0], p(.3): [-15, 0, 0], p(.45): [20, 0, 0], T: [0, 0, 0]}))}
    t.update(keys_lr("ear", {0: [0, 0, 0], p(.3): [25, 0, -10], p(.5): [35, 0, -15], T: [0, 0, 0]}))
    if kind == "legs4":
        for leg in ("leg_front_right", "leg_front_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], p(.3): [10, 0, 0], p(.45): [-28 * a, 0, 0], p(.6): [-8, 0, 0], T: [0, 0, 0]}))
        for leg in ("leg_back_right", "leg_back_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], p(.3): [-8, 0, 0], p(.45): [22 * a, 0, 0], T: [0, 0, 0]}))
    elif kind == "bird":
        t.update(keys_lr("wing", {0: [0, 0, 0], p(.3): [0, 0, 25], p(.45): [0, 0, 60], p(.7): [0, 0, 10], T: [0, 0, 0]}))
        t.update(keys_lr("leg", {0: [0, 0, 0], p(.3): [-10, 0, 0], p(.45): [25, 0, 0], T: [0, 0, 0]}))
    else:
        t.update(keys_lr("arm", {0: [0, 0, 0], p(.3): [20, 0, 0], p(.45): [-60, 0, 15], p(.6): [-20, 0, 0], T: [0, 0, 0]}))
        t.update(keys_lr("leg", {0: [0, 0, 0], p(.3): [-12, 0, 0], p(.45): [25, 0, 0], T: [0, 0, 0]}))
    return t


def special(S, k, kind):
    """Using its signature move: gathering itself (a crouch and a shiver), then letting it go, head up and mouth open,
    holding as the move flies, and settling. The move's own effect comes from the creature."""
    q = lambda f: S * f
    open_ = keys({0: [0, 0, 0], q(.4): [0, 0, 0], q(.46): [40, 0, 0], q(.85): [40, 0, 0], q(.92): [0, 0, 0], S: [0, 0, 0]})
    t = {"body": track(position=keys({0: [0, 0, 0], q(.3): [0, -k, 1.5 * k], q(.32): [0.3 * k, -k, 1.5 * k], q(.34): [-0.3 * k, -k, 1.5 * k],
                                      q(.36): [0.3 * k, -k, 1.5 * k], q(.38): [0, -k, 1.5 * k], q(.45): [0, 0.4 * k, -1.5 * k],
                                      q(.85): [0, 0.2 * k, -k], S: [0, 0, 0]}),
                       rotation=keys({0: [0, 0, 0], q(.3): [6, 0, 0], q(.45): [-6, 0, 0], q(.85): [-4, 0, 0], S: [0, 0, 0]})),
         "head": track(rotation=keys({0: [0, 0, 0], q(.3): [14, 0, 0], q(.45): [-12, 0, 0], q(.85): [-10, 0, 0], S: [0, 0, 0]})),
         "jaw": track(rotation=open_), "beak_lower": track(rotation=dict(open_)),
         "tail": track(rotation=keys({0: [0, 0, 0], q(.3): [-20, 0, 0], q(.45): [15, 0, 0], S: [0, 0, 0]}))}
    t.update(keys_lr("ear", {0: [0, 0, 0], q(.3): [30, 0, -10], q(.85): [30, 0, -10], S: [0, 0, 0]}))
    if kind == "legs4":
        for leg in ("leg_front_right", "leg_front_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], q(.3): [-10, 0, 0], q(.45): [-18, 0, 0], q(.85): [-12, 0, 0], S: [0, 0, 0]}))
        for leg in ("leg_back_right", "leg_back_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], q(.45): [12, 0, 0], S: [0, 0, 0]}))
    elif kind == "bird":
        flap = {0: [0, 0, 0], q(.3): [0, 0, 20]}
        for i, f in enumerate((.4, .47, .54, .61, .68, .75)):
            flap[q(f)] = [0, 0, 80 if i % 2 == 0 else -5]
        flap.update({q(.85): [0, 0, 30], S: [0, 0, 0]})
        t.update(keys_lr("wing", flap))
    else:
        t.update(keys_lr("arm", {0: [0, 0, 0], q(.3): [10, 0, 0], q(.45): [-50, 0, 20], q(.85): [-40, 0, 15], S: [0, 0, 0]}))
    return t


def confused(C, k, kind):
    """Dazed: swaying and wobbling, head lolling, eyes ringed, stars circling over its head."""
    t = {"body": track(rotation=[wave(C / 2, 4), 0, wave(C, 7)], position=[wave(C, 0.9 * k), 0, 0]),
         "neck": track(rotation=[0, wave(C, 6), wave(C, 6, 45)]),
         "head": track(rotation=[wave(C / 2, 6, 90), wave(C, 14), wave(C, 12, 90)]),
         "jaw": track(rotation=[8, 0, 0]), "beak_lower": track(rotation=[8, 0, 0]),
         "tail": track(rotation=[wave(C / 2, 8), wave(C, 12), 0]),
         "ear_right": track(rotation=[20, 0, wave(C / 2, 10)]), "ear_left": track(rotation=[20, 0, wave(C / 2, -10)]),
         "fx_stars": track(rotation=[0, "q.anim_time * 240", 0], position=[0, wave(C / 2, 0.6), 0])}
    if kind == "legs4":
        for i, leg in enumerate(("leg_front_right", "leg_front_left", "leg_back_right", "leg_back_left")):
            t[leg] = track(rotation=[wave(C / 2, 5, i * 90), 0, wave(C, 4, i * 60)])
    elif kind == "bird":
        t["wing_right"] = track(rotation=[0, 0, wave(C / 2, 10, 0, 20)])
        t["wing_left"] = track(rotation=[0, 0, wave(C / 2, -10, 0, -20)])
    else:
        t["arm_right"] = track(rotation=[wave(C / 2, 15, 0, -20), 0, 0])
        t["arm_left"] = track(rotation=[wave(C / 2, 15, 180, -20), 0, 0])
    t.update(away("squint_right", "squint_left", "faint_right", "faint_left", "fx_impact"))
    return t


def faint(F, k, kind, hw):
    """Fainting: a stagger, then over onto its side (resting on the ground, not sinking into it), limp, eyes crossed
    out. It stays down until it's recalled."""
    f = lambda x: F * x
    show_x = keys({0: list(HIDE), f(.28): list(HIDE), f(.3): [1, 1, 1]})
    t = {"root": track(rotation=keys({0: [0, 0, 0], f(.2): [0, 0, -8], f(.5): [0, 0, 92], f(.6): [0, 0, 84], f(.7): [0, 0, 90]}),
                       position=keys({0: [0, 0, 0], f(.2): [0, 0, 0], f(.5): [0, hw, 0], f(.7): [0, hw, 0]})),
         "body": track(rotation=keys({0: [0, 0, 0], f(.2): [8, 0, 0], f(.5): [0, 0, 0]})),
         "neck": track(rotation=keys({0: [0, 0, 0], f(.2): [10, 0, 0], f(.6): [15, 0, 0]})),
         "head": track(rotation=keys({0: [0, 0, 0], f(.2): [18, 0, -10], f(.6): [25, 0, 10], F: [25, 0, 10]})),
         "jaw": track(rotation=keys({0: [0, 0, 0], f(.6): [14, 0, 0]})), "beak_lower": track(rotation=keys({0: [0, 0, 0], f(.6): [14, 0, 0]})),
         "tail": track(rotation=keys({0: [0, 0, 0], f(.6): [25, 0, 0]})),
         "faint_right": track(scale=show_x), "faint_left": track(scale=dict(show_x))}
    t.update(keys_lr("ear", {0: [0, 0, 0], f(.6): [35, 0, -25]}))
    if kind == "legs4":
        for leg in ("leg_front_right", "leg_front_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], f(.2): [-10, 0, 0], f(.7): [-20, 0, 8]}))
        for leg in ("leg_back_right", "leg_back_left"):
            t[leg] = track(rotation=keys({0: [0, 0, 0], f(.7): [15, 0, -8]}))
    elif kind == "bird":
        t.update(keys_lr("wing", {0: [0, 0, 0], f(.7): [0, 0, 35]}))
        t.update(keys_lr("leg", {0: [0, 0, 0], f(.7): [-30, 0, 0]}))
    else:
        t.update(keys_lr("arm", {0: [0, 0, 0], f(.7): [-40, 0, 0]}))
    t.update(away("squint_right", "squint_left", "dizzy_right", "dizzy_left", "fx_stars", "fx_impact"))
    return t


def battle(A, m, info, kind, k=1.0, slow=1.0, extras=None):
    """Adds the battle animations to a creature's set."""
    e = extras or {}
    T, Tc, S = 0.9 * slow, 1.1 * slow, 1.6 * slow * e.get("special_slow", 1.0)
    C, F = 2.4 * slow, 1.5 * slow
    atk = A.add("attack", T, only(m, merge(attack(T, k, 1.0, kind), _extra(e, "attack"))), loop=False)
    p = lambda f: Tc * f
    impact = {"fx_impact": track(scale=keys({0: [1, 1, 1], p(.44): [1, 1, 1], p(.46): [700, 700, 700], p(.52): [1500, 1500, 1500],
                                             p(.66): [1800, 1800, 1800], p(.7): [1, 1, 1]}),
                                 rotation=keys({0: [0, 0, 0], p(.46): [0, 0, 0], p(.7): [0, 0, 45]}))}
    crit = A.add("critical", Tc, only(m, merge(attack(Tc, k, 1.45, kind), impact, _extra(e, "attack"))), loop=False)
    spec = A.add("special", S, only(m, merge(special(S, k, kind), _extra(e, "special"))), loop=False)
    R = 0.7
    recall = A.add("recall", R, only(m, {"root": track(scale=keys({0: [1, 1, 1], R * .15: [1.08, 1.08, 1.08], R * .8: list(HIDE), R: list(HIDE)}),
                                                       position=keys({0: [0, 0, 0], R * .8: [0, 3, 0], R: [0, 3, 0]}))}), loop=False)
    out = A.add("sendout", 0.6, only(m, {"root": track(scale=keys({0: list(HIDE), 0.35: [1.12, 1.12, 1.12], 0.5: [0.96, 0.96, 0.96],
                                                                     0.6: [1, 1, 1]}))}), loop=False)
    conf = A.add("confused", C, only(m, merge(confused(C, k, kind), _extra(e, "confused"))))
    fnt = A.add("faint", F, only(m, merge(faint(F, k, kind, half_width(m)), _extra(e, "faint"))), loop="hold_on_last_frame")
    info["actions"].update({"attack": atk, "critical": crit, "special": spec, "recall": recall, "sendout": out})
    info["move"] = getattr(m, "move_name", "SPECIAL")
    info["poses"].update({"confused": {"anim": conf, "parts": []}, "faint": {"anim": fnt, "parts": []}})
