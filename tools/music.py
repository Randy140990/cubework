#!/usr/bin/env python3
"""
Plays Pokémon Yellow's music, note for note, from pret's disassembly (pokeyellow/audio/music), the way the
Game Boy's sound chip does: two square-wave channels with their duty cycles and volume envelopes, the wave
channel with the game's own wave shapes, and the noise channel playing the game's drum kit. Each song is
rendered to an Ogg file for Retromon: the part that loops (so it can repeat seamlessly) and, when there is
one, the intro before it.

Usage: music.py <pokeyellow folder> <output folder> [song ...]
"""
import os, re, subprocess, sys, tempfile, wave

import numpy as np

RATE = 32768
FRAME_HZ = 4194304 / 70224  # the Game Boy's frame rate, about 59.73
SAMPLES_PER_FRAME = RATE / FRAME_HZ
PITCHES = {"C_": 0, "C#": 1, "D_": 2, "D#": 3, "E_": 4, "F_": 5, "F#": 6, "G_": 7, "G#": 8, "A_": 9, "A#": 10, "B_": 11}
DUTY = [0.125, 0.25, 0.5, 0.75]


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


class Song:
    """A music file's commands, with its labels (globals, and .locals under them)."""

    def __init__(self, files):
        self.cmds = []
        self.labels = {}
        glob = ""
        for path in files:
            for raw in read(path).splitlines():
                line = raw.split(";")[0].rstrip()
                if not line.strip():
                    continue
                m = re.match(r"^(\.?\w+)::?\s*$", line.strip()) if not raw.startswith("\t") or raw.strip().endswith(":") else None
                if m and not raw.startswith("\t"):
                    name = m.group(1)
                    if name.startswith("."):
                        self.labels[glob + name] = len(self.cmds)
                    else:
                        glob = name
                        self.labels[name] = len(self.cmds)
                    continue
                parts = line.strip().split(None, 1)
                op = parts[0]
                args = [a.strip() for a in parts[1].split(",")] if len(parts) > 1 else []
                # Labels in arguments are local to the global they're written under.
                args = [glob + a if a.startswith(".") else a for a in args]
                self.cmds.append((op, args))


def num(s):
    s = s.strip()
    if s.startswith("$"):
        return int(s[1:], 16)
    if s.startswith("%"):
        return int(s[1:], 2)
    return int(s)


def load_pitches(folder):
    vals = re.findall(r"dw \$([0-9A-Fa-f]{4})", read(os.path.join(folder, "audio/notes.asm")))
    return [int(v, 16) for v in vals[:12]]


def load_waves(folder):
    text = read(os.path.join(folder, "audio/wave_samples.asm"))
    waves = []
    for m in re.finditer(r"\.wave\d\s*\n\s*dn ([^\n]+)", text):
        waves.append([int(x) for x in m.group(1).split(",")])
    return waves


def load_drums(folder):
    drums = {}
    for i in range(1, 20):
        path = os.path.join(folder, f"audio/sfx/noise_instrument{i:02d}_1.asm")
        if not os.path.exists(path):
            continue
        notes = []
        for l in read(path).splitlines():
            m = re.match(r"\s*noise_note (\d+), (\d+), (-?\d+), (\d+)", l)
            if m:
                notes.append(tuple(int(x) for x in m.groups()))
        drums[i] = notes
    return drums


def frequency(pitches, pitch, octave):
    v = pitches[pitch]
    if v >= 0x8000:
        v -= 0x10000
    v >>= (octave - 1)
    return (v + 0x800) & 0x7FF


class Channel:
    def __init__(self, song, index, start):
        self.song = song
        self.index = index  # 1 to 4
        self.pc = start
        self.stack = []
        self.loops = {}
        self.wait = 1
        self.frac = 0
        self.speed = 12
        self.volume = 0
        self.fade = 0
        self.octave = 4
        self.duty = 2
        self.duty_pattern = None
        self.perfect = False
        self.vib_delay = 0
        self.vib_depth = 0
        self.vib_rate = 0
        self.slide = None
        self.wave = 0
        self.drum_speed = 12
        # What's sounding now.
        self.on = False
        self.freq = 0
        self.base_freq = 0
        self.env_vol = 0
        self.env_dir = 0
        self.env_period = 0
        self.env_clock = 0.0
        self.phase = 0.0
        self.note_frames = 0
        self.slide_to = None
        self.slide_frames = 0
        self.vib_count = 0
        self.vib_up = True
        self.drum = []
        self.drum_wait = 0
        self.lfsr = 0x7FFF
        self.noise_width7 = False
        self.noise_rate = 0.0
        self.noise_acc = 0.0
        self.ended = False
        self.loop_start_frame = None
        self.loop_label = None
        self.loop_end_frame = None
        self.jumps = []


def render(song, entry_labels, pitches, waves, drums, max_seconds=420):
    tempo = [256]
    chans = [Channel(song, i + 1, song.labels[l]) for i, l in enumerate(entry_labels) if l]
    # Where each channel's endless loop (sound_loop 0) goes back to, to find the loop.
    for c in chans:
        pass
    out = []
    frame = 0
    first_loop_hits = {}
    max_frames = int(max_seconds * FRAME_HZ)
    while frame < max_frames:
        for c in chans:
            if c.ended:
                continue
            c.wait -= 1
            if c.wait <= 0:
                step(c, tempo, pitches, drums, frame, first_loop_hits)
        buf = np.zeros(int(round((frame + 1) * SAMPLES_PER_FRAME)) - int(round(frame * SAMPLES_PER_FRAME)), dtype=np.float64)
        for c in chans:
            synth(c, buf, waves)
        out.append(buf)
        frame += 1
        if all(c.ended for c in chans):
            break
        # Every channel has gone round its endless loop once (and the song's had time to settle): done.
        if chans and all(c.loop_end_frame is not None or c.ended for c in chans) and any(c.loop_end_frame is not None for c in chans):
            break
    audio = np.concatenate(out) if out else np.zeros(1)
    starts = [c.loop_start_frame for c in chans if c.loop_end_frame is not None]
    ends = [c.loop_end_frame for c in chans if c.loop_end_frame is not None]
    return audio, starts, ends


def render_frames(song, labels, pitches, waves, drums, frames):
    tempo = [256]
    chans = [Channel(song, i + 1, song.labels[l]) for i, l in enumerate(labels) if l]
    out = []
    for frame in range(frames):
        for c in chans:
            if c.ended:
                continue
            c.wait -= 1
            if c.wait <= 0:
                step(c, tempo, pitches, drums, frame, {})
        buf = np.zeros(int(round((frame + 1) * SAMPLES_PER_FRAME)) - int(round(frame * SAMPLES_PER_FRAME)), dtype=np.float64)
        for c in chans:
            synth(c, buf, waves)
        out.append(buf)
    return np.concatenate(out) if out else np.zeros(1)


def step(c, tempo, pitches, drums, frame, first_loop_hits):
    song = c.song
    guard = 0
    while True:
        guard += 1
        if guard > 2000 or c.pc >= len(song.cmds):
            c.ended = True
            c.on = False
            return
        op, a = song.cmds[c.pc]
        c.pc += 1
        if op == "tempo":
            tempo[0] = num(a[0])
            c.frac = 0
        elif op in ("volume", "stereo_panning", "unknownmusic0xef", "execute_music", "channel_count", "channel", "pitch_sweep"):
            pass
        elif op == "vibrato":
            c.vib_delay, c.vib_depth, c.vib_rate = num(a[0]), num(a[1]), num(a[2])
        elif op == "duty_cycle":
            c.duty = num(a[0])
            c.duty_pattern = None
        elif op == "duty_cycle_pattern":
            c.duty_pattern = [num(x) for x in a]
        elif op == "toggle_perfect_pitch":
            c.perfect = not c.perfect
        elif op == "note_type":
            c.speed = num(a[0])
            if len(a) > 1:
                c.volume = num(a[1])
                if c.index == 3:
                    c.wave = num(a[2]) if len(a) > 2 else 0
                else:
                    f = num(a[2]) if len(a) > 2 else 0
                    c.fade = f
        elif op == "drum_speed":
            c.drum_speed = num(a[0])
        elif op == "octave":
            c.octave = num(a[0])
        elif op == "pitch_slide":
            c.slide = (num(a[0]), num(a[1]), PITCHES[a[2]])
        elif op == "sound_call":
            c.stack.append(c.pc)
            c.pc = song.labels[a[0]]
        elif op == "sound_ret":
            if c.stack:
                c.pc = c.stack.pop()
            else:
                c.ended = True
                c.on = False
                return
        elif op == "sound_loop":
            count, target = num(a[0]), song.labels[a[1]]
            if count == 0:
                c.jumps.append(frame)
                if c.loop_end_frame is None and c.loop_label is None:
                    c.loop_label = target
                    c.loop_end_frame = frame
                c.pc = target
            else:
                n = c.loops.get(c.pc, 1)
                if n >= count:
                    c.loops[c.pc] = 1
                else:
                    c.loops[c.pc] = n + 1
                    c.pc = target
        elif op in ("note", "rest", "drum_note"):
            if op == "drum_note":
                length = num(a[1])
                speed = c.drum_speed
            else:
                length = num(a[1]) if op == "note" else num(a[0])
                speed = c.speed if c.index != 4 else c.drum_speed
            total = length * speed * tempo[0] + c.frac
            c.wait = max(1, total >> 8)
            c.frac = total & 0xFF
            if op == "rest":
                if c.index != 4:
                    c.on = False
            elif op == "drum_note":
                inst = num(a[0])
                c.drum = list(drums.get(inst, []))
                c.drum_wait = 0
                c.on = True
            else:
                start_note(c, pitches, PITCHES[a[0]])
            return
        else:
            pass


def start_note(c, pitches, pitch):
    f = frequency(pitches, pitch, c.octave)
    if c.perfect:
        f += 1
    c.base_freq = f
    c.freq = f
    c.on = c.volume > 0 or c.index == 3 and c.volume > 0
    if c.index != 3:
        c.on = True
        c.env_vol = c.volume
        c.env_dir = 1 if (c.fade & 8) else -1
        c.env_period = c.fade & 7
        c.env_clock = 0.0
    c.note_frames = 0
    c.vib_count = 0
    c.vib_up = True
    c.slide_to = None
    if c.slide:
        length, octave, tp = c.slide
        target = frequency(pitches, tp, 8 - octave if octave <= 8 else 1)
        c.slide_to = target
        c.slide_frames = max(1, c.wait)
        c.slide_from = f
        c.slide = None


def gb_hz(c, f):
    if c.index == 3:
        return 65536.0 / (2048 - f)
    return 131072.0 / (2048 - f)


def synth(c, buf, waves):
    n = len(buf)
    if c.index == 4:
        synth_noise(c, buf)
        return
    if not c.on:
        return
    # Vibrato and pitch slides change the pitch frame by frame.
    c.note_frames += 1
    f = c.base_freq
    if c.slide_to is not None:
        t = min(1.0, c.note_frames / c.slide_frames)
        f = int(round(c.slide_from + (c.slide_to - c.slide_from) * t))
    elif c.vib_depth and c.note_frames > c.vib_delay:
        c.vib_count += 1
        if c.vib_count > max(1, c.vib_rate):
            c.vib_count = 0
            c.vib_up = not c.vib_up
        up = (c.vib_depth + 1) // 2
        down = c.vib_depth // 2
        f = c.base_freq + (up if c.vib_up else -down)
    f = max(0, min(2047, f))
    hz = gb_hz(c, f)
    inc = hz / RATE
    ph = c.phase + inc * np.arange(1, n + 1)
    c.phase = float(ph[-1] % 1.0)
    ph = ph % 1.0
    if c.index == 3:
        if c.volume == 0:
            return
        level = {1: 1.0, 2: 0.5, 3: 0.25}.get(c.volume, 0.0)
        w = waves[c.wave % len(waves)] if waves else [8] * 32
        idx = (ph * 32).astype(int) % 32
        s = np.array(w, dtype=np.float64)[idx] / 15.0
        buf += (s - 0.5) * 2 * level * 0.9
        return
    duty = DUTY[c.duty_pattern[c.note_frames % 4] if c.duty_pattern else c.duty]
    s = np.where(ph < duty, 1.0, -1.0)
    vol = envelope(c, n)
    buf += s * vol / 15.0


def envelope(c, n):
    """The volume over this frame's samples, the hardware envelope stepping every period/64 of a second."""
    if c.env_period == 0:
        return np.full(n, float(c.env_vol))
    out = np.empty(n)
    step_len = c.env_period / 64.0 * RATE
    i = 0
    while i < n:
        left = step_len - c.env_clock
        take = int(min(n - i, max(1, np.ceil(left))))
        out[i:i + take] = c.env_vol
        c.env_clock += take
        i += take
        if c.env_clock >= step_len:
            c.env_clock -= step_len
            c.env_vol = max(0, min(15, c.env_vol + c.env_dir))
    return out


def synth_noise(c, buf):
    n = len(buf)
    if not c.on:
        return
    # The drum: its noise notes one after another, each so many frames long.
    if c.drum_wait <= 0:
        if not c.drum:
            c.on = False
            return
        length, vol, fade, nr43 = c.drum.pop(0)
        c.drum_wait = length + 1
        c.env_vol = vol
        c.env_dir = 1 if (fade & 8) or fade < 0 else -1
        c.env_period = abs(fade) & 7
        c.env_clock = 0.0
        shift, width, div = nr43 >> 4, (nr43 >> 3) & 1, nr43 & 7
        r = 0.5 if div == 0 else div
        c.noise_rate = 524288.0 / r / (2 ** (shift + 1))
        c.noise_width7 = width == 1
        c.lfsr = 0x7FFF
    c.drum_wait -= 1
    vol = envelope(c, n)
    steps = c.noise_rate / RATE
    out = np.empty(n)
    lfsr = c.lfsr
    acc = c.noise_acc
    for i in range(n):
        acc += steps
        while acc >= 1.0:
            acc -= 1.0
            bit = (lfsr ^ (lfsr >> 1)) & 1
            lfsr = (lfsr >> 1) | (bit << 14)
            if c.noise_width7:
                lfsr = (lfsr & ~(1 << 6)) | (bit << 6)
        out[i] = 1.0 if (lfsr & 1) == 0 else -1.0
    c.lfsr = lfsr
    c.noise_acc = acc
    buf += out * vol / 15.0 * 0.8


def measure(song, labels, pitches, drums, seconds=600):
    """Each channel's loop: when it first starts (frame) and how long it is (frames); None for a jingle."""
    tempo = [256]
    chans = [Channel(song, i + 1, song.labels[l]) for i, l in enumerate(labels) if l]
    for frame in range(int(seconds * FRAME_HZ)):
        for c in chans:
            if c.ended:
                continue
            c.wait -= 1
            if c.wait <= 0:
                step(c, tempo, pitches, drums, frame, {})
        if all(c.ended or len(c.jumps) >= 2 for c in chans):
            break
    loops = [(c.jumps[0] - (c.jumps[1] - c.jumps[0]), c.jumps[1] - c.jumps[0]) for c in chans if len(c.jumps) >= 2]
    if not loops:
        return None, frame + 1
    return loops, frame + 1


def common_loop(loops):
    """Where every channel is going round together: the latest start, and a length that fits every channel's loop."""
    from math import gcd
    start = max(st for st, _ in loops)
    periods = sorted({p for _, p in loops})
    merged = []
    for p in periods:
        if merged and abs(p - merged[-1]) <= 2:
            merged[-1] = max(merged[-1], p)
        else:
            merged.append(p)
    length = merged[0]
    for p in merged[1:]:
        length = length * p // gcd(length, p)
    if length > 240 * FRAME_HZ:
        length = max(merged)
    return start, length


def song_files(folder):
    """Each song: its name and its channels' labels, from the music headers."""
    songs = {}
    for hf in sorted(os.listdir(os.path.join(folder, "audio/headers"))):
        if not hf.startswith("musicheaders"):
            continue
        text = read(os.path.join(folder, "audio/headers", hf))
        for m in re.finditer(r"^Music_(\w+)::\s*\n\s*channel_count \d+\s*\n((?:\s*channel \d, \w+\s*\n?)+)", text, re.M):
            chans = dict((int(n), l) for n, l in re.findall(r"channel (\d), (\w+)", m.group(2)))
            songs[m.group(1)] = chans
    # A few jingles that play like music (levelling up, a catch, an item).
    jingles = {"Level_Up": "level_up", "Caught_Mon": "caught_mon", "Get_Item1_1": "get_item", "Get_Key_Item_1": "get_key_item", "Dex_Page_Added": "dex_page_added",
               "Get_Item2_1": "get_item2"}
    for hf in sorted(os.listdir(os.path.join(folder, "audio/headers"))):
        if not hf.startswith("sfxheaders"):
            continue
        text = read(os.path.join(folder, "audio/headers", hf))
        for m in re.finditer(r"^SFX_(\w+)::\s*\n\s*channel_count \d+\s*\n((?:\s*channel \d, \w+\s*\n?)+)", text, re.M):
            if m.group(1) not in jingles:
                continue
            chans = dict((int(n) - 4, l) for n, l in re.findall(r"channel (\d), (\w+)", m.group(2)))
            songs["JINGLE_" + jingles[m.group(1)]] = chans
    return songs


def finish(audio):
    # The Game Boy's output: DC removed (a gentle high-pass), mixed down, a touch of softening.
    from scipy.signal import lfilter
    a = 0.996
    y = lfilter([a, -a], [1, -a], audio)
    y = lfilter([0.6, 0.4], [1], y)
    y = y / 4.2
    return np.clip(y, -1, 1)


def write_ogg(samples, path):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as t:
        tmp = t.name
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes((samples * 32000).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "1", path], check=True)
    os.remove(tmp)


def snake(name):
    if name.startswith("JINGLE_"):
        return name[7:]
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


def main():
    folder, out = sys.argv[1], sys.argv[2]
    only = set(sys.argv[3:])
    os.makedirs(out, exist_ok=True)
    pitches, waves, drums = load_pitches(folder), load_waves(folder), load_drums(folder)
    songs = song_files(folder)
    files = [os.path.join(folder, "audio/music", f) for f in sorted(os.listdir(os.path.join(folder, "audio/music")))]
    files += [os.path.join(folder, "audio/sfx", f) for f in sorted(os.listdir(os.path.join(folder, "audio/sfx")))]
    song = Song(files)
    made = {}
    for name, chans in songs.items():
        sid = snake(name)
        if only and sid not in only:
            continue
        labels = [chans.get(i) for i in (1, 2, 3, 4)]
        if not any(l and l in song.labels for l in labels):
            continue
        labels = [l if l in song.labels else None for l in labels]
        try:
            loops, total = measure(song, labels, pitches, drums)
        except KeyError as e:
            print("skipped", sid, e)
            continue
        if loops:
            start, length = common_loop(loops)
            frames = start + length
        else:
            frames = total
        audio = finish(render_frames(song, labels, pitches, waves, drums, frames))
        cut = lambda fr: int(round(fr * SAMPLES_PER_FRAME))
        if loops:
            loop = audio[cut(start):cut(start + length)]
            write_ogg(loop, os.path.join(out, sid + ".ogg"))
            if start > FRAME_HZ / 4:
                write_ogg(audio[:cut(start)], os.path.join(out, sid + "_intro.ogg"))
            made[sid] = (round(len(loop) / RATE, 1), round(start / FRAME_HZ, 1))
        else:
            write_ogg(audio, os.path.join(out, sid + ".ogg"))
            made[sid] = (round(len(audio) / RATE, 1), "once")
        print(sid, made[sid], flush=True)


def loop_start_sample(song, labels, pitches, drums, waves):
    """When the first channel reaches the label its endless loop jumps back to (render again, stopping there)."""
    tempo = [256]
    chans = [Channel(song, i + 1, song.labels[l]) for i, l in enumerate(labels) if l]
    targets = {}
    for c in chans:
        # Find this channel's endless loop target by walking its commands.
        pc, seen = c.pc, 0
        stack = []
        while pc < len(song.cmds) and seen < 50000:
            op, a = song.cmds[pc]
            seen += 1
            if op == "sound_loop" and num(a[0]) == 0:
                targets[id(c)] = song.labels[a[1]]
                break
            if op == "sound_ret" and not stack:
                break
            pc += 1
    frame = 0
    while frame < int(420 * FRAME_HZ):
        for c in chans:
            if c.ended:
                continue
            c.wait -= 1
            if c.wait <= 0:
                if id(c) in targets and c.pc == targets[id(c)] and not c.stack:
                    return int(round(frame * SAMPLES_PER_FRAME))
                step(c, tempo, pitches, drums, frame, {})
        frame += 1
    return None


if __name__ == "__main__":
    main()
