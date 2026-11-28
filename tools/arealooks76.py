#!/usr/bin/env python3
"""
(0.76) John's 28 Nov 12:24, Goldenrod's floor: "the interior is like this sort of more stony marble on the left, and
then that tile on the outside. And then for one reason or another, on that right hand side, you've gone with tiles on
the inside as well, which isn't what it should be. It should be the stony one, the exact same."

The three little squares edged in white in the middle of Goldenrod's streets (his 12:43 save: brown glazed
terracotta inside, the same block as the street round them, so 0.74 gave them the street's yellow flagstones) get the
pavements' stony cream flagstones (gold_plaza), the same as inside every other white edge.

Called from tools/arealooks.py after the 0.74 rows (so these win where they overlap).
"""

# Each square's inside (x0, z0, x1, z1), read off the save: brown glazed terracotta wholly ringed in white terracotta.
SQUARES = [(-2106, 672, -2101, 674), (-2106, 698, -2101, 700), (-2106, 729, -2101, 731)]


def round76(add, G):
    for x0, z0, x1, z1 in SQUARES:
        add("swap", "brown_glazed_terracotta", (x0, z0, x1, z1, "a white-edged square in Goldenrod's street"),
            "block/brown_glazed_terracotta=gold_plaza@4", ("19", "19"))
