"""
Generates the app icon and Play Store graphics from one geometry definition.

    python store/generate_assets.py            # icons + store icon + feature graphic
    python store/generate_assets.py --frames   # also frame store/screenshots/raw/*.png

Outputs
  app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml  (adaptive icon, API 26+)
  app/src/main/res/mipmap-*/ic_launcher{,_round}.png                            (legacy icons, API 24-25)
  store/play-icon-512.png, store/feature-graphic-1024x500.png
  store/screenshots/phone/*.png  (captioned 1080x1920 store screenshots, with --frames)

The icon is a 3x3 sliding-tile board with the gap in the bottom-right corner and the amber
tile sliding into it. Everything is laid out in the 108x108 adaptive-icon viewport.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
STORE = ROOT / "store"

# ---- Geometry (108 x 108 viewport, safe zone = circle of radius 33 around the centre) ----
BG_TOP = (0x86, 0x66, 0xFF)      # gradient start, top-left
BG_BOTTOM = (0x45, 0x25, 0xB0)   # gradient end, bottom-right
TILE = (0xFF, 0xFF, 0xFF)
WHITE = (0xFF, 0xFF, 0xFF)
TILE_SIZE, GAP, ORIGIN, RADIUS = 15.0, 3.5, 28.0, 3.6
SLIDE = 5.0                      # how far the amber tile has slid towards the gap
SHADOW_DY, SHADOW_ALPHA = 1.4, 0x38

# ---- Game glyphs, in tile-local units (0..15). Colours: "g" = glyph colour, or an RGB tuple. ----
#   ("poly", [(x, y), ...], col)        filled polygon
#   ("rrect", x, y, w, h, r, col)       filled rounded rectangle
#   ("circle", cx, cy, r, col)          filled circle
#   ("stroke", [(x, y), ...], w, col)   polyline with round caps/joins


def _star(cx, cy, ro, ri):
    import math
    return [(cx + (ro if i % 2 == 0 else ri) * math.sin(i * math.pi / 5),
             cy - (ro if i % 2 == 0 else ri) * math.cos(i * math.pi / 5)) for i in range(10)]


DARK = (0x3E, 0x27, 0x23)
GLYPHS = {
    # River crossing: a sailing boat.
    "boat": [("poly", [(2.6, 9.4), (12.4, 9.4), (10.7, 12.0), (4.3, 12.0)], "g"),
             ("poly", [(7.9, 2.6), (7.9, 8.6), (12.0, 8.6)], "g"),
             ("poly", [(7.1, 3.8), (7.1, 8.6), (3.6, 8.6)], "g")],
    # River crossing with animals (and the sheep & food puzzle to come).
    "sheep": [("stroke", [(6.4, 10.4), (6.4, 12.6)], 1.1, DARK),
              ("stroke", [(9.6, 10.4), (9.6, 12.6)], 1.1, DARK),
              ("circle", 7.4, 6.6, 2.3, "g"), ("circle", 9.9, 7.2, 2.1, "g"),
              ("circle", 10.1, 9.3, 2.0, "g"), ("circle", 7.7, 9.6, 2.2, "g"),
              ("circle", 5.9, 8.3, 2.0, "g"), ("circle", 3.9, 7.4, 1.8, DARK)],
    # Water jugs: a measuring glass, half full.
    "jug": [("stroke", [(4.5, 2.9), (4.5, 12.0), (10.5, 12.0), (10.5, 2.9)], 1.3, "g"),
            ("rrect", 4.5, 7.2, 6.0, 4.8, 0.4, "g")],
    # Tower of Hanoi: three discs on a peg.
    "hanoi": [("stroke", [(7.5, 3.0), (7.5, 11.0)], 1.1, "g"),
              ("rrect", 5.2, 4.7, 4.6, 1.9, 0.95, "g"),
              ("rrect", 4.0, 7.0, 7.0, 1.9, 0.95, "g"),
              ("rrect", 2.8, 9.3, 9.4, 1.9, 0.95, "g"),
              ("rrect", 2.2, 11.6, 10.6, 1.1, 0.55, "g")],
    # Sliding tiles: a 2x2 board with one gap.
    "sliding": [("rrect", 3.25, 3.25, 3.8, 3.8, 0.8, "g"), ("rrect", 7.95, 3.25, 3.8, 3.8, 0.8, "g"),
                ("rrect", 3.25, 7.95, 3.8, 3.8, 0.8, "g")],
    # Light toggle: a 3x3 board with a lit plus.
    "lights": [("rrect", 3.0 + c * 3.2, 3.0 + r * 3.2, 2.8, 2.8, 0.6,
                (0xFF, 0xD5, 0x4F) if r == 1 or c == 1 else (0x60, 0x7D, 0x8B))
               for r in range(3) for c in range(3)],
    # Stars you earn for an optimal solution.
    "star": [("poly", _star(7.5, 7.9, 5.0, 2.1), "g")],
    # Brain teasers / hints: a light bulb.
    "bulb": [("circle", 7.5, 6.0, 3.4, "g"),
             ("poly", [(5.5, 8.0), (9.5, 8.0), (8.9, 10.1), (6.1, 10.1)], "g"),
             ("rrect", 5.9, 10.5, 3.2, 1.0, 0.5, "g"),
             ("rrect", 6.4, 11.8, 2.2, 0.9, 0.45, "g")],
}

# (row, col) -> (glyph, tile colour, glyph colour). (2, 2) is the gap; (2, 1) is sliding into it.
BOARD = {
    (0, 0): ("boat", (0x1E, 0x88, 0xE5), WHITE),
    (0, 1): ("sheep", (0x7C, 0xB3, 0x42), WHITE),
    (0, 2): ("jug", (0x00, 0x89, 0x7B), WHITE),
    (1, 0): ("hanoi", (0xF4, 0x51, 0x1E), WHITE),
    (1, 1): ("star", WHITE, (0x5B, 0x3C, 0xC4)),
    (1, 2): ("lights", (0x37, 0x47, 0x4F), WHITE),
    (2, 0): ("bulb", (0x00, 0xAC, 0xC1), WHITE),
    (2, 1): ("sliding", (0xFF, 0xC8, 0x3D), (0x7A, 0x4A, 0x00)),
}


def tiles():
    """(x, y, tile colour, glyph, glyph colour); the amber tile is mid-slide towards the empty corner."""
    out = []
    for (row, col), (glyph, color, glyph_color) in BOARD.items():
        x = ORIGIN + col * (TILE_SIZE + GAP) + (SLIDE if (row, col) == (2, 1) else 0)
        y = ORIGIN + row * (TILE_SIZE + GAP)
        out.append((x, y, color, glyph, glyph_color))
    return out


def glyph_prims(x, y, glyph, glyph_color):
    """The glyph's primitives moved to viewport coordinates, with colours resolved."""
    col = lambda c: glyph_color if c == "g" else c
    for p in GLYPHS[glyph]:
        kind = p[0]
        if kind == "poly":
            yield kind, [(x + px, y + py) for px, py in p[1]], col(p[2])
        elif kind == "rrect":
            yield kind, (x + p[1], y + p[2], p[3], p[4], p[5]), col(p[6])
        elif kind == "circle":
            yield kind, (x + p[1], y + p[2], p[3]), col(p[4])
        elif kind == "stroke":
            yield kind, ([(x + px, y + py) for px, py in p[1]], p[2]), col(p[3])


def streaks():
    """Motion lines behind the sliding tile: (x1, y, x2)."""
    y0 = ORIGIN + 2 * (TILE_SIZE + GAP)
    x2 = ORIGIN + TILE_SIZE + GAP + SLIDE - 1.6
    return [(x2 - 4.5, y0 + 5.0, x2), (x2 - 3.0, y0 + 10.0, x2)]


STREAK_WIDTH = 1.8


# ---- Vector drawables ----
def rr_path(x, y, w, h, r):
    f = lambda v: f"{v:.2f}".rstrip("0").rstrip(".")
    return (f"M{f(x + r)},{f(y)}h{f(w - 2 * r)}a{f(r)},{f(r)} 0,0 1,{f(r)},{f(r)}"
            f"v{f(h - 2 * r)}a{f(r)},{f(r)} 0,0 1,-{f(r)},{f(r)}h-{f(w - 2 * r)}"
            f"a{f(r)},{f(r)} 0,0 1,-{f(r)},-{f(r)}v-{f(h - 2 * r)}a{f(r)},{f(r)} 0,0 1,{f(r)},-{f(r)}z")


def hexc(rgb, alpha=0xFF):
    return "#%02X%02X%02X%02X" % (alpha, *rgb)


def vector(body, aapt=False):
    ns = ' xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated by store/generate_assets.py; edit the script, not this file. -->\n'
            f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            f'{body}</vector>\n')


def fill_path(d, color):
    return f'    <path\n        android:fillColor="{color}"\n        android:pathData="{d}" />\n'


def fmt(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def prim_xml(kind, geom, color):
    if kind == "poly":
        return fill_path("M" + "L".join(f"{fmt(a)},{fmt(b)}" for a, b in geom) + "Z", color)
    if kind == "rrect":
        return fill_path(rr_path(*geom), color)
    if kind == "circle":
        cx, cy, r = geom
        return fill_path(f"M{fmt(cx - r)},{fmt(cy)}a{fmt(r)},{fmt(r)} 0,1 0,{fmt(2 * r)},0"
                         f"a{fmt(r)},{fmt(r)} 0,1 0,-{fmt(2 * r)},0Z", color)
    pts, w = geom
    return (f'    <path\n        android:pathData="M' + "L".join(f"{fmt(a)},{fmt(b)}" for a, b in pts) + '"\n'
            f'        android:strokeColor="{color}"\n        android:strokeWidth="{fmt(w)}"\n'
            '        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n')


def glyph_xml(x, y, glyph, glyph_color, mono=False):
    return "".join(prim_xml(kind, geom, "#FF000000" if mono else hexc(c))
                   for kind, geom, c in glyph_prims(x, y, glyph, glyph_color))


def streak_paths(color):
    return "".join(
        f'    <path\n        android:pathData="M{x1:g},{y:g}H{x2:g}"\n        android:strokeColor="{color}"\n'
        f'        android:strokeWidth="{STREAK_WIDTH:g}"\n        android:strokeLineCap="round" />\n'
        for x1, y, x2 in streaks())


def write_vectors():
    drawable = RES / "drawable"
    background = vector(
        '    <path android:pathData="M0,0h108v108h-108z">\n'
        '        <aapt:attr name="android:fillColor">\n'
        '            <gradient\n                android:type="linear"\n'
        '                android:startX="0"\n                android:startY="0"\n'
        '                android:endX="108"\n                android:endY="108"\n'
        f'                android:startColor="{hexc(BG_TOP)}"\n'
        f'                android:endColor="{hexc(BG_BOTTOM)}" />\n'
        '        </aapt:attr>\n    </path>\n', aapt=True)
    shadows = "".join(fill_path(rr_path(x, y + SHADOW_DY, TILE_SIZE, TILE_SIZE, RADIUS), hexc((0, 0, 0), SHADOW_ALPHA))
                      for x, y, *_ in tiles())
    faces = "".join(fill_path(rr_path(x, y, TILE_SIZE, TILE_SIZE, RADIUS), hexc(c)) + glyph_xml(x, y, g, gc)
                    for x, y, c, g, gc in tiles())
    foreground = vector(shadows + streak_paths(hexc(TILE, 0xB0)) + faces)
    # Themed icons only use alpha: outline each tile and draw its glyph inside.
    outline = lambda x, y: (f'    <path\n        android:pathData="{rr_path(x + 0.6, y + 0.6, TILE_SIZE - 1.2, TILE_SIZE - 1.2, RADIUS - 0.6)}"\n'
                            '        android:strokeColor="#FF000000"\n        android:strokeWidth="1.2" />\n')
    mono = vector(streak_paths("#FF000000") +
                  "".join(outline(x, y) + glyph_xml(x, y, g, gc, mono=True) for x, y, c, g, gc in tiles()))
    (drawable / "ic_launcher_background.xml").write_text(background, encoding="utf-8")
    (drawable / "ic_launcher_foreground.xml").write_text(foreground, encoding="utf-8")
    (drawable / "ic_launcher_monochrome.xml").write_text(mono, encoding="utf-8")


# ---- Raster rendering ----
SS = 4  # supersampling


def gradient(w, h, start=BG_TOP, end=BG_BOTTOM):
    yy, xx = np.mgrid[0:h, 0:w]
    t = ((xx / max(w - 1, 1)) + (yy / max(h - 1, 1))) / 2.0
    s, e = np.array(start, float), np.array(end, float)
    rgb = s + (e - s) * t[..., None]
    return Image.fromarray(rgb.astype(np.uint8)).convert("RGBA")


def render_icon(px, crop=72.0, shape="square"):
    """Renders the adaptive icon as seen through a launcher mask: [crop] units of the 108 viewport."""
    big = px * SS
    k = big / crop
    off = (108 - crop) / 2
    T = lambda v: (v - off) * k
    img = gradient(big, big)

    shadow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    for x, y, *_ in tiles():
        sd.rounded_rectangle([T(x), T(y + SHADOW_DY), T(x + TILE_SIZE), T(y + SHADOW_DY + TILE_SIZE)],
                             radius=RADIUS * k, fill=(0, 0, 0, SHADOW_ALPHA))
    img = Image.alpha_composite(img, shadow)

    d = ImageDraw.Draw(img)
    for x1, y, x2 in streaks():
        w = STREAK_WIDTH * k
        d.line([T(x1), T(y), T(x2), T(y)], fill=TILE + (0xB0,), width=round(w))
        for cx in (x1, x2):
            d.ellipse([T(cx) - w / 2, T(y) - w / 2, T(cx) + w / 2, T(y) + w / 2], fill=TILE + (0xB0,))
    for x, y, c, g, gc in tiles():
        d.rounded_rectangle([T(x), T(y), T(x + TILE_SIZE), T(y + TILE_SIZE)], radius=RADIUS * k, fill=c + (255,))
        for kind, geom, color in glyph_prims(x, y, g, gc):
            fill = color + (255,)
            if kind == "poly":
                d.polygon([(T(a), T(b)) for a, b in geom], fill=fill)
            elif kind == "rrect":
                gx, gy, gw, gh, gr = geom
                d.rounded_rectangle([T(gx), T(gy), T(gx + gw), T(gy + gh)], radius=gr * k, fill=fill)
            elif kind == "circle":
                cx, cy, r = geom
                d.ellipse([T(cx - r), T(cy - r), T(cx + r), T(cy + r)], fill=fill)
            else:
                pts, w = geom
                d.line([(T(a), T(b)) for a, b in pts], fill=fill, width=round(w * k), joint="curve")
                for a, b in pts:
                    d.ellipse([T(a) - w * k / 2, T(b) - w * k / 2, T(a) + w * k / 2, T(b) + w * k / 2], fill=fill)

    mask = Image.new("L", (big, big), 0)
    md = ImageDraw.Draw(mask)
    if shape == "circle":
        md.ellipse([0, 0, big - 1, big - 1], fill=255)
    elif shape == "rounded":
        md.rounded_rectangle([0, 0, big - 1, big - 1], radius=big * 0.22, fill=255)
    else:
        md.rectangle([0, 0, big, big], fill=255)
    img.putalpha(mask)
    return img.resize((px, px), Image.LANCZOS)


def write_legacy_mipmaps():
    for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        folder = RES / f"mipmap-{name}"
        for old in folder.glob("ic_launcher*.webp"):
            old.unlink()
        # Legacy launchers draw the bitmap as-is, so leave a small margin like the 48dp keyline.
        inner = round(px * 44 / 48)
        for shape, file in (("rounded", "ic_launcher.png"), ("circle", "ic_launcher_round.png")):
            canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
            icon = render_icon(inner, shape=shape)
            canvas.alpha_composite(icon, ((px - inner) // 2, (px - inner) // 2))
            canvas.save(folder / file, optimize=True)


def font(name, size):
    return ImageFont.truetype(f"C:/Windows/Fonts/{name}", size)


def decorative_tiles(img, seed, count, alpha=26):
    rng = np.random.default_rng(seed)
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    w, h = img.size
    for _ in range(count):
        s = int(rng.integers(40, 110))
        x, y = int(rng.integers(-40, w)), int(rng.integers(-40, h))
        d.rounded_rectangle([x, y, x + s, y + s], radius=s * 0.24, fill=(255, 255, 255, alpha))
    img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(1)))


def write_store_graphics():
    render_icon(512, crop=76.0, shape="square").convert("RGB").save(STORE / "play-icon-512.png", optimize=True)

    W, H = 1024, 500
    g = gradient(W, H)
    decorative_tiles(g, seed=3, count=14)
    icon = render_icon(300, crop=76.0, shape="rounded")
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([82, 112, 82 + 300, 112 + 300], radius=66, fill=(20, 0, 60, 110))
    g.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(18)))
    g.alpha_composite(icon, (76, 96))
    d = ImageDraw.Draw(g)
    d.text((432, 118), "Lite Puzzles", font=font("segoeuib.ttf", 92), fill="white")
    d.text((436, 236), "Classic logic mini games", font=font("segoeui.ttf", 40), fill=(235, 228, 255))
    # Game names rather than emoji: system emoji fonts aren't licensed for store art.
    d.text((438, 318), "River crossing · Water jugs · Tower of Hanoi", font=font("seguisb.ttf", 28), fill=(220, 210, 255))
    d.text((438, 358), "Sliding tiles · Light toggle", font=font("seguisb.ttf", 28), fill=(220, 210, 255))
    g.convert("RGB").save(STORE / "feature-graphic-1024x500.png", optimize=True)


# ---- Captioned screenshots ----
CAPTIONS = {
    "01_home": ("Classic puzzles,", "one tap away"),
    "02_river": ("Get everyone across", "the river safely"),
    "03_jugs": ("Measure exactly", "the right amount"),
    "04_hanoi": ("Move the tower,", "one disc at a time"),
    "05_tiles": ("Slide the tiles", "back in order"),
    "06_lights": ("Switch every", "light off"),
}


def write_framed_screenshots():
    raw_dir, out_dir = STORE / "screenshots" / "raw", STORE / "screenshots" / "phone"
    out_dir.mkdir(parents=True, exist_ok=True)
    W, H = 1080, 1920
    for i, raw in enumerate(sorted(raw_dir.glob("*.png"))):
        line1, line2 = CAPTIONS.get(raw.stem, (raw.stem, ""))
        canvas = gradient(W, H)
        decorative_tiles(canvas, seed=10 + i, count=10, alpha=18)
        d = ImageDraw.Draw(canvas)
        for text, y, f in ((line1, 70, font("segoeuib.ttf", 76)), (line2, 165, font("segoeuib.ttf", 76))):
            d.text((W / 2, y), text, font=f, fill="white", anchor="mt")
        shot = Image.open(raw).convert("RGBA")
        sh = H - 300 - 40
        sw = round(shot.width * sh / shot.height)
        shot = shot.resize((sw, sh), Image.LANCZOS)
        r = 48
        mask = Image.new("L", shot.size, 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=r, fill=255)
        x, y = (W - sw) // 2, 300
        shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle([x, y + 12, x + sw, y + sh + 12], radius=r, fill=(20, 0, 60, 140))
        canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(22)))
        frame = Image.new("RGBA", (sw + 16, sh + 16), (0, 0, 0, 0))
        ImageDraw.Draw(frame).rounded_rectangle([0, 0, sw + 15, sh + 15], radius=r + 8, fill=(25, 18, 40, 255))
        canvas.alpha_composite(frame, (x - 8, y - 8))
        shot.putalpha(mask)
        canvas.alpha_composite(shot, (x, y))
        canvas.convert("RGB").save(out_dir / f"{raw.stem}.png", optimize=True)


if __name__ == "__main__":
    write_vectors()
    write_legacy_mipmaps()
    write_store_graphics()
    if "--frames" in sys.argv:
        write_framed_screenshots()
    print("done")
