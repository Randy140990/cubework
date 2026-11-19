"""
Retromon's own creatures' animations, by body: four legs, two (hopping), and birds. Each set is the creature's
standing idle, its livelier battle stance, walking, calling out and getting hit (and flying and hovering for those
that fly). Parts a creature hasn't got are left out, and each creature can add its own (a flame's flicker, a plume).
"""

from monlib import Anims, wave, bounce, keys, track, merge, only, eyelids, pulse, squeeze
from monbattle import battle as battle_set


def _hit(R, k, body_back=4.0, lift=1.2, rear=14, roll=8, head=(22, -14)):
    """Getting hit, on any body: knocked back and up, a flinch with the head snapping down and aside, a shake to settle,
    and the eyes squeezed shut. R is its length; k how big (less for heavy ones)."""
    a, b, c, d = R * 0.1, R * 0.3, R * 0.5, R * 0.7
    hx, hz = head
    return {
        "body": track(position=keys({0: [0, 0, 0], a: [0, lift * k, body_back * k], b: [-0.8 * k, -0.4 * k, body_back * 0.75 * k],
                                     c: [0.6 * k, 0, body_back * 0.35 * k], d: [-0.3 * k, 0, body_back * 0.12 * k], R: [0, 0, 0]}),
                      rotation=keys({0: [0, 0, 0], a: [-rear, 0, roll], b: [rear * 0.35, 0, -roll * 0.6], c: [-rear * 0.2, 0, roll * 0.35],
                                     d: [rear * 0.07, 0, -roll * 0.12], R: [0, 0, 0]})),
        "neck": track(rotation=keys({0: [0, 0, 0], a: [-12, 0, 0], b: [10, 0, 0], c: [-4, 0, 0], R: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], a: [hx, 0, hz], b: [-hx * 0.45, 14, -hz * 0.6], c: [hx * 0.28, -8, hz * 0.3],
                                     d: [-hx * 0.1, 3, 0], R: [0, 0, 0]})),
        "jaw": track(rotation=keys({0: [0, 0, 0], a: [16, 0, 0], b: [10, 0, 0], c: [0, 0, 0], R: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], a: [40, 0, -25], c: [18, 0, -10], R: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], a: [40, 0, 25], c: [18, 0, 10], R: [0, 0, 0]})),
        "tail": track(rotation=keys({0: [0, 0, 0], a: [28, 0, 0], c: [10, 0, 0], R: [0, 0, 0]})),
        **squeeze(R)}


def with_move(m, extras):
    """The creature's own extras, with its signature move's (its effect's parts kept away, and their flight)."""
    out = dict(extras or {})
    for mv in (getattr(m, "move_extras", None), getattr(m, "attack_extras", None)):
        for key, tracks in (mv or {}).items():
            out[key] = merge(out.get(key), tracks)
    return out


def _extra(extras, name):
    e = extras or {}
    return merge(e.get("all"), e.get(name))


def four_legs(sid, m, walk=0.8, stride=30, k=1.0, slow=1.0, extras=None, idle="battle", fly=None):
    """A four-legged creature. k: how big its bobs are (less for heavy ones); slow: how much slower everything is."""
    extras = with_move(m, extras)
    A = Anims(sid)
    L = ("leg_front_right", "leg_back_left"), ("leg_front_left", "leg_back_right")
    P = 4 * slow
    ground = A.add("ground_idle", P, only(m, merge({
        "body": track(position=[0, wave(P, 0.2 * k), 0], scale=pulse(P / 2, 0.012)),
        "neck": track(rotation=[wave(P, 2), 0, 0]),
        "head": track(rotation=[wave(P, 3, 90), wave(P, 9), wave(P, 2, 45)]),
        "jaw": track(rotation=keys({0: [0, 0, 0], 2.4 * slow: [0, 0, 0], 2.6 * slow: [6, 0, 0], 2.9 * slow: [0, 0, 0], P: [0, 0, 0]})),
        "ear_right": track(rotation=[0, wave(P / 2, 5), wave(P / 4, 4)]), "ear_left": track(rotation=[0, wave(P / 2, -5, 60), wave(P / 4, -4, 30)]),
        "tail": track(rotation=[wave(P / 2, 4), wave(P / 2, 10), 0]), "tail2": track(rotation=[wave(P / 2, 6, 60), wave(P / 2, 6, 40), 0]),
        "tail3": track(rotation=[wave(P / 2, 6, 120), 0, 0]),
    }, eyelids(P * 0.78), _extra(extras, "idle"))))
    B = 2 * slow
    battle = A.add("battle_idle", B, only(m, merge({
        "body": track(position=[0, bounce(B / 2, 0.45 * k), 0], rotation=[wave(B / 2, 1.2), 0, 0]),
        "neck": track(rotation=[wave(B / 2, 3, 0, 4), 0, 0]),
        "head": track(rotation=[wave(B / 2, 3, 90, -3), wave(B, 5), 0]),
        "ear_right": track(rotation=[wave(B / 2, 3), 0, wave(B / 4, 5)]), "ear_left": track(rotation=[wave(B / 2, 3), 0, wave(B / 4, -5)]),
        "tail": track(rotation=[wave(B / 2, 6), wave(B / 4, 14), 0]), "tail2": track(rotation=[0, wave(B / 4, 10, 60), 0]),
        "leg_front_right": track(rotation=[wave(B / 2, 3, 0, -5), 0, 0]), "leg_front_left": track(rotation=[wave(B / 2, 3, 0, 5), 0, 0]),
    }, eyelids(B * 0.7), _extra(extras, "battle"))))
    W = walk
    tracks = {"body": track(position=[0, bounce(W / 2, 0.5 * k), 0], rotation=[0, 0, wave(W, 2.5)]),
              "neck": track(rotation=[wave(W / 2, 3), 0, 0]),
              "head": track(rotation=[wave(W / 2, 2.5, 90), wave(W, -3), 0]),
              "tail": track(rotation=[wave(W / 2, 6), wave(W, 16), 0]), "tail2": track(rotation=[wave(W / 2, 6, 60), wave(W, 10, 60), 0]),
              "ear_right": track(rotation=[wave(W / 2, 6), 0, 0]), "ear_left": track(rotation=[wave(W / 2, 6), 0, 0])}
    for leg in L[0]:
        tracks[leg] = track(rotation=[wave(W, stride), 0, 0])
        tracks[leg.replace("leg_", "shin_")] = track(rotation=[bounce(W, -22 * min(1, k + 0.2), 0), 0, 0])
    for leg in L[1]:
        tracks[leg] = track(rotation=[wave(W, -stride), 0, 0])
        tracks[leg.replace("leg_", "shin_")] = track(rotation=[bounce(W, -22 * min(1, k + 0.2), 90), 0, 0])
    walk_id = A.add("ground_walk", W, only(m, merge(tracks, eyelids(W * 0.6), _extra(extras, "walk"))))
    C = 1.2 * slow
    cry = A.add("cry", C, only(m, merge({
        "body": track(position=keys({0: [0, 0, 0], C * 0.2: [0, 1.0 * k, 0], C * 0.65: [0, 0.8 * k, 0], C: [0, 0, 0]}),
                      rotation=keys({0: [0, 0, 0], C * 0.2: [-8, 0, 0], C * 0.65: [-8, 0, 0], C: [0, 0, 0]})),
        "neck": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-12, 0, 0], C * 0.65: [-12, 0, 0], C: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-22, 0, 0], C * 0.4: [-16, 0, 0], C * 0.6: [-22, 0, 0], C: [0, 0, 0]})),
        "jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [42, 0, 0], C * 0.4: [30, 0, 0], C * 0.6: [42, 0, 0], C * 0.8: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-18, 0, -14], C * 0.8: [-18, 0, -14], C: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-18, 0, 14], C * 0.8: [-18, 0, 14], C: [0, 0, 0]})),
        "leg_front_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-14, 0, 0], C * 0.65: [-14, 0, 0], C: [0, 0, 0]})),
        "leg_front_left": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-14, 0, 0], C * 0.65: [-14, 0, 0], C: [0, 0, 0]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    legs = {"leg_front_right": track(rotation=keys({0: [0, 0, 0], R * 0.1: [-20, 0, 0], R * 0.3: [8, 0, 0], R * 0.6: [0, 0, 0]})),
            "leg_front_left": track(rotation=keys({0: [0, 0, 0], R * 0.1: [-20, 0, 0], R * 0.3: [8, 0, 0], R * 0.6: [0, 0, 0]})),
            "leg_back_right": track(rotation=keys({0: [0, 0, 0], R * 0.1: [14, 0, 0], R * 0.3: [-6, 0, 0], R * 0.6: [0, 0, 0]})),
            "leg_back_left": track(rotation=keys({0: [0, 0, 0], R * 0.1: [14, 0, 0], R * 0.3: [-6, 0, 0], R * 0.6: [0, 0, 0]}))}
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), legs, _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle if idle == "battle" else ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk_id, "parts": []}}}
    if fly:
        info["poses"]["fly"] = {"anim": A.add("air_fly", fly["period"], only(m, merge(fly["fly"], eyelids(fly["period"] * 0.5), _extra(extras, "fly")))), "parts": []}
        info["poses"]["hover"] = {"anim": A.add("air_hover", fly["hover_period"], only(m, merge(fly["hover"], eyelids(fly["hover_period"] * 0.5), _extra(extras, "hover")))), "parts": []}
    battle_set(A, m, info, "legs4", k, slow, extras)
    return A, info


def wings(flap, amp, rest, period, phase=0.0):
    """The wings' tracks for a flap: up and down about rest (degrees), amp each way. The right wing lifts with a
    positive turn about z (Bedrock's turns), the left with a negative one."""
    return {"wing_right": track(rotation=[0, 0, wave(period, amp, phase, rest)]), "wing_left": track(rotation=[0, 0, wave(period, -amp, phase, -rest)]),
            "wing_mid_right": track(rotation=[0, wave(period, flap, phase + 40), 0]), "wing_mid_left": track(rotation=[0, wave(period, -flap, phase + 40), 0]),
            "wing_tip_right": track(rotation=[0, 0, wave(period, amp * 0.4, phase + 70)]), "wing_tip_left": track(rotation=[0, 0, wave(period, -amp * 0.4, phase + 70)])}


def bird(sid, m, walk=0.5, stride=40, k=1.0, slow=1.0, extras=None, flap=0.36):
    """A bird: pecks and looks about, walks with its head bobbing, flies and hovers."""
    extras = with_move(m, extras)
    A = Anims(sid)
    P = 4 * slow
    peck = keys({0: [0, 0, 0], 2.2 * slow: [0, 0, 0], 2.35 * slow: [0, 25, 0], 2.9 * slow: [0, 25, 0], 3.05 * slow: [0, -20, 0], 3.6 * slow: [0, -20, 0],
                 3.75 * slow: [0, 0, 0], P: [0, 0, 0]})
    ground = A.add("ground_idle", P, only(m, merge({
        "body": track(position=[0, wave(P / 2, 0.15 * k), 0], scale=pulse(P / 4, 0.015)),
        "neck": track(rotation=[wave(P / 2, 3), 0, 0]),
        "head": track(rotation=peck),
        "plume": track(rotation=[wave(P / 4, 6), 0, 0]), "plume_tip": track(rotation=[wave(P / 4, 10, 60), 0, 0]),
        "tail": track(rotation=keys({0: [0, 0, 0], 1.0 * slow: [0, 0, 0], 1.08 * slow: [-16, 0, 0], 1.2 * slow: [0, 0, 0], P: [0, 0, 0]})),
        "wing_right": track(rotation=[0, 0, wave(P / 2, 1.5)]), "wing_left": track(rotation=[0, 0, wave(P / 2, -1.5)]),
    }, eyelids(P * 0.38), _extra(extras, "idle"))))
    W = walk
    walk_id = A.add("ground_walk", W, only(m, merge({
        "leg_right": track(rotation=[wave(W, stride), 0, 0]), "leg_left": track(rotation=[wave(W, -stride), 0, 0]),
        "foot_right": track(rotation=[bounce(W, -18), 0, 0]), "foot_left": track(rotation=[bounce(W, -18, 90), 0, 0]),
        "body": track(position=[0, bounce(W / 2, 0.5 * k), 0], rotation=[0, 0, wave(W, 4)]),
        # The head bobs as birds' do: held back as the foot goes down, then thrust forward.
        "head": track(position=[0, 0, wave(W / 2, 0.5 * k)]),
        "neck": track(rotation=[wave(W / 2, 4), 0, 0]),
        "plume": track(rotation=[wave(W / 2, 10), 0, 0]),
        "tail": track(rotation=[wave(W / 2, 6), 0, 0]),
    }, eyelids(W * 0.6), _extra(extras, "walk"))))
    F = flap
    fly = A.add("air_fly", F, only(m, merge({
        "body": track(rotation=[14, 0, 0], position=[0, wave(F, 0.6 * k, 90), 0]),
        "leg_right": track(rotation=[60, 0, 0]), "leg_left": track(rotation=[60, 0, 0]),
        "head": track(rotation=[-12, 0, 0]), "neck": track(rotation=[-8, 0, 0]),
        "plume": track(rotation=[24, 0, 0]),
        "tail": track(rotation=[wave(F, 6, 0, 10), 0, 0]),
    }, wings(18, 52, 70, F), eyelids(F * 0.5), _extra(extras, "fly"))))
    H = flap * 1.5
    hover = A.add("air_hover", H, only(m, merge({
        "body": track(position=[0, wave(H, 0.8 * k, 90), 0], rotation=[-6, 0, 0]),
        "leg_right": track(rotation=[25, 0, 0]), "leg_left": track(rotation=[25, 0, 0]),
        "plume": track(rotation=[wave(H, 10), 0, 0]),
        "tail": track(rotation=[wave(H, 8, 0, 18), 0, 0]),
    }, wings(24, 42, 65, H), eyelids(H * 0.5), _extra(extras, "hover"))))
    C = 1.0 * slow
    cry = A.add("cry", C, only(m, merge({
        "neck": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-14, 0, 0], C * 0.6: [-14, 0, 0], C: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-28, 0, 0], C * 0.4: [-22, 0, 0], C * 0.6: [-28, 0, 0], C: [0, 0, 0]})),
        "beak_lower": track(rotation=keys({0: [0, 0, 0], C * 0.2: [30, 0, 0], C * 0.4: [12, 0, 0], C * 0.6: [30, 0, 0], C * 0.8: [0, 0, 0]})),
        "plume": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-25, 0, 0], C * 0.6: [12, 0, 0], C: [0, 0, 0]})),
        "wing_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [0, 0, 55], C * 0.4: [0, 0, 35], C * 0.6: [0, 0, 55], C: [0, 0, 0]})),
        "wing_left": track(rotation=keys({0: [0, 0, 0], C * 0.2: [0, 0, -55], C * 0.4: [0, 0, -35], C * 0.6: [0, 0, -55], C: [0, 0, 0]})),
        "body": track(position=keys({0: [0, 0, 0], C * 0.2: [0, 1 * k, 0], C * 0.6: [0, 1 * k, 0], C: [0, 0, 0]})),
        "tail": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-20, 0, 0], C * 0.6: [-20, 0, 0], C: [0, 0, 0]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.7 * slow
    ruffle = {"wing_right": track(rotation=keys({0: [0, 0, 0], R * 0.1: [0, 0, 55], R * 0.3: [0, 0, 18], R * 0.45: [0, 0, 32], R: [0, 0, 0]})),
              "wing_left": track(rotation=keys({0: [0, 0, 0], R * 0.1: [0, 0, -55], R * 0.3: [0, 0, -18], R * 0.45: [0, 0, -32], R: [0, 0, 0]})),
              "tail": track(rotation=keys({0: [0, 0, 0], R * 0.1: [-25, 0, 0], R * 0.5: [-8, 0, 0], R: [0, 0, 0]})),
              "beak_lower": track(rotation=keys({0: [0, 0, 0], R * 0.1: [22, 0, 0], R * 0.45: [0, 0, 0]})),
              "plume": track(rotation=keys({0: [0, 0, 0], R * 0.1: [30, 0, 0], R * 0.35: [-12, 0, 0], R * 0.6: [5, 0, 0], R: [0, 0, 0]}))}
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k, body_back=3.5, rear=16, roll=6, head=(20, -12)), ruffle, _extra(extras, "recoil"))),
                   loop=False)
    info = {"idle": ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk_id, "parts": []}, "fly": {"anim": fly, "parts": []}, "hover": {"anim": hover, "parts": []}}}
    battle_set(A, m, info, "bird", k, slow, extras)
    return A, info


def hopper(sid, m, hop=0.45, height=2.0, k=1.0, extras=None):
    """A creature that hops on long back feet (a jerboa): stands up tall, sniffs, hops along."""
    extras = with_move(m, extras)
    A = Anims(sid)
    ground = A.add("ground_idle", 4.0, only(m, merge({
        "body": track(position=[0, wave(2, 0.14 * k), 0], scale=[1, wave(1, 0.025, 0, 1), 1]),
        "head": track(rotation=[wave(4, 3, 90), wave(4, 12), 0]),
        "snout": track(scale=[1, 1, wave(0.25, 0.12, 0, 1)]),
        "whiskers_right": track(rotation=[0, wave(0.25, 6), 0]), "whiskers_left": track(rotation=[0, wave(0.25, -6), 0]),
        "ear_right": track(rotation=[wave(2, 4), wave(1, 10), 0]), "ear_left": track(rotation=[wave(2, 4, 50), wave(1, -10, 50), 0]),
        "tail": track(rotation=[0, wave(2, 10), 0]), "tail2": track(rotation=[wave(2, 6, 90), wave(2, 6), 0]), "tail3": track(rotation=[wave(2, 8, 150), 0, 0]),
        "arm_right": track(rotation=[wave(1, 6), 0, 0]), "arm_left": track(rotation=[wave(1, 6, 40), 0, 0]),
    }, eyelids(2.2), _extra(extras, "idle"))))
    walk = A.add("ground_walk", hop, only(m, merge({
        # Both feet together: the body sails up, the tail balancing it.
        "body": track(position=[0, bounce(hop, height * k), 0], rotation=[wave(hop, 8, 90), 0, 0]),
        "leg_right": track(rotation=[wave(hop, 30, 180), 0, 0]), "leg_left": track(rotation=[wave(hop, 30, 180), 0, 0]),
        "foot_right": track(rotation=[wave(hop, -20, 180), 0, 0]), "foot_left": track(rotation=[wave(hop, -20, 180), 0, 0]),
        "tail": track(rotation=[wave(hop, 12), 0, 0]), "tail2": track(rotation=[wave(hop, 10, 60), 0, 0]), "tail3": track(rotation=[wave(hop, 10, 120), 0, 0]),
        "ear_right": track(rotation=[wave(hop, 10, 30), 0, 0]), "ear_left": track(rotation=[wave(hop, 10, 30), 0, 0]),
        "arm_right": track(rotation=[-20, 0, 0]), "arm_left": track(rotation=[-20, 0, 0]),
    }, eyelids(hop * 0.6), _extra(extras, "walk"))))
    battle = A.add("battle_idle", 1.0, only(m, merge({
        "body": track(position=[0, bounce(0.5, 0.5 * k), 0]),
        "head": track(rotation=[wave(1, 3, 90, -2), wave(1, 6), 0]),
        "ear_right": track(rotation=[wave(0.5, 5), 0, 0]), "ear_left": track(rotation=[wave(0.5, 5, 40), 0, 0]),
        "tail": track(rotation=[0, wave(0.5, 14), 0]),
        "arm_right": track(rotation=[wave(0.5, 10, 0, -20), 0, 0]), "arm_left": track(rotation=[wave(0.5, 10, 180, -20), 0, 0]),
    }, eyelids(0.7), _extra(extras, "battle"))))
    cry = A.add("cry", 1.0, only(m, merge({
        "body": track(rotation=keys({0: [0, 0, 0], 0.2: [-22, 0, 0], 0.7: [-22, 0, 0], 1.0: [0, 0, 0]}),
                      position=keys({0: [0, 0, 0], 0.2: [0, 1.4 * k, 0], 0.7: [0, 1.4 * k, 0], 1.0: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], 0.2: [-10, 0, 0], 0.4: [-4, 0, 0], 0.6: [-10, 0, 0], 1.0: [0, 0, 0]})),
        "jaw": track(rotation=keys({0: [0, 0, 0], 0.2: [38, 0, 0], 0.4: [22, 0, 0], 0.6: [38, 0, 0], 0.8: [0, 0, 0]})),
        "ear_right": track(rotation=keys({0: [0, 0, 0], 0.2: [-12, 0, 10], 0.8: [-12, 0, 10], 1.0: [0, 0, 0]})),
        "ear_left": track(rotation=keys({0: [0, 0, 0], 0.2: [-12, 0, -10], 0.8: [-12, 0, -10], 1.0: [0, 0, 0]})),
        "arm_right": track(rotation=keys({0: [0, 0, 0], 0.2: [-40, 0, 0], 0.7: [-40, 0, 0], 1.0: [0, 0, 0]})),
        "arm_left": track(rotation=keys({0: [0, 0, 0], 0.2: [-40, 0, 0], 0.7: [-40, 0, 0], 1.0: [0, 0, 0]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.7
    flail = {"arm_right": track(rotation=keys({0: [0, 0, 0], R * 0.1: [-35, 0, 20], R * 0.4: [-10, 0, 5], R: [0, 0, 0]})),
             "arm_left": track(rotation=keys({0: [0, 0, 0], R * 0.1: [-35, 0, -20], R * 0.4: [-10, 0, -5], R: [0, 0, 0]})),
             "ear_right": track(rotation=keys({0: [0, 0, 0], R * 0.1: [50, 0, -20], R * 0.5: [20, 0, -8], R: [0, 0, 0]})),
             "ear_left": track(rotation=keys({0: [0, 0, 0], R * 0.1: [50, 0, 20], R * 0.5: [20, 0, 8], R: [0, 0, 0]}))}
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k, body_back=3.5, lift=1.8, rear=18, roll=8, head=(18, -12)), flail,
                                              _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle if (extras or {}).get("battle_idle") else ground, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk, "parts": []}}}
    battle_set(A, m, info, "hopper", k, 1.0, extras)
    return A, info
