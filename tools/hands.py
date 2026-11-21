#!/usr/bin/env python3
"""
Hands for everyone: the bottom two pixels of every arm in the person's own skin colour, as Red's are, so whatever
they wear (Oak's white coat, Blue's jacket) their hands show at the ends of their sleeves.

For each skin in textures/entity/npc (64 x 64, the classic 4-pixel arms): the arms' last two rows all the way round
and their undersides take the skin colour (read from the face's cheeks), and the sleeve layer over them is cleared
there. The outfit tops (textures/outfit/top_*) are cleared there too, so the body's skin shows. Run after any of the
skin painters (skins.py, heroes.py, crowd.py call it); running it again changes nothing.
"""
import collections
import glob
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "textures")

# The arms in the skin layout: (x of the part, y of the part), 4 wide, 12 tall, 4 deep.
ARMS = [(40, 16), (32, 48)]          # right arm, left arm
SLEEVES = [(40, 32), (48, 48)]       # their outer layers


def rows(ox, oy):
    """The pixels of a part's last two rows all the way round, and of its underside."""
    out = [(x, y) for x in range(ox, ox + 16) for y in (oy + 4 + 10, oy + 4 + 11)]
    out += [(x, y) for x in range(ox + 8, ox + 12) for y in range(oy, oy + 4)]
    return out


def skin_colour(p):
    """The face's skin: the commonest warm, mid-light colour on the cheeks (front of the head, under the eyes), else
    on the sides of the head; None if there's no skin to see (a mask, a helmet)."""
    for area in ((8, 9, 16, 15), (0, 10, 8, 16), (16, 10, 24, 16)):
        c = collections.Counter()
        for y in range(area[1], area[3]):
            for x in range(area[0], area[2]):
                r, g, b, a = p[x, y]
                if a and r >= g >= b and r - b > 25 and 80 < r and r + g + b < 690:
                    c[(r, g, b, 255)] += 1
        if c:
            return c.most_common(1)[0][0]
    return None


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)


def npc(path):
    im = Image.open(path).convert("RGBA")
    if im.size != (64, 64):
        return False
    p = im.load()
    sk = skin_colour(p)
    if sk is None:
        return False
    before = im.tobytes()
    for ox, oy in ARMS:
        for x, y in rows(ox, oy):
            # (The very last row a touch darker, as the underside of a hand is.)
            p[x, y] = shade(sk, 0.92) if y == oy + 15 else sk
    for ox, oy in SLEEVES:
        for x, y in rows(ox, oy):
            p[x, y] = (0, 0, 0, 0)
    if im.tobytes() == before:
        return False
    im.save(path)
    return True


def top(path):
    """An outfit's top: clear its sleeves' last two rows (the body's skin under them is the hand)."""
    im = Image.open(path).convert("RGBA")
    p = im.load()
    before = im.tobytes()
    for ox, oy in ARMS + SLEEVES:
        for x, y in rows(ox, oy):
            p[x, y] = (0, 0, 0, 0)
    if im.tobytes() == before:
        return False
    im.save(path)
    return True


def main():
    done = [os.path.basename(f) for f in sorted(glob.glob(os.path.join(ROOT, "entity", "npc", "*.png"))) if npc(f)]
    done += [os.path.basename(f) for f in sorted(glob.glob(os.path.join(ROOT, "outfit", "top_*.png"))) if top(f)]
    print("hands:", len(done), "changed")
    return done


if __name__ == "__main__":
    main()
