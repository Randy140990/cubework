#!/usr/bin/env python3
"""
Plays Pokémon Crystal's Johto music, note for note, from pret's disassembly (pokecrystal/audio/music), as
tools/music.py plays Yellow's: the same Game Boy channels, with Crystal's own engine where it differs (its notes
from a longer frequency table, its transpositions and pitch offsets, its volume envelopes set apart from the note
type, its drum kits). Each song is rendered to an Ogg for Retromon: the part that loops, and the intro before it.

Usage: music_gsc.py <pokecrystal folder> <output folder> [song ...]
Songs are named as Retromon's places name them (data/places.tsv: new_bark_town, route_29, ...); with none given,
every Johto place's song that has no Ogg yet.
"""
import os, re, sys

import numpy as np

import music as M

num = M.num
_read = M.read


def _read_gsc(path):
    """A file's text, its local labels all with their colons (a few of Crystal's are written without)."""
    return re.sub(r"(?m)^(\.\w+)[ \t]*$", r"\1:", _read(path))


M.read = _read_gsc


def load_table(folder):
    """Crystal's FrequencyTable: index 0 a rest, 1 to 12 C_ to B_, then the octave above (for transpositions)."""
    text = M.read(os.path.join(folder, "audio/notes.asm"))
    body = text.split("FrequencyTable:", 1)[1]
    vals = []
    for m in re.finditer(r"dw\s+(\$[0-9A-Fa-f]+|\d+)", body):
        vals.append(num(m.group(1)))
    return vals


def load_kits(folder):
    """The drum kits: for each, its instruments' noise notes (Drumkits -> DrumkitN -> DrumXX -> noise_note ...)."""
    text = M.read(os.path.join(folder, "audio/drumkits.asm"))
    labels, cur = {}, None
    for raw in text.splitlines():
        line = raw.split(";")[0].rstrip()
        m = re.match(r"^(\w+):", line)
        if m:
            cur = m.group(1)
            labels[cur] = []
            continue
        if cur and line.strip():
            labels[cur].append(line.strip())
    kits = []
    for l in labels.get("Drumkits", []):
        m = re.match(r"dw (\w+)", l)
        if not m: continue
        inst = []
        for d in labels.get(m.group(1), []):
            dm = re.match(r"dw (\w+)", d)
            if not dm: continue
            notes = []
            for n in labels.get(dm.group(1), []):
                nm = re.match(r"noise_note (\d+), (\d+), (-?\d+), (\d+)", n)
                if nm: notes.append(tuple(int(x) for x in nm.groups()))
            inst.append(notes)
        kits.append(inst)
    return kits


TABLE, KITS = [], []


def load_waves(folder):
    """Crystal's wave shapes: each a line of 32 nybbles."""
    text = M.read(os.path.join(folder, "audio/wave_samples.asm"))
    waves = []
    for line in text.splitlines():
        line = line.split(";")[0].strip()
        if line.startswith("dn "):
            vals = [int(v) for v in line[3:].split(",")]
            if len(vals) == 32: waves.append(vals)
    return waves


def frequency(c, pitch):
    """Crystal's GetFrequency: the pitch and octave moved by the channel's transposition, shifted down 7 - octave."""
    th, tl = getattr(c, "trans", (0, 0))
    v = TABLE[min(len(TABLE) - 1, pitch + tl)]
    if v >= 0x8000: v -= 0x10000
    octv = ((8 - c.octave) & 7) + th
    while octv < 7:
        v >>= 1
        octv += 1
    f = v & 0x7FF
    return (f + getattr(c, "poff", 0)) & 0x7FF


PITCH = {k: v + 1 for k, v in M.PITCHES.items()}


def step(c, tempo, pitches, drums, frame, first_loop_hits):
    """One channel's commands until its next note or rest (music.step with Crystal's differences)."""
    song = c.song
    guard = 0
    while True:
        guard += 1
        if guard > 3000 or c.pc >= len(song.cmds):
            c.ended = True
            c.on = False
            return
        op, a = song.cmds[c.pc]
        c.pc += 1
        if op == "tempo":
            tempo[0] = num(a[0])
            c.frac = 0
        elif op in ("volume", "stereo_panning", "force_stereo_panning", "channel_count", "channel", "db", "pitch_sweep", "unknownmusic0xe7",
                    "unknownmusic0xde", "sound_priority", "sfx_priority_on", "sfx_priority_off", "set_condition"):
            pass
        elif op == "vibrato":
            c.vib_delay, c.vib_depth, c.vib_rate = num(a[0]), num(a[1]), num(a[2]) if len(a) > 2 else 0
        elif op == "duty_cycle":
            c.duty = num(a[0]) & 3
            c.duty_pattern = None
        elif op == "duty_cycle_pattern":
            c.duty_pattern = [num(x) & 3 for x in a]
        elif op in ("toggle_perfect_pitch", "pitch_inc_switch"):
            c.perfect = not c.perfect
        elif op == "note_type":
            c.speed = num(a[0])
            if len(a) > 2:
                c.volume = num(a[1])
                if c.index == 3:
                    c.wave = num(a[2])
                else:
                    f = num(a[2])
                    c.fade = f if f >= 0 else (8 | (-f & 7))
        elif op == "volume_envelope":
            c.volume = num(a[0])
            f = num(a[1])
            if c.index == 3:
                c.wave = f
            else:
                c.fade = f if f >= 0 else (8 | (-f & 7))
        elif op == "drum_speed":
            c.drum_speed = num(a[0])
        elif op == "octave":
            c.octave = num(a[0])
        elif op == "transpose":
            c.trans = (num(a[0]), num(a[1]))
        elif op == "pitch_offset":
            c.poff = num(a[0])
        elif op == "toggle_noise":
            c.kit = num(a[0]) if a else None
        elif op == "pitch_slide":
            c.slide = (num(a[0]), num(a[1]), M.PITCHES.get(a[2], 0))
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
        elif op in ("sound_loop", "sound_jump"):
            count, target = (0, song.labels[a[0]]) if op == "sound_jump" else (num(a[0]), song.labels[a[1]])
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
                length, speed = num(a[1]), c.drum_speed
            else:
                length = num(a[1]) if op == "note" else num(a[0])
                speed = c.speed if c.index != 4 else c.drum_speed
            total = length * speed * tempo[0] + c.frac
            c.wait = max(1, total >> 8)
            c.frac = total & 0xFF
            if op == "rest":
                if c.index != 4: c.on = False
            elif op == "drum_note":
                kit = KITS[c.kit] if getattr(c, "kit", None) is not None and c.kit < len(KITS) else (KITS[0] if KITS else [])
                inst = num(a[0])
                c.drum = list(kit[inst]) if inst < len(kit) else []
                c.drum_wait = 0
                c.on = bool(c.drum)
            else:
                f = frequency(c, PITCH[a[0]])
                if c.perfect: f += 1
                M.start_freq(c, pitches, f)
            return


def song_headers(folder):
    """Each song's channels, from its own file's header (Music_X: channel_count n / channel i, label)."""
    songs, files = {}, []
    mdir = os.path.join(folder, "audio/music")
    for f in sorted(os.listdir(mdir)):
        if not f.endswith(".asm"): continue
        path = os.path.join(mdir, f)
        files.append(path)
        text = M.read(path)
        for m in re.finditer(r"^Music_(\w+):\s*\n\s*channel_count \d+\s*\n((?:\s*channel \d, \w+\s*\n?)+)", text, re.M):
            songs[m.group(1)] = dict((int(n), l) for n, l in re.findall(r"channel (\d), (\w+)", m.group(2)))
    return songs, files


def key_of(label):
    return re.sub(r"[^a-z0-9]", "", label.lower())


def johto_songs(places):
    names = []
    for line in open(places, encoding="utf-8"):
        if line.startswith("#") or not line.strip(): continue
        r = line.rstrip("\n").split("\t")
        if len(r) > 3 and r[3] not in names: names.append(r[3])
    return names


def main():
    global TABLE, KITS
    folder, out = sys.argv[1], sys.argv[2]
    only = sys.argv[3:]
    os.makedirs(out, exist_ok=True)
    TABLE, KITS = load_table(folder), load_kits(folder)
    M.step = step
    pitches, waves = TABLE[1:13], load_waves(folder)
    songs, files = song_headers(folder)
    song = M.Song(files)
    by_key = {key_of(n): n for n in songs}
    if not only:
        here = os.path.dirname(os.path.abspath(__file__))
        only = [n for n in johto_songs(os.path.join(here, "..", "src", "main", "resources", "retromon", "data", "places.tsv"))
                if not os.path.exists(os.path.join(out, n + ".ogg")) and key_of(n) in by_key]
    for sid in only:
        name = by_key.get(key_of(sid))
        if not name:
            print("no song for", sid)
            continue
        chans = songs[name]
        labels = [chans.get(i) for i in (1, 2, 3, 4)]
        labels = [l if l and l in song.labels else None for l in labels]
        loops, total = M.measure(song, labels, pitches, None)
        if loops:
            start, length = M.common_loop(loops)
            frames = start + length
        else:
            frames = total
        audio = M.finish(M.render_frames(song, labels, pitches, waves, None, frames))
        cut = lambda fr: int(round(fr * M.SAMPLES_PER_FRAME))
        if loops:
            M.write_ogg(audio[cut(start):cut(start + length)], os.path.join(out, sid + ".ogg"))
            if start > M.FRAME_HZ / 4:
                M.write_ogg(audio[:cut(start)], os.path.join(out, sid + "_intro.ogg"))
            print(sid, "loop %.1fs, intro %.1fs" % (length / M.FRAME_HZ, start / M.FRAME_HZ), flush=True)
        else:
            M.write_ogg(audio, os.path.join(out, sid + ".ogg"))
            print(sid, "once %.1fs" % (len(audio) / M.RATE), flush=True)


if __name__ == "__main__":
    main()
