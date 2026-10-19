"""Draws the three blocky menu backgrounds (main / deposit / withdraw) in the style of my
orb drawing: lime box with dark olive outline, bubbly orb clusters, pale glow, drips, hidden cat.
1 texel = 2 GUI pixels. Run:  python -I make_gui.py <textures/gui folder>"""
import math, random, sys
from PIL import Image

W, H = 144, 108                 # texels (drawn at 288 x 216 GUI pixels)
BX, BY, BW, BH = 15, 18, 110, 64  # inner box (GUI 30,36 220x128)
# colours picked straight from my drawing
OLIVE = (55, 83, 14, 255)        # box outline
ORB_LINE = (55, 99, 14, 255)     # bubble outlines
LIME = (128, 255, 32, 255)       # box centre
LIME_M = (128, 245, 32, 255)     # box shading, middle band
LIME_D = (128, 229, 32, 255)     # box shading, edge band
ORB = (128, 245, 32, 255)
ORB_HI = (128, 255, 32, 255)
GLOW = (201, 229, 160, 255)      # glow, outer
GLOW_IN = (201, 245, 160, 255)   # glow, middle
GLOW_HOT = (201, 255, 160, 255)  # glow right around the bubbles
CLEAR = (0, 0, 0, 0)

CAT = ['.OOOOO.', 'OGGNGGO', 'OGEGEGO', 'OGGGGGO', 'OO...OO', 'O.....O']  # upside down, peeking


def make(seed, cat_x):
    rnd = random.Random(seed)
    im = Image.new('RGBA', (W, H), CLEAR)
    px = im.load()
    orbs = []  # (cx, cy, r)

    def free(cx, cy, r):
        # keep the cat's spot under the box empty
        if cat_x - 3 <= cx <= cat_x + 10 and BY + BH - 2 <= cy <= BY + BH + 9:
            return False
        return all((cx - x) ** 2 + (cy - y) ** 2 >= (r + rr) ** 2 for x, y, rr in orbs)

    def ok(cx, cy, r):
        return r + 1 <= cx < W - r - 1 and r + 1 <= cy < H - r - 1
    # Bubbles flow around the box edge like a stream: a smooth "thickness" wave decides where the
    # stream is thick, thin or broken; big bubbles hug the edge, smaller ones drift further out.
    per = 2 * (BW + BH)
    waves = [(rnd.uniform(1.5, 3.5), rnd.uniform(0, 6.3), rnd.uniform(0.5, 1.0)) for _ in range(3)]

    def thick(t):  # 0..1 around the edge
        v = sum(a * math.sin(2 * math.pi * f * t + ph) for f, ph, a in waves) / sum(a for _, _, a in waves)
        return max(0.0, min(1.0, 0.3 + 1.0 * v))

    def edge_point(t):  # point on the box outline + outward direction
        d = t * per
        if d < BW: return BX + d, BY, 0, -1
        d -= BW
        if d < BH: return BX + BW, BY + d, 1, 0
        d -= BH
        if d < BW: return BX + BW - d, BY + BH, 0, 1
        d -= BW
        return BX, BY + BH - d, -1, 0

    placed = 0
    for _ in range(20000):
        if placed >= 75: break
        t = rnd.random()
        k = thick(t)
        if k < 0.15 or rnd.random() > k: continue
        ex, ey, nx, ny = edge_point(t)
        out = abs(rnd.gauss(0, 2 + 6 * k))           # how far it drifted from the edge
        out -= 2                                      # some tuck behind the box edge
        r = round((1.2 + 2.6 * k) * max(0.0, 1 - out / (6 + 12 * k)) + rnd.uniform(-0.6, 0.6))
        if r < 2 and rnd.random() < 0.8: continue
        r = max(1, min(r, 4))
        side = rnd.uniform(-2, 2)
        cx = round(ex + nx * (out + r) + ny * side); cy = round(ey + ny * (out + r) + nx * side)
        if ok(cx, cy, r) and free(cx, cy, r):
            orbs.append((cx, cy, r)); placed += 1
    # drips: trails falling from where the bottom stream is thickest
    bottom = [t / 100 for t in range(100) if (BW + BH) / per < t / 100 < (2 * BW + BH) / per]
    bottom.sort(key=lambda t: -thick(t))
    used = []
    for t in bottom:
        if len(used) == 2: break
        x = round(edge_point(t)[0])
        if abs(x - cat_x - 3) < 10 or any(abs(x - u) < 14 for u in used): continue
        used.append(x)
        y = BY + BH + 4
        for i in range(rnd.randint(3, 5)):
            r = max(1, 3 - i // 2)
            if ok(x, y, r) and free(x, y, r): orbs.append((x, y, r))
            y += r * 2 + 1; x += rnd.randint(-1, 1)

    def disc(cx, cy, r):
        return [(x, y) for y in range(cy - r, cy + r + 1) for x in range(cx - r, cx + r + 1)
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r and 0 <= x < W and 0 <= y < H]

    # pale glow: everything within 3 texels of an orb or the box
    mask = set()
    for cx, cy, r in orbs: mask.update(disc(cx, cy, r + 3))
    for y in range(BY - 3, BY + BH + 4):
        for x in range(BX - 3, BX + BW + 4): mask.add((x, y))
    # keep the cat's spot glowing too
    for y in range(BY + BH, BY + BH + 9):
        for x in range(cat_x - 2, cat_x + 9): mask.add((x, y))
    for x, y in mask:
        if 0 <= x < W and 0 <= y < H: px[x, y] = GLOW
    inner = set()
    for cx, cy, r in orbs: inner.update(disc(cx, cy, r + 2))
    for x, y in inner:
        if (x, y) in mask and 0 <= x < W and 0 <= y < H: px[x, y] = GLOW_IN
    hot = set()
    for cx, cy, r in orbs: hot.update(disc(cx, cy, r + 1) if r > 1 else [])
    for x, y in hot: px[x, y] = GLOW_HOT

    # orbs: outline, fill, one highlight pixel
    for cx, cy, r in orbs:
        cells = set(disc(cx, cy, r))
        for x, y in cells:
            edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            px[x, y] = ORB_LINE if edge or r == 1 and (x, y) != (cx, cy) else ORB
        if r >= 2: px[cx - r // 2, cy - r // 2] = ORB_HI

    # the box: darker band inside the edge (like the drawing's shading), olive outline
    for y in range(BY, BY + BH):
        for x in range(BX, BX + BW):
            d = min(x - BX, BX + BW - 1 - x, y - BY, BY + BH - 1 - y)
            if d == 0:
                px[x, y] = OLIVE
                continue
            # the drawing's fade: dark edge -> middle green -> bright centre, wavy like brush strokes
            # and reaching further in at the corners
            corner = min(((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                         for cx in (BX, BX + BW) for cy in (BY, BY + BH))
            shade = min(d, corner * 0.55) + 1.6 * math.sin(x * 0.21 + seed) * math.cos(y * 0.33 - seed)
            px[x, y] = LIME_D if shade < 3.5 else LIME_M if shade < 9 else LIME
    # the hidden cat, peeking out under the bottom edge
    for dy, row in enumerate(CAT):
        for dx, ch in enumerate(row):
            if ch in 'OEN': px[cat_x + dx, BY + BH + dy] = OLIVE
            elif ch == 'G': px[cat_x + dx, BY + BH + dy] = LIME
    return im


if __name__ == '__main__':
    out = sys.argv[1]
    for name, seed, cat_x in (('main', 11, BX + BW - 20), ('deposit', 27, BX + 14), ('withdraw', 43, BX + 60)):
        make(seed, cat_x).save(f'{out}/bg_{name}.png')
