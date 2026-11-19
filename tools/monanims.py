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
    }, tail_chain(P / 2, 8, 4), eyelids(P * 0.78), _extra(extras, "idle"))))
    B = 2 * slow
    battle = A.add("battle_idle", B, only(m, merge({
        "body": track(position=[0, bounce(B / 2, 0.45 * k), 0], rotation=[wave(B / 2, 1.2), 0, 0]),
        "neck": track(rotation=[wave(B / 2, 3, 0, 4), 0, 0]),
        "head": track(rotation=[wave(B / 2, 3, 90, -3), wave(B, 5), 0]),
        "ear_right": track(rotation=[wave(B / 2, 3), 0, wave(B / 4, 5)]), "ear_left": track(rotation=[wave(B / 2, 3), 0, wave(B / 4, -5)]),
        "leg_front_right": track(rotation=[wave(B / 2, 3, 0, -5), 0, 0]), "leg_front_left": track(rotation=[wave(B / 2, 3, 0, 5), 0, 0]),
    }, tail_chain(B / 2, 11, 5), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = walk
    tracks = {"body": track(position=[0, bounce(W / 2, 0.5 * k), 0], rotation=[0, 0, wave(W, 2.5)]),
              "neck": track(rotation=[wave(W / 2, 3), 0, 0]),
              "head": track(rotation=[wave(W / 2, 2.5, 90), wave(W, -3), 0]),
              **tail_chain(W, 13, 5),
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


def wings(flap, amp, rest, period, phase=0.0, span=1.0, unfold=0.0):
    """The wings' tracks for a flap, like an arm from shoulder to fingertips: the shoulder (wing) lifts and beats about
    rest (degrees), amp each way, and each joint after it (the elbow, the wrist, the two fans of flight feathers) bends
    a beat behind the one before, so the beat runs out along the wing in a wave. The right wing lifts with a positive
    turn about z (Bedrock's turns), the left with a negative one. span: how much bigger the wings open out than they
    sit folded (neat at the sides, huge spread in flight). unfold: how far the elbow opens from its folded rest."""
    sc = [span, span, span] if span != 1.0 else None
    out = {}
    # Each joint lags well behind the one before (55 degrees of the beat), and bends further, so the beat visibly rolls
    # out from shoulder to fingertips; spread wings also flex at the elbow and wrist on the upstroke, folding in a
    # little as they rise and opening out again for the downstroke.
    parts = (("wing", 0, amp, 0, rest, 0), ("wing_mid", -unfold, amp * 0.45, flap, 0, 14), ("wing_tip", 0, amp * 0.45, 0, 0, 9),
             ("wing_fan", 0, amp * 0.4, flap * 0.5, 0, 5), ("wing_fan2", 0, amp * 0.4, 0, 0, 0))
    for i, (name, x, z_amp, y_amp, base, flex) in enumerate(parts):
        ph = phase + i * 55
        for side, sgn in (("right", 1), ("left", -1)):
            rx = wave(period, flex, ph + 90, x) if (flex and unfold) else x
            rot = [rx, wave(period, sgn * y_amp, ph + 5) if y_amp else 0, wave(period, sgn * z_amp, ph, sgn * base)]
            out[f"{name}_{side}"] = track(rotation=rot, scale=sc if name == "wing" else None)
    return out


def tail_chain(period, yaw, pitch=0.0, n=6, lag=45):
    """A tail's joints swaying one after another, each a beat behind and a little further than the one before, so a
    long tail ripples along its length (tail, tail2... for those it has)."""
    out = {}
    for i in range(n):
        name = "tail" if i == 0 else f"tail{i + 1}"
        k = 1 + i * 0.25
        out[name] = track(rotation=[wave(period, pitch * k, i * lag) if pitch else 0, wave(period, yaw * k, i * lag) if yaw else 0, 0])
    return out


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
    }, wings(18, 52, 70, F, span=1.9, unfold=75), eyelids(F * 0.5), _extra(extras, "fly"))))
    H = flap * 1.5
    hover = A.add("air_hover", H, only(m, merge({
        "body": track(position=[0, wave(H, 0.8 * k, 90), 0], rotation=[-6, 0, 0]),
        "leg_right": track(rotation=[25, 0, 0]), "leg_left": track(rotation=[25, 0, 0]),
        "plume": track(rotation=[wave(H, 10), 0, 0]),
        "tail": track(rotation=[wave(H, 8, 0, 18), 0, 0]),
    }, wings(24, 42, 65, H, span=1.75, unfold=75), eyelids(H * 0.5), _extra(extras, "hover"))))
    C = 1.0 * slow
    cry = A.add("cry", C, only(m, merge({
        "neck": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-14, 0, 0], C * 0.6: [-14, 0, 0], C: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-28, 0, 0], C * 0.4: [-22, 0, 0], C * 0.6: [-28, 0, 0], C: [0, 0, 0]})),
        "beak_lower": track(rotation=keys({0: [0, 0, 0], C * 0.2: [30, 0, 0], C * 0.4: [12, 0, 0], C * 0.6: [30, 0, 0], C * 0.8: [0, 0, 0]})),
        "plume": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-25, 0, 0], C * 0.6: [12, 0, 0], C: [0, 0, 0]})),
        "wing_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [0, 0, 55], C * 0.4: [0, 0, 35], C * 0.6: [0, 0, 55], C: [0, 0, 0]})),
        # The wings opened out as it calls (the elbows unfold).
        "wing_mid_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-60, 0, 0], C * 0.6: [-60, 0, 0], C: [0, 0, 0]})),
        "wing_mid_left": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-60, 0, 0], C * 0.6: [-60, 0, 0], C: [0, 0, 0]})),
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
        **tail_chain(2, 9, 5),
        "arm_right": track(rotation=[wave(1, 6), 0, 0]), "arm_left": track(rotation=[wave(1, 6, 40), 0, 0]),
    }, eyelids(2.2), _extra(extras, "idle"))))
    walk = A.add("ground_walk", hop, only(m, merge({
        # Both feet together: the body sails up, the tail balancing it.
        "body": track(position=[0, bounce(hop, height * k), 0], rotation=[wave(hop, 8, 90), 0, 0]),
        "leg_right": track(rotation=[wave(hop, 30, 180), 0, 0]), "leg_left": track(rotation=[wave(hop, 30, 180), 0, 0]),
        "foot_right": track(rotation=[wave(hop, -20, 180), 0, 0]), "foot_left": track(rotation=[wave(hop, -20, 180), 0, 0]),
        **tail_chain(hop, 4, 10),
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


# ---------- more body plans: crawlers, cocoons, flying insects ----------

def chain(names, period, pitch=0.0, yaw=0.0, lag=50, grow=0.0, lift=0.0):
    """A chain of joints (a larva's segments, an abdomen) each moving a beat behind the one before."""
    out = {}
    for i, nm in enumerate(names):
        k = 1 + i * grow
        out[nm] = track(rotation=[wave(period, pitch * k, i * lag) if pitch else 0, wave(period, yaw * k, i * lag) if yaw else 0, 0],
                        position=[0, wave(period, lift, i * lag), 0] if lift else None)
    return out


def _antennae(period, amp, spread=0.0):
    return {"antenna_right": track(rotation=[wave(period, amp), 0, wave(period / 2, amp * 0.7, 0, spread)]),
            "antenna_left": track(rotation=[wave(period, amp, 40), 0, wave(period / 2, -amp * 0.7, 30, -spread)])}


def crawler(sid, m, segs, walk=0.9, k=1.0, slow=1.0, extras=None):
    """A crawling larva: its body a chain of segments behind its head (segs, front to back), rippling front to back as
    it crawls; it sways and twitches its antennae at rest and rears up to call."""
    extras = with_move(m, extras)
    A = Anims(sid)
    P = 3.2 * slow
    ground = A.add("ground_idle", P, only(m, merge({
        "body": track(position=[0, wave(P, 0.15 * k), 0]),
        "head": track(rotation=[wave(P, 4, 90), wave(P, 10), wave(P, 3, 45)]),
        "jaw": track(rotation=keys({0: [0, 0, 0], 2.0 * slow: [0, 0, 0], 2.2 * slow: [8, 0, 0], 2.5 * slow: [0, 0, 0], P: [0, 0, 0]})),
    }, _antennae(P / 2, 8), chain(segs, P / 2, 0, 5, 40), eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.6 * slow
    battle = A.add("battle_idle", B, only(m, merge({
        "body": track(position=[0, bounce(B / 2, 0.3 * k), 0], rotation=[wave(B / 2, 2, 0, -3), 0, 0]),
        "head": track(rotation=[wave(B / 2, 5, 0, -6), wave(B, 6), 0]),
    }, _antennae(B / 2, 10), chain(segs, B / 2, 4, 3, 50), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = walk
    walk_id = A.add("ground_walk", W, only(m, merge({
        "body": track(position=[0, bounce(W, 0.35 * k), wave(W, 0.6 * k)]),
        "head": track(rotation=[wave(W, 6), wave(W, 4), 0]),
    }, _antennae(W, 8), chain(segs, W, 10, 2, 60, lift=0.45 * k), eyelids(W * 0.6), _extra(extras, "walk"))))
    C = 1.2 * slow
    up = lambda a: keys({0: [0, 0, 0], C * 0.2: [a, 0, 0], C * 0.65: [a, 0, 0], C: [0, 0, 0]})
    cry = A.add("cry", C, only(m, merge({
        "body": track(rotation=up(-14), position=keys({0: [0, 0, 0], C * 0.2: [0, 1.0 * k, 0], C * 0.65: [0, 1.0 * k, 0], C: [0, 0, 0]})),
        "head": track(rotation=up(-24)), segs[0]: track(rotation=up(12)),
        "jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [38, 0, 0], C * 0.4: [26, 0, 0], C * 0.6: [38, 0, 0], C * 0.8: [0, 0, 0]})),
        "antenna_right": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-20, 0, -22], C * 0.8: [-20, 0, -22], C: [0, 0, 0]})),
        "antenna_left": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-20, 0, 22], C * 0.8: [-20, 0, 22], C: [0, 0, 0]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "legs4", k, slow, extras)
    return A, info


def cocoon(sid, m, k=1.0, slow=1.0, extras=None):
    """A creature shut in its hard cocoon: it can barely move. It sways a little, shudders, and shuffles along in small
    hops."""
    extras = with_move(m, extras)
    A = Anims(sid)
    P = 4.0 * slow
    ground = A.add("ground_idle", P, only(m, merge({
        "body": track(rotation=[wave(P, 1.5, 90), 0, wave(P, 2)], position=[0, wave(P / 2, 0.08 * k), 0]),
        "head": track(rotation=[wave(P, 2), wave(P, 4), 0]),
    }, eyelids(P * 0.6), _extra(extras, "idle"))))
    B = 2.0 * slow
    battle = A.add("battle_idle", B, only(m, merge({
        "body": track(rotation=[wave(B / 2, 2), 0, wave(B, 3)], position=[0, bounce(B / 2, 0.15 * k), 0]),
    }, eyelids(B * 0.7), _extra(extras, "battle"))))
    W = 0.7 * slow
    walk_id = A.add("ground_walk", W, only(m, merge({
        "body": track(position=[0, bounce(W, 1.0 * k), 0], rotation=[wave(W, 4), 0, wave(W * 2, 3)]),
    }, eyelids(W), _extra(extras, "walk"))))
    C = 1.1 * slow
    cry = A.add("cry", C, only(m, merge({
        "body": track(rotation=keys({0: [0, 0, 0], C * 0.1: [0, 0, 6], C * 0.2: [0, 0, -6], C * 0.3: [0, 0, 6], C * 0.4: [0, 0, -6], C * 0.5: [0, 0, 4],
                                     C * 0.6: [0, 0, -3], C: [0, 0, 0]}),
                      scale=keys({0: [1, 1, 1], C * 0.15: [1.05, 1.08, 1.05], C * 0.6: [1.05, 1.08, 1.05], C: [1, 1, 1]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k * 0.5, lift=0.4, rear=6), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "legs4", k, slow, extras)
    return A, info


def insect(sid, m, flap=0.16, amp=45, k=1.0, slow=1.0, extras=None, abdomen=("abdomen", "abdomen2", "abdomen3"), legs=()):
    """A flying insect, always on the wing: its wings beating (the hind pair a beat behind the fore), its body bobbing
    as it hovers, its abdomen swinging, its legs hanging. flap: one wingbeat (a moth's slow, a wasp's a blur)."""
    extras = with_move(m, extras)
    A = Anims(sid)

    def beat(f, a, rest=18):
        return {"wing_right": track(rotation=[0, wave(f, a * 0.2), wave(f, a, 0, rest)]), "wing_left": track(rotation=[0, wave(f, -a * 0.2), wave(f, -a, 0, -rest)]),
                "hindwing_right": track(rotation=[0, wave(f, a * 0.15, 60), wave(f, a * 0.8, 60, rest * 0.6)]),
                "hindwing_left": track(rotation=[0, wave(f, -a * 0.15, 60), wave(f, -a * 0.8, 60, -rest * 0.6)])}
    dangle = {lg: track(rotation=[wave(1.4 * slow, 8, i * 40), 0, 0]) for i, lg in enumerate(legs)}
    P = 2.0 * slow
    base = {"body": track(position=[0, wave(P, 1.2 * k), 0], rotation=[wave(P, 3, 60), 0, wave(P * 2, 2)]),
            "head": track(rotation=[wave(P, 4, 90), wave(P * 2, 8), 0])}
    ground = A.add("ground_idle", P, only(m, merge(base, beat(flap, amp), _antennae(P / 2, 8), chain(abdomen, P, 5, 0, 50), dangle,
                                                   eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.0 * slow
    battle = A.add("battle_idle", B, only(m, merge({"body": track(position=[0, wave(B, 1.5 * k), 0], rotation=[wave(B, 4, 60, -4), 0, 0]),
                                                    "head": track(rotation=[wave(B, 4, 0, -4), wave(B * 2, 6), 0])},
                                                   beat(flap, amp * 1.1), _antennae(B / 2, 10), chain(abdomen, B, 7, 0, 50), dangle,
                                                   eyelids(B * 0.7), _extra(extras, "battle"))))
    W = 1.0 * slow
    walk_id = A.add("ground_walk", W, only(m, merge({"body": track(position=[0, wave(W, 1.4 * k), 0], rotation=[12, 0, wave(W, 3)])},
                                                    beat(flap * 0.9, amp * 1.1), _antennae(W / 2, 6), chain(abdomen, W, 6, 0, 50), dangle,
                                                    eyelids(W * 0.6), _extra(extras, "walk"))))
    fly = A.add("air_fly", W, only(m, merge({"body": track(position=[0, wave(W, 1.8 * k), 0], rotation=[18, 0, wave(W, 4)])},
                                            beat(flap * 0.85, amp * 1.2), chain(abdomen, W, 6, 0, 50), dangle, eyelids(W * 0.5), _extra(extras, "fly"))))
    C = 1.2 * slow
    cry = A.add("cry", C, only(m, merge({
        "body": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-14, 0, 0], C * 0.65: [-14, 0, 0], C: [0, 0, 0]}),
                      position=keys({0: [0, 0, 0], C * 0.2: [0, 2 * k, 0], C * 0.65: [0, 2 * k, 0], C: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-18, 0, 0], C * 0.65: [-18, 0, 0], C: [0, 0, 0]})),
        "jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [34, 0, 0], C * 0.6: [34, 0, 0], C * 0.8: [0, 0, 0]})),
    }, beat(flap * 0.7, amp * 1.4, 30), _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), beat(flap, amp), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk_id, "parts": []}, "fly": {"anim": fly, "parts": []}}}
    battle_set(A, m, info, "bird", k, slow, extras)
    return A, info


def slither(segs, period, amp, lag=50.0, grow=0.06, coil=None, body_amp=None):
    """Tracks for a snake's body to slither: a wave runs back down its length, each segment's heading following the one
    in front of it a beat later (lag degrees), so the body draws an S that travels back along it while the head goes
    straight on. Each joint turns by the difference between its heading and the one before. coil: resting turns added
    to each joint (curled up, or lying in curves)."""
    out = {}
    heads = [amp * (0.55 if body_amp is None else body_amp)] + [amp * (1 + i * grow) for i in range(len(segs))]
    out["body"] = track(rotation=[0, wave(period, heads[0], 0), 0])
    for i, nm in enumerate(segs):
        a1, a0 = heads[i + 1], heads[i]
        e = f"{wave(period, a1, -(i + 1) * lag)} - {wave(period, a0, -i * lag)}"
        if coil:
            e = f"{coil[i]} + {e}"
        out[nm] = track(rotation=[0, e, 0])
    return out


def serpent(sid, m, segs, necks=("neck", "neck2"), k=1.0, slow=1.0, extras=None, coil=None):
    """A snake: its body a chain of segments behind its reared-up front (segs, from behind the neck to the tail tip),
    the neck joints holding the head. At rest it lies curled (coil: each joint's resting turn), swaying a little and
    flicking its tongue; it slithers with a wave running back down its length, the S of its body travelling along it
    while its head stays steady and looks ahead; it rears higher to hiss with its jaws wide (a frill flaring)."""
    extras = with_move(m, extras)
    A = Anims(sid)
    flick = lambda L, at: track(scale=keys({0: [1, 1, 0.05], at: [1, 1, 0.05], at + 0.08: [1, 1, 1], at + 0.22: [1, 1, 1], at + 0.3: [1, 1, 0.05], L: [1, 1, 0.05]}))
    steady = lambda P, amp: {necks[0]: track(rotation=[0, wave(P, -amp * 0.5, 0), 0]), necks[-1]: track(rotation=[0, wave(P, -amp * 0.3, 0), 0])}
    P = 3.0 * slow
    ground = A.add("ground_idle", P, only(m, merge(slither(segs, P * 2, 4, 40, coil=coil), {
        "head": track(rotation=[wave(P, 4, 90), wave(P, 10), 0]),
        "tongue": flick(P, 1.1 * slow), "hood": track(scale=[wave(P, 0.04, 0, 1), 1, 1]),
    }, chain(list(necks), P, 3, 8, 60), eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.6 * slow
    battle = A.add("battle_idle", B, only(m, merge(slither(segs, B, 5, 45, coil=coil), {
        "head": track(rotation=[wave(B / 2, 4, 0, 0), wave(B, 8), 0]),
        "tongue": flick(B, 0.5 * slow), "hood": track(scale=[wave(B / 2, 0.06, 0, 1.06), 1, 1]),
    }, chain(list(necks), B / 2, 5, 6, 60), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = 1.1 * slow
    sl = slither(segs, W, 30, 52)
    walk_id = A.add("ground_walk", W, only(m, merge(sl, steady(W, 30 * 0.55), {
        "head": track(rotation=[0, wave(W, -4, 90), 0]), "tongue": flick(W, 0.3),
    }, eyelids(W * 0.6), _extra(extras, "walk"))))
    C = 1.3 * slow
    up = lambda a: keys({0: [0, 0, 0], C * 0.2: [a, 0, 0], C * 0.7: [a, 0, 0], C: [0, 0, 0]})
    cry = A.add("cry", C, only(m, merge({
        necks[0]: track(rotation=up(-16)), necks[-1]: track(rotation=up(-10)), "head": track(rotation=up(-14)),
        "jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [48, 0, 0], C * 0.7: [48, 0, 0], C * 0.85: [0, 0, 0]})),
        "tongue": track(scale=keys({0: [1, 1, 0.05], C * 0.2: [1, 1, 1.3], C * 0.7: [1, 1, 1.3], C * 0.8: [1, 1, 0.05]})),
        "hood": track(scale=keys({0: [1, 1, 1], C * 0.2: [1.35, 1.15, 1], C * 0.7: [1.35, 1.15, 1], C: [1, 1, 1]})),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k, lift=0.6), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "legs4", k, slow, extras)
    return A, info


def flyer(sid, m, flap=0.5, amp=40, rest=20, span=1.0, unfold=75, k=1.0, slow=1.0, extras=None, legs=()):
    """A creature always on the wing (a bat): its jointed wings unfolded and beating in a wave (wings), its body bobbing
    as it hovers, its feet hanging. Dives forward to fly, beats harder to call."""
    extras = with_move(m, extras)
    A = Anims(sid)
    dangle = {lg: track(rotation=[wave(1.4 * slow, 8, i * 40), 0, 0]) for i, lg in enumerate(legs)}
    P = 2.0 * slow
    ground = A.add("ground_idle", P, only(m, merge({"body": track(position=[0, wave(flap * 2, 1.4 * k, 90), 0], rotation=[wave(P, 3), 0, wave(P * 2, 2)]),
                                                     "head": track(rotation=[wave(P, 4, 90), wave(P * 2, 10), 0]),
                                                     "ear_right": track(rotation=[0, 0, wave(P / 2, 6)]), "ear_left": track(rotation=[0, 0, wave(P / 2, -6)])},
                                                    wings(10, amp, rest, flap, span=span, unfold=unfold), dangle, eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.0 * slow
    battle = A.add("battle_idle", flap * 2, only(m, merge({"body": track(position=[0, wave(flap * 2, 1.8 * k, 90), 0], rotation=[-4, 0, 0])},
                                                          wings(12, amp * 1.1, rest, flap, span=span, unfold=unfold), dangle, eyelids(B), _extra(extras, "battle"))))
    walk_id = A.add("ground_walk", flap * 2, only(m, merge({"body": track(position=[0, wave(flap * 2, 1.4 * k, 90), 0], rotation=[14, 0, 0])},
                                                           wings(12, amp, rest, flap, span=span, unfold=unfold), dangle, eyelids(B), _extra(extras, "walk"))))
    fly = A.add("air_fly", flap * 2, only(m, merge({"body": track(position=[0, wave(flap * 2, 2 * k, 90), 0], rotation=[20, 0, 0])},
                                                   wings(14, amp * 1.2, rest, flap * 0.9, span=span, unfold=unfold), dangle, eyelids(B), _extra(extras, "fly"))))
    C = 1.2 * slow
    cry = A.add("cry", C, only(m, merge({
        "body": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-12, 0, 0], C * 0.65: [-12, 0, 0], C: [0, 0, 0]})),
        "head": track(rotation=keys({0: [0, 0, 0], C * 0.2: [-20, 0, 0], C * 0.65: [-20, 0, 0], C: [0, 0, 0]})),
        "jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [45, 0, 0], C * 0.65: [45, 0, 0], C * 0.85: [0, 0, 0]})),
    }, wings(14, amp * 1.4, rest + 10, flap * 0.8, span=span, unfold=unfold), _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), wings(10, amp, rest, flap, span=span, unfold=unfold), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil},
            "poses": {"walk": {"anim": walk_id, "parts": []}, "fly": {"anim": fly, "parts": []}}}
    battle_set(A, m, info, "bird", k, slow, extras)
    return A, info


def burrower(sid, m, bodies=("body",), k=1.0, slow=1.0, extras=None):
    """Creatures that live in a mound of earth (moles): each body bobs up and down out of it, sniffing; they dip under
    and pop up again to move, and rise high out of the mound to call."""
    extras = with_move(m, extras)
    A = Anims(sid)
    P = 2.4 * slow
    bob = lambda per, amp, ph=0.0: {b: track(position=[0, wave(per, amp * k, ph + i * 120), 0], rotation=[0, wave(per * 2, 8, ph + i * 90), 0])
                                    for i, b in enumerate(bodies)}
    ground = A.add("ground_idle", P, only(m, merge(bob(P, 0.4), {"head": track(rotation=[wave(P, 4), wave(P, 8), 0])},
                                                   eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.2 * slow
    battle = A.add("battle_idle", B, only(m, merge(bob(B, 0.7), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = 0.8 * slow
    walk_id = A.add("ground_walk", W, only(m, merge({b: track(position=[0, f"(math.sin(q.anim_time * {450 / W:.1f} + {i * 90}) - 0.4) * {2.5 * k:.2f}", 0])
                                                     for i, b in enumerate(bodies)}, {"mound": track(scale=[1, wave(W / 2, 0.06, 0, 1), 1])},
                                                    eyelids(W), _extra(extras, "walk"))))
    C = 1.2 * slow
    cry = A.add("cry", C, only(m, merge({b: track(position=keys({0: [0, 0, 0], C * 0.2: [0, 3 * k, 0], C * 0.7: [0, 3 * k, 0], C: [0, 0, 0]}))
                                         for b in bodies}, {"jaw": track(rotation=keys({0: [0, 0, 0], C * 0.2: [35, 0, 0], C * 0.7: [35, 0, 0], C: [0, 0, 0]}))},
                                        _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge({b: track(position=keys({0: [0, 0, 0], R * 0.15: [0, -2 * k, 1 * k], R * 0.6: [0, 0, 0]}))
                                               for b in bodies}, squeeze(R), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "legs4", k, slow, extras)
    return A, info


def biped(sid, m, walk=0.7, stride=28, k=1.0, slow=1.0, extras=None, guard=40):
    """A creature that stands and walks upright: hips, knees and ankles bending as it steps, its arms swinging from the
    shoulder and bending at the elbow, a fighting guard in battle (arms raised, knees bent), arms flung up to call."""
    extras = with_move(m, extras)
    A = Anims(sid)
    P = 3.0 * slow
    lr = lambda name, rot: {name + "_right": track(rotation=rot(1)), name + "_left": track(rotation=rot(-1))}
    ground = A.add("ground_idle", P, only(m, merge({
        "body": track(position=[0, wave(P / 2, 0.15 * k), 0], scale=pulse(P / 2, 0.012)),
        "head": track(rotation=[wave(P, 3, 90), wave(P, 12), wave(P, 2, 45)]),
        "jaw": track(rotation=keys({0: [0, 0, 0], 2.0 * slow: [0, 0, 0], 2.2 * slow: [6, 0, 0], 2.5 * slow: [0, 0, 0], P: [0, 0, 0]})),
        "ear_right": track(rotation=[0, 0, wave(P / 2, 4)]), "ear_left": track(rotation=[0, 0, wave(P / 2, -4)]),
    }, lr("arm", lambda d: [wave(P, 3, 90 if d > 0 else 270), 0, d * 4]), lr("forearm", lambda d: [wave(P, 3, 0, -8), 0, 0]),
        tail_chain(P / 2, 8, 3), eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.2 * slow
    battle = A.add("battle_idle", B, only(m, merge({
        "body": track(position=[0, bounce(B / 2, 0.35 * k, 0, -0.4 * k), 0], rotation=[wave(B, 2, 0, 4), wave(B, 4), 0]),
        "head": track(rotation=[wave(B / 2, 2, 0, -4), wave(B, 5), 0]),
    }, lr("arm", lambda d: [wave(B / 2, 4, 0 if d > 0 else 180, -guard), 0, d * 10]), lr("forearm", lambda d: [wave(B / 2, 5, 40, -guard * 1.3), 0, 0]),
        lr("leg", lambda d: [-12, 0, d * 4]), lr("shin", lambda d: [18, 0, 0]), lr("foot", lambda d: [-6, 0, 0]),
        tail_chain(B / 2, 10, 4), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = walk
    walk_id = A.add("ground_walk", W, only(m, merge({
        "body": track(position=[0, bounce(W / 2, 0.45 * k), 0], rotation=[4, wave(W, 4), wave(W, 3)]),
        "head": track(rotation=[wave(W / 2, 2), wave(W, -3), 0]),
        "leg_right": track(rotation=[wave(W, stride), 0, 0]), "leg_left": track(rotation=[wave(W, -stride), 0, 0]),
        "shin_right": track(rotation=[bounce(W, 34, 0), 0, 0]), "shin_left": track(rotation=[bounce(W, 34, 90), 0, 0]),
        "foot_right": track(rotation=[wave(W, -10, 60), 0, 0]), "foot_left": track(rotation=[wave(W, 10, 60), 0, 0]),
        "arm_right": track(rotation=[wave(W, -stride * 0.7), 0, 4]), "arm_left": track(rotation=[wave(W, stride * 0.7), 0, -4]),
        "forearm_right": track(rotation=[wave(W, -10, 30, -14), 0, 0]), "forearm_left": track(rotation=[wave(W, 10, 30, -14), 0, 0]),
    }, tail_chain(W, 12, 4), eyelids(W * 0.6), _extra(extras, "walk"))))
    C = 1.2 * slow
    up = lambda a: keys({0: [0, 0, 0], C * 0.2: a, C * 0.7: a, C: [0, 0, 0]})
    cry = A.add("cry", C, only(m, merge({
        "body": track(rotation=up([-8, 0, 0]), position=keys({0: [0, 0, 0], C * 0.2: [0, 1 * k, 0], C * 0.7: [0, 1 * k, 0], C: [0, 0, 0]})),
        "head": track(rotation=up([-20, 0, 0])), "jaw": track(rotation=up([40, 0, 0])),
        "arm_right": track(rotation=up([-130, 0, 25])), "arm_left": track(rotation=up([-130, 0, -25])),
        "forearm_right": track(rotation=up([-20, 0, 0])), "forearm_left": track(rotation=up([-20, 0, 0])),
    }, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "hopper", k, slow, extras)
    return A, info


def floater(sid, m, tentacles=(), k=1.0, slow=1.0, extras=None, bob=1.0, pulse_bell=0.0):
    """A creature that floats (a jellyfish, a hovering stone): it bobs and drifts, its tentacles (each a list of joint
    names, root first) rippling below it a beat apart; its arms, if it has them, drifting at its sides. It tips forward
    to move, rises and flares out to call. pulse_bell: how much its bell (body) swells and narrows as it swims."""
    extras = with_move(m, extras)
    A = Anims(sid)

    def tents(period, amp, yaw):
        out = {}
        for j, names in enumerate(tentacles):
            out.update(chain(names, period, pitch=amp, yaw=yaw, lag=55, grow=0.35))
            for i, nm in enumerate(names):
                out[nm] = track(rotation=[wave(period, amp * (1 + i * 0.35), i * 55 + j * 47), wave(period * 1.5, yaw * (1 + i * 0.3), i * 50 + j * 83), 0])
        return out
    sw = (lambda P: track(position=[0, wave(P, bob * k, 90), 0], rotation=[wave(P, 3), wave(P * 2, 6), wave(P * 1.3, 3)],
                          scale=[wave(P, pulse_bell, 0, 1), wave(P, -pulse_bell, 0, 1), wave(P, pulse_bell, 0, 1)] if pulse_bell else None))
    arms = lambda P, a, base=0: {"arm_right": track(rotation=[wave(P, a, 0, base), 0, wave(P, a * 0.6, 90, 10)]),
                                 "arm_left": track(rotation=[wave(P, a, 40, base), 0, wave(P, -a * 0.6, 130, -10)]),
                                 "forearm_right": track(rotation=[wave(P, a * 0.6, 60, -10), 0, 0]), "forearm_left": track(rotation=[wave(P, a * 0.6, 100, -10), 0, 0])}
    P = 2.4 * slow
    ground = A.add("ground_idle", P, only(m, merge({"body": sw(P), "head": track(rotation=[wave(P, 3, 90), wave(P * 2, 8), 0])},
                                                    arms(P, 6), tents(P, 10, 6), eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.4 * slow
    battle = A.add("battle_idle", B, only(m, merge({"body": sw(B)}, arms(B, 10, -30), tents(B, 14, 8), eyelids(B * 0.7), _extra(extras, "battle"))))
    W = 1.2 * slow
    walk_id = A.add("ground_walk", W, only(m, merge({"body": track(position=[0, wave(W, bob * 0.8 * k, 90), 0], rotation=[12, 0, wave(W, 3)],
                                                             scale=[wave(W, pulse_bell, 0, 1), wave(W, -pulse_bell, 0, 1), wave(W, pulse_bell, 0, 1)] if pulse_bell else None)},
                                                     arms(W, 12, 20), tents(W, 22, 4), eyelids(W * 0.7), _extra(extras, "walk"))))
    C = 1.3 * slow
    up = lambda a: keys({0: [0, 0, 0], C * 0.2: a, C * 0.7: a, C: [0, 0, 0]})
    flare = {}
    for names in tentacles:
        for i, nm in enumerate(names):
            flare[nm] = track(rotation=up([-30 if i == 0 else -10, 0, 0]))
    cry = A.add("cry", C, only(m, merge({"body": track(position=keys({0: [0, 0, 0], C * 0.2: [0, 2 * k, 0], C * 0.7: [0, 2 * k, 0], C: [0, 0, 0]}),
                                                     rotation=up([-10, 0, 0])),
                                         "jaw": track(rotation=up([35, 0, 0])), "head": track(rotation=up([-14, 0, 0])),
                                         "arm_right": track(rotation=up([-110, 0, 30])), "arm_left": track(rotation=up([-110, 0, -30]))},
                                        flare, _extra(extras, "cry"))), loop=False)
    R = 0.75 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), tents(R, 24, 10), _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "hopper", k, slow, extras)
    return A, info


def ooze(sid, m, k=1.0, slow=1.0, extras=None):
    """A creature of ooze (a slime): it sags and swells as it breathes, its drippy arms swaying; it lurches along by
    surging forward and slumping, rears up tall to call, and splats flat when hit."""
    extras = with_move(m, extras)
    A = Anims(sid)
    sq = lambda P, a, ph=0: track(scale=[wave(P, a, ph, 1), wave(P, -a * 1.4, ph, 1), wave(P, a, ph, 1)])
    arms = lambda P, a, base=0: {"arm_right": track(rotation=[wave(P, a, 0, base), 0, wave(P, a * 0.5, 60, 6)]),
                                 "arm_left": track(rotation=[wave(P, a, 50, base), 0, wave(P, -a * 0.5, 110, -6)]),
                                 "forearm_right": track(rotation=[wave(P, a * 0.8, 40, -6), 0, 0]),
                                 "forearm_left": track(rotation=[wave(P, a * 0.8, 90, -6), 0, 0])}
    P = 3.0 * slow
    ground = A.add("ground_idle", P, only(m, merge({"body": sq(P, 0.04), "head": track(rotation=[wave(P, 3, 90), wave(P * 2, 8), 0]),
                                                    "puddle": sq(P, -0.03, 90), "jaw": track(rotation=[wave(P, 4, 0, 4), 0, 0])},
                                                   arms(P, 8), eyelids(P * 0.7), _extra(extras, "idle"))))
    B = 1.4 * slow
    battle = A.add("battle_idle", B, only(m, merge({"body": sq(B, 0.06), "puddle": sq(B, -0.04, 90)}, arms(B, 10, -30), eyelids(B * 0.7),
                                                   _extra(extras, "battle"))))
    W = 1.1 * slow
    walk_id = A.add("ground_walk", W, only(m, merge({"body": track(scale=[wave(W, 0.05, 0, 1), wave(W, -0.08, 0, 1), wave(W, 0.1, 90, 1)],
                                                                   rotation=[wave(W, 6, 90, 4), 0, 0], position=[0, 0, wave(W, -0.6 * k, 90)]),
                                                     "puddle": track(scale=[1, 1, wave(W, 0.12, 0, 1)])},
                                                    arms(W, 16, -10), eyelids(W * 0.7), _extra(extras, "walk"))))
    C = 1.3 * slow
    up = lambda a: keys({0: [0, 0, 0], C * 0.25: a, C * 0.7: a, C: [0, 0, 0]})
    cry = A.add("cry", C, only(m, merge({"body": track(scale=keys({0: [1, 1, 1], C * 0.25: [0.9, 1.25, 0.9], C * 0.7: [0.9, 1.25, 0.9], C: [1, 1, 1]})),
                                         "jaw": track(rotation=up([40, 0, 0])), "head": track(rotation=up([-12, 0, 0])),
                                         "arm_right": track(rotation=up([-120, 0, 30])), "arm_left": track(rotation=up([-120, 0, -30]))},
                                        _extra(extras, "cry"))), loop=False)
    R = 0.8 * slow
    recoil = A.add("recoil", R, only(m, merge(_hit(R, k), {"body": track(scale=keys({0: [1, 1, 1], R * 0.15: [1.25, 0.7, 1.25], R * 0.5: [0.95, 1.08, 0.95], R: [1, 1, 1]}))},
                                              _extra(extras, "recoil"))), loop=False)
    info = {"idle": battle, "ground": ground, "actions": {"cry": cry, "recoil": recoil}, "poses": {"walk": {"anim": walk_id, "parts": []}}}
    battle_set(A, m, info, "hopper", k, slow, extras)
    return A, info
