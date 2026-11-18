"""
The wardrobe's layers: each is a 64x64 skin-shaped picture in greys (with a little shading), which the game
tints with the colour you pick and stacks into your skin: body, face, bottoms, shoes, top, hair, hat.
Saved in assets/retromon/textures/outfit/<layer>.png. The face (eyes and mouth) keeps its own colours.
"""
import os
import random

from PIL import Image

from skinbits import rect, box, band, front

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "retromon", "textures", "outfit")
W = (255, 255, 255, 255)
HEAD, BODY, RARM, LARM, RLEG, LLEG = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (32, 48, 4, 12, 4), (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)
HAT, JACKET, RSLEEVE, LSLEEVE, RPANTS, LPANTS = (32, 0, 8, 8, 8), (16, 32, 8, 12, 4), (40, 32, 4, 12, 4), (48, 48, 4, 12, 4), (0, 32, 4, 12, 4), (0, 48, 4, 12, 4)


def new():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    return im, im.load()


def shade(im, seed, amount=18):
    """A little grain, and the lower rows of each part a touch darker, so a flat colour reads as cloth or hair."""
    rnd = random.Random(seed)
    p = im.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = p[x, y]
            if a == 0:
                continue
            k = 255 - rnd.randint(0, amount) - (y % 16) // 4 * 3
            p[x, y] = (r * k // 255, g * k // 255, b * k // 255, a)
    return im


def save(name, im):
    im.save(os.path.join(OUT, name + ".png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    # The body (tinted by skin tone): head, arms and legs.
    im, p = new()
    for part in (HEAD, BODY, RARM, LARM, RLEG, LLEG):
        box(p, *part, W)
    save("body", shade(im, 1, 8))
    # The face: eyes and mouth, in their own colours.
    im, p = new()
    p[9, 12] = p[10, 12] = (255, 255, 255, 255)
    p[10, 12] = (45, 50, 80, 255)
    p[13, 12] = (45, 50, 80, 255)
    p[14, 12] = (255, 255, 255, 255)
    p[9, 11] = p[10, 11] = p[13, 11] = p[14, 11] = (70, 50, 40, 255)
    p[11, 14] = p[12, 14] = (170, 95, 85, 255)
    save("face", im)
    # Tops.
    im, p = new()
    box(p, *BODY, W)
    band(p, *RARM, 0, 4, W)
    band(p, *LARM, 0, 4, W)
    save("top_tee", shade(im, 2))
    im, p = new()
    box(p, *BODY, W)
    band(p, *RARM, 0, 10, W)
    band(p, *LARM, 0, 10, W)
    box(p, *JACKET, W)
    band(p, *RSLEEVE, 0, 10, W)
    band(p, *LSLEEVE, 0, 10, W)
    save("top_jacket", shade(im, 3))
    im, p = new()
    front(p, *JACKET, 3, 0, 5, 12, (245, 245, 245, 255))
    band(p, *JACKET, 0, 1, (245, 245, 245, 255))
    save("top_jacket_trim", im)
    im, p = new()
    box(p, *BODY, W)
    band(p, *RARM, 0, 11, W)
    band(p, *LARM, 0, 11, W)
    front(p, *BODY, 1, 7, 7, 10, (215, 215, 215, 255))
    rect(p, 56, 8, 64, 16, W)  # the hood down the back of the head
    save("top_hoodie", shade(im, 4))
    im, p = new()
    box(p, *BODY, W)
    save("top_vest", shade(im, 5))
    # Bottoms.
    im, p = new()
    box(p, *RLEG, W)
    box(p, *LLEG, W)
    band(p, *BODY, 10, 12, W)
    save("bottom_jeans", shade(im, 6))
    im, p = new()
    band(p, *RLEG, 0, 5, W)
    band(p, *LLEG, 0, 5, W)
    band(p, *BODY, 10, 12, W)
    save("bottom_shorts", shade(im, 7))
    im, p = new()
    band(p, *RLEG, 0, 4, W)
    band(p, *LLEG, 0, 4, W)
    band(p, *BODY, 9, 12, W)
    band(p, *RPANTS, 0, 5, W)
    band(p, *LPANTS, 0, 5, W)
    save("bottom_skirt", shade(im, 8))
    # Shoes.
    im, p = new()
    band(p, *RLEG, 9, 12, W)
    band(p, *LLEG, 9, 12, W)
    rect(p, 8, 16, 12, 20, W)
    rect(p, 24, 48, 28, 52, W)
    save("shoes", shade(im, 9))
    # Hair.
    def hair_base(p):
        rect(p, 8, 0, 16, 8, W)
        band(p, *HEAD, 0, 2, W)
        rect(p, 24, 8, 32, 14, W)
        rect(p, 0, 8, 8, 12, W)
        rect(p, 16, 8, 24, 12, W)
    for name, extra in (("short", None), ("spiky", "spiky"), ("long", "long"), ("ponytail", "ponytail"), ("pigtails", "pigtails"), ("bun", "bun")):
        im, p = new()
        hair_base(p)
        if extra == "spiky":
            for x in range(40, 48):
                p[x, 0 + (x % 2)] = W
            rect(p, 32, 8, 64, 9, W)
            for x in range(32, 64, 2):
                p[x, 9] = W
        elif extra == "long":
            rect(p, 24, 8, 32, 16, W)
            rect(p, 0, 8, 8, 16, W)
            rect(p, 16, 8, 24, 16, W)
            rect(p, 32, 20, 40, 26, W)
            rect(p, 56, 8, 64, 16, W)
        elif extra == "ponytail":
            rect(p, 58, 10, 62, 16, W)
            rect(p, 34, 20, 38, 24, W)
        elif extra == "pigtails":
            rect(p, 32, 10, 35, 16, W)
            rect(p, 45, 10, 48, 16, W)
            rect(p, 0, 8, 8, 14, W)
            rect(p, 16, 8, 24, 14, W)
        elif extra == "bun":
            rect(p, 58, 9, 62, 13, W)
            rect(p, 42, 0, 46, 3, W)
        save("hair_" + name, shade(im, 10 + len(name), 26))
    # Hats (on the head's outer layer).
    im, p = new()
    rect(p, 40, 0, 48, 8, W)
    rect(p, 32, 8, 64, 10, W)
    rect(p, 40, 10, 48, 11, (205, 205, 205, 255))
    save("hat_cap", shade(im, 20))
    im, p = new()
    rect(p, 40, 0, 48, 8, W)
    rect(p, 32, 8, 64, 11, W)
    for x in range(32, 64):
        p[x, 11] = (225, 225, 225, 255)
    save("hat_beanie", shade(im, 21))
    im, p = new()
    rect(p, 40, 0, 48, 8, W)
    rect(p, 32, 8, 64, 10, W)
    rect(p, 32, 10, 64, 11, (230, 230, 230, 255))
    save("hat_sunhat", shade(im, 22))
    print("outfit layers saved")


if __name__ == "__main__":
    main()
