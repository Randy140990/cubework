"""Puts back (git checkout) every changed PNG whose pixels are the same as the committed one, so a rebuild that
only re-encodes pictures doesn't touch them."""
import io, subprocess, sys
from PIL import Image
import numpy as np
root = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
changed = subprocess.check_output(["git", "-C", root, "diff", "--name-only"], text=True).split()
same = []
for f in changed:
    if not f.endswith(".png"): continue
    try:
        old = Image.open(io.BytesIO(subprocess.check_output(["git", "-C", root, "show", "HEAD:" + f]))).convert("RGBA")
        new = Image.open(root + "/" + f).convert("RGBA")
    except Exception:
        continue
    if old.size == new.size and np.array_equal(np.asarray(old), np.asarray(new)): same.append(f)
for i in range(0, len(same), 200): subprocess.check_call(["git", "-C", root, "checkout", "--"] + same[i:i + 200])
print(len(same), "unchanged pictures put back;", len([f for f in changed if f.endswith(".png")]) - len(same), "really changed")
