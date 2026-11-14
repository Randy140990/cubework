"""Draws Cubework's wallpaper textures (16 x 16, seamless) into the resources. Run once after changing them."""
import os
import random
from PIL import Image

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "cubework", "textures", "block", "wallpaper")

# Colourways: (name, base colour); the pattern is drawn a little darker and lighter than the base.
COLOURWAYS = [("yellow", (201, 187, 112)), ("cream", (226, 215, 182)), ("sage", (168, 178, 138)), ("rose", (204, 160, 150))]


def shade(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def draw(base, pattern, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    dark, light = shade(base, 0.86), shade(base, 1.07)
    for y in range(16):
        for x in range(16):
            c = base
            if pattern == "diamond":
                # A lattice of thin diagonal lines every 8 pixels, a small light dot in each diamond.
                if (x + y) % 8 == 0 or (x - y) % 8 == 0:
                    c = dark
                elif (x + y) % 8 == 4 and (x - y) % 8 == 4:
                    c = light
            else:
                # Stripes: a 2-pixel darker band and a 1-pixel light line, every 8 pixels.
                if x % 8 in (0, 1):
                    c = dark
                elif x % 8 == 4:
                    c = light
            n = 1 + rnd.uniform(-0.025, 0.025)
            img.putpixel((x, y), shade(c, n) + (255,))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (name, base) in enumerate(COLOURWAYS):
        draw(base, "diamond", i).save(os.path.join(OUT, name + "_wallpaper.png"))
        draw(base, "striped", 100 + i).save(os.path.join(OUT, name + "_striped_wallpaper.png"))


if __name__ == "__main__":
    main()
