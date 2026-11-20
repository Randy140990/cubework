#!/usr/bin/env python3
"""Retromon's title wordmark: RETROMON in round, heavy letters (not blocks) on a gentle arch, filled gold to orange to
red with a shine across the top, edged in cream and then deep teal, standing on a teal 3D edge with a soft shadow.

    python3 tools/wordmark.py OUT.png [--scale 2]
Drawn at twice the size the title shows it (838 by 152 canvas pixels), so it's sharp on big screens.
"""
import math, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops

FONT = "/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf"


def main():
    out = sys.argv[1]
    k = 2
    W, H = 838 * k, 152 * k
    text = "RETROMON"
    size = int(128 * k)
    font = ImageFont.truetype(FONT, size)
    track = -int(2 * k)
    # Each letter's place along the line, then the whole word centred.
    widths = [font.getlength(c) for c in text]
    total = sum(widths) + track * (len(text) - 1)
    # Stretch it a little taller (heavy and round, like a game's name).
    big = Image.new("L", (int(total + 200 * k), H * 2), 0)
    masks = {}
    arch = 16 * k
    for name, stroke in (("fill", 0), ("cream", int(5 * k)), ("edge", int(12 * k))):
        m = Image.new("L", big.size, 0)
        d = ImageDraw.Draw(m)
        x = 100 * k
        for i, c in enumerate(text):
            t = (x - 100 * k + widths[i] / 2) / total
            y = int(70 * k - arch * math.sin(math.pi * t))
            layer = Image.new("L", big.size, 0)
            ImageDraw.Draw(layer).text((x, y), c, font=font, fill=255, stroke_width=stroke, stroke_fill=255)
            # A slight lean following the arch.
            ang = (t - 0.5) * -8
            cx, cy = x + widths[i] / 2, y + size * 0.6
            layer = layer.rotate(ang, resample=Image.BICUBIC, center=(cx, cy))
            m = ImageChops.lighter(m, layer)
            x += widths[i] + track
        masks[name] = m
    bbox = masks["edge"].getbbox()
    pad = 22 * k
    crop = (bbox[0] - pad, bbox[1] - pad, bbox[2] + pad, bbox[3] + pad + 16 * k)
    for n in masks:
        masks[n] = masks[n].crop(crop)
    w0, h0 = masks["fill"].size
    img = Image.new("RGBA", (w0, h0), (0, 0, 0, 0))

    def solid(rgb, mask, alpha=255):
        layer = Image.new("RGBA", (w0, h0), rgb + (0,))
        a = mask.point(lambda v: v * alpha // 255)
        layer.putalpha(a)
        return layer

    edge = masks["edge"]
    # The soft shadow under it all.
    sh = edge.filter(ImageFilter.GaussianBlur(8 * k))
    shadow = Image.new("L", (w0, h0), 0)
    shadow.paste(sh, (0, int(14 * k)))
    img = Image.alpha_composite(img, solid((8, 30, 28), shadow, 110))
    # The 3D edge: the outline stamped down a few pixels, darker as it goes.
    depth = int(9 * k)
    for dy in range(depth, 0, -1):
        m = Image.new("L", (w0, h0), 0)
        m.paste(edge, (0, dy))
        shade = int(22 + 18 * (1 - dy / depth))
        img = Image.alpha_composite(img, solid((shade // 2, shade + 22, shade + 18), m))
    img = Image.alpha_composite(img, solid((18, 60, 56), edge))
    img = Image.alpha_composite(img, solid((255, 244, 216), masks["cream"]))
    # The fill: gold at the top, orange, then red at the foot.
    grad = Image.new("RGBA", (w0, h0))
    fb = masks["fill"].getbbox()
    top, bot = fb[1], fb[3]
    stops = [(0.0, (255, 222, 104)), (0.45, (255, 160, 48)), (1.0, (229, 72, 77))]
    px = grad.load()
    for y in range(h0):
        t = min(1, max(0, (y - top) / max(1, bot - top)))
        for i in range(1, len(stops)):
            if t <= stops[i][0]:
                a0, c0 = stops[i - 1]
                a1, c1 = stops[i]
                u = (t - a0) / (a1 - a0)
                c = tuple(int(c0[j] + (c1[j] - c0[j]) * u) for j in range(3))
                break
        for x in range(w0):
            px[x, y] = c + (255,)
    grad.putalpha(masks["fill"])
    img = Image.alpha_composite(img, grad)
    # A shine across the top of the letters: the fill shrunk, cut off below the middle, soft.
    shine = masks["fill"].filter(ImageFilter.MinFilter(int(5 * k) | 1))
    cut = Image.new("L", (w0, h0), 0)
    ImageDraw.Draw(cut).rectangle((0, 0, w0, top + (bot - top) * 0.42), fill=255)
    shine = ImageChops.multiply(shine, cut).filter(ImageFilter.GaussianBlur(1.2 * k))
    img = Image.alpha_composite(img, solid((255, 255, 255), shine, 92))
    # Into the title's box, centred.
    final = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sc = min(W / w0, H / h0)
    fitted = img.resize((int(w0 * sc), int(h0 * sc)), Image.LANCZOS)
    final.paste(fitted, ((W - fitted.width) // 2, (H - fitted.height) // 2), fitted)
    final.save(out)
    print(out, final.size)


if __name__ == "__main__":
    main()
