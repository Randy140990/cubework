"""The shooting star's chime for the opening (as the handheld games' logo star had): a falling, bell-like
sweep with sparkles, written to sounds/sfx/sfx_star.ogg."""
import math, random, struct, subprocess, wave, os, tempfile

RATE = 44100
random.seed(7)
n = int(RATE * 1.6)
out = [0.0] * n
# The sweep: bell partials falling from high to mid as the star crosses the sky.
for i in range(n):
    t = i / RATE
    if t < 1.05:
        f = 2600 * (1 - t / 1.05) ** 1.4 + 780
        env = min(1, t / 0.02) * (1 - t / 1.05) ** 0.6
        ph = 2 * math.pi * f * t
        out[i] += env * (0.35 * math.sin(ph) + 0.12 * math.sin(2.76 * ph) + 0.06 * math.sin(5.4 * ph))
# Sparkles: short high pings scattered along it, and a little shower at the landing.
for k in range(46):
    at = random.uniform(0.05, 1.0) if k < 30 else random.uniform(1.0, 1.45)
    f = random.uniform(3200, 6800)
    for j in range(int(RATE * 0.09)):
        i = int(at * RATE) + j
        if i >= n:
            break
        tt = j / RATE
        out[i] += 0.07 * math.exp(-tt * 45) * math.sin(2 * math.pi * f * tt)
peak = max(abs(v) for v in out) or 1
tmp = tempfile.mktemp(suffix=".wav")
with wave.open(tmp, "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(RATE)
    w.writeframes(b"".join(struct.pack("<h", int(v / peak * 0.85 * 32767)) for v in out))
dst = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/retromon/sounds/sfx/sfx_star.ogg")
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "5", dst], check=True)
os.remove(tmp)
print("wrote", os.path.normpath(dst))
