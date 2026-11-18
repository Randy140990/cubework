#!/usr/bin/env python3
"""Paints Retromon's first people as 64x64 player skins: the nurse, the Mart clerk, a youngster and a gym leader."""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "retromon", "textures", "entity", "npc")

def rect(p, x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            p[x, y] = c

def box(p, ox, oy, w, h, d, c):
    """All six faces of a box part in the skin layout (w wide, h tall, d deep) at texture offset (ox, oy)."""
    rect(p, ox + d, oy, ox + d + w, oy + d, c)                    # top
    rect(p, ox + d + w, oy, ox + d + 2 * w, oy + d, c)            # bottom
    rect(p, ox, oy + d, ox + 2 * d + 2 * w, oy + d + h, c)        # sides, front, back

def band(p, ox, oy, w, h, d, y0, y1, c):
    """Rows y0..y1 (from the top of the part) all the way round."""
    rect(p, ox, oy + d + y0, ox + 2 * d + 2 * w, oy + d + y1, c)

def front(p, ox, oy, w, h, d, x0, y0, x1, y1, c):
    rect(p, ox + d + x0, oy + d + y0, ox + d + x1, oy + d + y1, c)

