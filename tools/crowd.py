#!/usr/bin/env python3
"""
Makes Orbit Arena's crowd sounds from scratch (no recordings, so nothing to license): a stadium murmur
that loops, small and big cheers, a roar for "FIGHT!", an uproar for a win, applause, an "ooh" when you
take a big hit and an "aww" when you lose.

Each sound is a crowd of synthetic voices: a pulse train at a voice's pitch (with jitter, so no two are
alike) shaped by vowel formants, given a pitch contour and an envelope, scattered across the stereo field
and through a big reverb. Applause is hundreds of claps, each a short burst of filtered noise.

Usage: crowd.py <out dir>   (writes <out dir>/crowd/*.ogg; needs numpy, scipy and ffmpeg)
"""
import os
import subprocess
import sys

import numpy as np
from scipy.signal import butter, fftconvolve, lfilter, sosfilt

SR = 44100
rng = np.random.default_rng(20261114)

# Vowel formants (F1, F2, F3) in Hz.
VOWELS = {
    "ah": (760, 1250, 2550), "eh": (580, 1850, 2600), "ee": (300, 2250, 3000), "oh": (520, 900, 2500),
    "oo": (350, 820, 2300), "ae": (690, 1700, 2500), "uh": (620, 1180, 2550),
}


def resonator(x, f, bw):
    """A two-pole resonator at f Hz with bandwidth bw Hz."""
    r = np.exp(-np.pi * bw / SR)
    c = -r * r
    b = 2 * r * np.cos(2 * np.pi * f / SR)
    a0 = 1 - b - c
    return lfilter([a0], [1, -b, -c], x)


def formants(x, vowel, shift=1.0):
    f1, f2, f3 = (f * shift for f in VOWELS[vowel])
    return resonator(x, f1, 90) * 1.0 + resonator(x, f2, 120) * 0.55 + resonator(x, f3, 170) * 0.28


def glottal(f0, jitter=0.012):
    """A pulse train following the pitch curve f0 (one value per sample), softened like a glottis."""
    n = len(f0)
    wobble = 1 + jitter * np.cumsum(rng.normal(0, 1, n)) / np.sqrt(np.arange(1, n + 1)) * 4
    phase = np.cumsum(f0 * wobble / SR)
    pulses = np.zeros(n)
    idx = np.nonzero(np.diff(np.floor(phase)) > 0)[0]
    pulses[idx] = 1.0
    # Two one-pole lowpasses: the glottal pulse's slope.
    for _ in range(2):
        pulses = lfilter([0.18], [1, -0.82], pulses)
    return pulses - np.mean(pulses)


def envelope(n, attack, release, shape=1.0):
    t = np.arange(n) / SR
    dur = n / SR
    env = np.minimum(1, t / max(attack, 1e-3)) * np.minimum(1, (dur - t) / max(release, 1e-3))
    return np.clip(env, 0, 1) ** shape


def voice(dur, f0_start, f0_peak, f0_end, vowel_from, vowel_to, loud=1.0, breath=0.25, female=False):
    """One shout: a pitch rise and fall over dur seconds, gliding from one vowel to another."""
    n = int(dur * SR)
    t = np.linspace(0, 1, n)
    peak_at = rng.uniform(0.2, 0.45)
    f0 = np.where(t < peak_at, f0_start + (f0_peak - f0_start) * np.sin(t / peak_at * np.pi / 2),
                  f0_peak + (f0_end - f0_peak) * np.clip((t - peak_at) / (1 - peak_at), 0, 1) ** 1.3)
    f0 *= 1 + 0.015 * np.sin(2 * np.pi * rng.uniform(4.5, 6.5) * t * dur)  # a little vibrato
    src = glottal(f0) + breath * rng.normal(0, 0.08, n)
    shift = 1.15 if female else 1.0
    a = formants(src, vowel_from, shift)
    b = formants(src, vowel_to, shift) if vowel_to != vowel_from else a
    glide = np.clip((t - 0.1) / 0.35, 0, 1)
    out = a * (1 - glide) + b * glide
    out *= envelope(n, rng.uniform(0.03, 0.12), rng.uniform(0.15, 0.4))
    return out * loud / (np.max(np.abs(out)) + 1e-9)


def pink(n):
    """Pink-ish noise (Paul Kellet's filter)."""
    w = rng.normal(0, 1, n)
    b = [0.049922035, -0.095993537, 0.050612699, -0.004408786]
    a = [1, -2.494956002, 2.017265875, -0.522189400]
    return lfilter(b, a, w)


def band(x, lo, hi, order=2):
    sos = butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band", output="sos")
    return sosfilt(sos, x)


def lowpass(x, f, order=2):
    sos = butter(order, f / (SR / 2), btype="low", output="sos")
    return sosfilt(sos, x)


def place(mix, sig, start, pan, dist):
    """Adds a mono signal into the stereo mix at start (s), panned (-1..1), duller and quieter further off."""
    s = int(start * SR)
    if s >= mix.shape[1]:
        return
    sig = lowpass(sig, 9000 - 6000 * dist) * (1.0 - 0.6 * dist)
    e = min(mix.shape[1], s + len(sig))
    sig = sig[: e - s]
    left, right = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    mix[0, s:e] += sig * left
    mix[1, s:e] += sig * right


def reverb(mix, rt60=2.4, wet=0.45, pre=0.03):
    """A stadium: early echoes off the stands, then a long tail."""
    n = int(rt60 * 1.2 * SR)
    t = np.arange(n) / SR
    out = np.zeros_like(mix)
    for ch in range(2):
        ir = rng.normal(0, 1, n) * np.exp(-6.9 * t / rt60)
        ir = lowpass(ir, 5000)
        ir[: int(pre * SR)] = 0
        for _ in range(14):
            k = int(rng.uniform(0.02, 0.25) * SR)
            ir[k] += rng.uniform(0.4, 1.2) * (1 if rng.random() < 0.5 else -1)
        ir /= np.sqrt(np.sum(ir ** 2))
        out[ch] = fftconvolve(mix[ch], ir)[: mix.shape[1]]
    return mix * (1 - wet) + out * wet * 1.4


def finish(mix, peak=0.89):
    mix = mix - np.mean(mix, axis=1, keepdims=True)
    mix /= np.max(np.abs(mix)) + 1e-9
    mix = np.tanh(mix * 1.6) / np.tanh(1.6)
    return mix * peak


def crowd_shouts(mix, count, t0, spread, dur_range, kind="cheer", loud=1.0):
    """Many voices shouting together, starting within spread seconds of t0."""
    for _ in range(count):
        female = rng.random() < 0.4
        base = rng.uniform(190, 260) if female else rng.uniform(115, 165)
        if kind == "cheer":
            words = [("eh", "ah"), ("ee", "ah"), ("oo", "oo"), ("ae", "ae"), ("eh", "eh"), ("oh", "oh")]
            v0, v1 = words[rng.integers(len(words))]
            start, peak, end = base * 1.5, base * rng.uniform(2.0, 2.6), base * rng.uniform(1.3, 1.8)
        elif kind == "ooh":
            v0, v1 = ("oo", "oh")
            start, peak, end = base * 1.6, base * 1.9, base * 1.1
        elif kind == "aww":
            v0, v1 = ("ah", "uh")
            start, peak, end = base * 1.7, base * 1.75, base * 1.05
        else:  # roar: open throats, held
            v0, v1 = ("ah", "ah") if rng.random() < 0.6 else ("eh", "ah")
            start, peak, end = base * 1.6, base * rng.uniform(2.1, 2.7), base * 1.9
        dur = rng.uniform(*dur_range)
        v = voice(dur, start, peak, end, v0, v1, loud=rng.uniform(0.4, 1.0) * loud, female=female)
        dist = rng.uniform(0, 1) ** 0.7
        place(mix, v, t0 + rng.uniform(0, spread) ** 1.5 / max(spread, 1e-6) ** 0.5, rng.uniform(-1, 1), dist)


def roar_bed(mix, t0, dur, level, attack=0.15, release=0.8):
    """The crowd's roar under the voices: band-limited noise with the cheer's shape, different in each ear."""
    n = int(dur * SR)
    env = envelope(n, attack, release) * level
    s = int(t0 * SR)
    e = min(mix.shape[1], s + n)
    for ch in range(2):
        noise = band(pink(n), 250, 3200)
        noise *= env / (np.max(np.abs(noise)) + 1e-9)
        mix[ch, s:e] += noise[: e - s] * 0.9


def claps(mix, count, t0, dur, rate=(3.5, 6.0), level=1.0, ramp=0.4, fade=1.5):
    """Applause: count people clapping for dur seconds."""
    for _ in range(count):
        hz = rng.uniform(*rate)
        pan, dist = rng.uniform(-1, 1), rng.uniform(0, 1) ** 0.6
        centre, q = rng.uniform(900, 2600), rng.uniform(1.2, 2.5)
        loud = rng.uniform(0.3, 1.0) * level
        t = t0 + rng.uniform(0, ramp)
        stop = t0 + dur - rng.uniform(0, fade)
        clap_len = int(0.05 * SR)
        while t < stop:
            burst = rng.normal(0, 1, clap_len) * np.exp(-np.arange(clap_len) / (SR * rng.uniform(0.005, 0.014)))
            burst = band(burst, centre / q, min(SR / 2 - 100, centre * q))
            fall = min(1.0, (stop - t) / max(fade, 0.1)) if fade > 0 else 1.0
            place(mix, burst * loud * fall, t, pan, dist)
            t += 1 / hz * rng.uniform(0.88, 1.12)


def whistles(mix, count, t0, spread):
    for _ in range(count):
        dur = rng.uniform(0.5, 1.1)
        n = int(dur * SR)
        t = np.linspace(0, 1, n)
        f_lo, f_hi = rng.uniform(1500, 2000), rng.uniform(2600, 3300)
        if rng.random() < 0.5:  # wolf whistle: up, then up and down
            f = np.where(t < 0.4, f_lo + (f_hi - f_lo) * t / 0.4, f_lo + (f_hi - f_lo) * np.sin((t - 0.4) / 0.6 * np.pi))
        else:  # a long rising whistle
            f = f_lo + (f_hi - f_lo) * t ** 0.6
        f *= 1 + 0.01 * np.sin(2 * np.pi * 6 * t * dur)
        sig = np.sin(2 * np.pi * np.cumsum(f) / SR) * envelope(n, 0.03, 0.15) * rng.uniform(0.2, 0.45)
        sig += rng.normal(0, 0.01, n) * envelope(n, 0.03, 0.15)
        place(mix, sig, t0 + rng.uniform(0, spread), rng.uniform(-1, 1), rng.uniform(0.1, 0.8))


def chatter(mix, voices, length):
    """The murmur: people talking, syllable by syllable, all over the stands."""
    for _ in range(voices):
        female = rng.random() < 0.45
        base = rng.uniform(170, 230) if female else rng.uniform(95, 140)
        pan, dist = rng.uniform(-1, 1), rng.uniform(0.2, 1) ** 0.6
        t = rng.uniform(-1, 1)
        while t < length:
            # A phrase of 3 to 9 syllables, then a pause.
            for _ in range(rng.integers(3, 10)):
                d = rng.uniform(0.09, 0.22)
                n = int(d * SR)
                f0 = base * rng.uniform(0.9, 1.25) * np.linspace(1, rng.uniform(0.85, 1.1), n)
                src = glottal(f0) + rng.normal(0, 0.02, n)
                vowel = list(VOWELS)[rng.integers(len(VOWELS))]
                syl = formants(src, vowel, 1.12 if female else 1.0) * envelope(n, 0.02, 0.05)
                syl /= np.max(np.abs(syl)) + 1e-9
                if t >= 0:
                    place(mix, syl * rng.uniform(0.25, 0.6), t, pan, dist)
                t += d + rng.uniform(0.0, 0.06)
            t += rng.uniform(0.3, 1.6)


def render(length):
    return np.zeros((2, int(length * SR)))


def write(path, mix):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = (np.clip(mix.T, -1, 1) * 32767).astype("<i2").tobytes()
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "2", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "4", path], input=raw, check=True)


def make_murmur(out):
    # Render longer than the loop, then fold the end over the start so it loops without a seam.
    loop, fold = 14.0, 2.0
    mix = render(loop + fold)
    chatter(mix, 140, loop + fold)
    n = mix.shape[1]
    noise = band(pink(n), 200, 2500)
    noise /= np.max(np.abs(noise)) + 1e-9
    mix += np.stack([noise, np.roll(noise, 5000)]) * 0.06
    mix = reverb(mix, rt60=2.6, wet=0.6)
    L, F = int(loop * SR), int(fold * SR)
    fade = np.linspace(0, 1, F)
    out_mix = mix[:, :L].copy()
    out_mix[:, :F] = mix[:, :F] * fade + mix[:, L:L + F] * (1 - fade)
    write(os.path.join(out, "murmur.ogg"), finish(out_mix, 0.7))


def make_cheer(out, name, count, length, dur_range, bed, claps_n=0, loud=1.0):
    mix = render(length)
    crowd_shouts(mix, count, 0.02, 0.35, dur_range, "cheer", loud)
    roar_bed(mix, 0.0, length * 0.8, bed, 0.12, length * 0.45)
    if claps_n:
        claps(mix, claps_n, 0.2, length - 0.3, level=0.5)
    write(os.path.join(out, name + ".ogg"), finish(reverb(mix, 2.4, 0.42)))


def make_roar(out):
    length = 3.8
    mix = render(length)
    crowd_shouts(mix, 260, 0.0, 0.25, (1.2, 2.6), "roar")
    crowd_shouts(mix, 80, 0.2, 0.6, (0.5, 1.0), "cheer")
    roar_bed(mix, 0.0, length * 0.9, 0.9, 0.1, 1.4)
    write(os.path.join(out, "roar.ogg"), finish(reverb(mix, 2.6, 0.45)))


def make_uproar(out):
    length = 8.0
    mix = render(length)
    crowd_shouts(mix, 280, 0.0, 0.3, (1.4, 3.0), "roar")
    for wave in (1.6, 3.2, 4.6):
        crowd_shouts(mix, 120, wave, 0.6, (0.5, 1.4), "cheer", 0.85)
    roar_bed(mix, 0.0, length, 1.0, 0.1, 2.5)
    claps(mix, 260, 0.8, length - 0.8, level=0.8, fade=2.0)
    whistles(mix, 14, 0.6, 5.0)
    write(os.path.join(out, "uproar.ogg"), finish(reverb(mix, 2.8, 0.45)))


def make_applause(out, name, count, length, level):
    mix = render(length)
    claps(mix, count, 0.0, length, level=level, fade=2.5)
    write(os.path.join(out, name + ".ogg"), finish(reverb(mix, 2.4, 0.4), 0.8))


def make_reaction(out, name, kind, count, length):
    mix = render(length)
    crowd_shouts(mix, count, 0.0, 0.2, (0.8, 1.6) if kind == "ooh" else (1.2, 2.0), kind)
    roar_bed(mix, 0.0, length * 0.7, 0.25, 0.1, length * 0.4)
    write(os.path.join(out, name + ".ogg"), finish(reverb(mix, 2.4, 0.45), 0.8))


def main():
    out = os.path.join(sys.argv[1], "crowd")
    make_murmur(out)
    for i in (1, 2, 3):
        make_cheer(out, "cheer_small_%d" % i, 60, 2.0, (0.35, 0.8), 0.25)
        make_cheer(out, "cheer_%d" % i, 170, 3.2, (0.5, 1.6), 0.55, claps_n=40)
    make_roar(out)
    make_uproar(out)
    make_applause(out, "applause", 220, 6.5, 0.8)
    make_applause(out, "applause_light", 45, 4.0, 0.5)
    for i in (1, 2):
        make_reaction(out, "ooh_%d" % i, "ooh", 120, 2.2)
    make_reaction(out, "aww", "aww", 150, 3.0)


if __name__ == "__main__":
    main()
