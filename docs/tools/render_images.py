#!/usr/bin/env python3
"""Renders the documentation images for Custom Disc Maestro from the mod's actual textures.

Every illustration (pipeline, recipes, disc styles, sleeves, worktables) is composed from the
textures shipped in `src/main/resources` plus a handful of vanilla textures taken from the
Minecraft client jar. The client jar is located automatically:

  1. `$MC_CLIENT_JAR` if set,
  2. the NeoForm cache (`~/.gradle/caches/neoformruntime/artifacts/minecraft_*_client.jar`),
  3. otherwise it is downloaded from Mojang's public piston-meta CDN (cached in `~/.cache`).

Usage:  python docs/tools/render_images.py        (writes into docs/img/)
Needs:  Pillow  (`pip install pillow`)
"""
import glob
import io
import json
import os
import pathlib
import shutil
import urllib.request
import zipfile

from PIL import Image, ImageDraw, ImageFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
CDM_I = ROOT / "src/main/resources/assets/cdm/textures/item"
CDM_B = ROOT / "src/main/resources/assets/cdm/textures/block"
OUT = ROOT / "docs/img"
MC_VERSION = "1.21.1"

VANILLA_NEEDED = [
    "item/iron_ingot", "item/copper_ingot", "item/redstone", "item/string", "item/paper",
    "item/diamond", "item/flint", "item/spider_eye", "item/black_dye", "item/white_dye",
    "block/note_block", "block/piston_top", "block/oak_planks", "block/blackstone",
    "block/cauldron_top", "block/cauldron_side", "block/water_still",
    "block/jukebox_top", "block/jukebox_side",
]

# ---------------------------------------------------------------- vanilla jar resolution
def find_client_jar() -> pathlib.Path:
    env = os.environ.get("MC_CLIENT_JAR")
    if env and pathlib.Path(env).is_file():
        return pathlib.Path(env)
    for hit in glob.glob(os.path.expanduser(
            f"~/.gradle/caches/neoformruntime/artifacts/minecraft_{MC_VERSION}_client.jar")):
        return pathlib.Path(hit)
    cache = pathlib.Path(os.path.expanduser(f"~/.cache/cdm-docs/minecraft_{MC_VERSION}_client.jar"))
    if cache.is_file():
        return cache
    print(f"downloading Minecraft {MC_VERSION} client jar from piston-meta…")
    manifest = json.load(urllib.request.urlopen(
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    version_url = next(v["url"] for v in manifest["versions"] if v["id"] == MC_VERSION)
    client_url = json.load(urllib.request.urlopen(version_url))["downloads"]["client"]["url"]
    cache.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(client_url) as resp, open(cache, "wb") as f:
        shutil.copyfileobj(resp, f)
    return cache

_JAR = zipfile.ZipFile(find_client_jar())

def van(name: str) -> Image.Image:
    data = _JAR.read(f"assets/minecraft/textures/{name}.png")
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    return im.crop((0, 0, 16, 16)) if im.height > 16 else im  # first frame of animations

def cdm(n): return Image.open(CDM_I / f"{n}.png").convert("RGBA")
def cdmb(n): return Image.open(CDM_B / f"{n}.png").convert("RGBA")

# ---------------------------------------------------------------- pixel helpers
def tint(im, rgb):
    o = Image.new("RGBA", im.size, (0, 0, 0, 0))
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = im.getpixel((x, y))
            if a:
                o.putpixel((x, y), (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a))
    return o

def comp(*layers):
    o = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for l in layers:
        o.alpha_composite(l)
    return o

def disc(style, vinyl, label, broken=False):
    sfx = "_broken" if broken else ""
    return comp(tint(cdm(f"disc_vinyl{sfx}"), vinyl),
                tint(cdm(f"disc_label_{style}{sfx}"), label), cdm(f"disc_frame{sfx}"))

def sleeve(bg_c, st_c, sticker=None, filled=False):
    layers = ([cdm("disc_peek")] if filled else []) + [tint(cdm("sleeve_base"), bg_c)]
    if sticker:
        layers.append(tint(cdm(f"sleeve_sticker_{sticker}"), st_c))
    return comp(*layers)

# ---------------------------------------------------------------- isometric block renderer
def iso(top, front, side, k=5, fluid=None, fluid_drop=3):
    """Minecraft-style isometric cube: top lit 1.0, front 0.8, side 0.6; optional inner fluid."""
    pad = 2
    W = H = 32 * k + 2 * pad
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    Tx, Ty = W / 2, pad
    ex, ey, ez = (k, k / 2), (-k, k / 2), (0, k)

    def P(u, v, w):
        return (Tx + u * ex[0] + v * ey[0], Ty + u * ex[1] + v * ey[1] + w * ez[1])

    def shade(c, f):
        return (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])

    def face(tex, corner, f):
        for ty in range(16):
            for tx in range(16):
                c = tex.getpixel((tx, ty))
                if c[3] == 0:
                    continue
                d.polygon([corner(tx, ty), corner(tx + 1, ty),
                           corner(tx + 1, ty + 1), corner(tx, ty + 1)], fill=shade(c, f))

    face(top, lambda u, v: P(u, v, 0), 1.0)
    face(front, lambda u, w: P(u, 16, w), 0.80)
    face(side, lambda v, w: P(16, v, w), 0.60)
    if fluid is not None:
        for ty in range(2, 14):
            for tx in range(2, 14):
                c = fluid.getpixel((tx, ty))
                if c[3] == 0:
                    continue
                d.polygon([P(tx, ty, fluid_drop), P(tx + 1, ty, fluid_drop),
                           P(tx + 1, ty + 1, fluid_drop), P(tx, ty + 1, fluid_drop)],
                          fill=shade(c, 0.95))
    return im

# ---------------------------------------------------------------- design system
BG_TOP, BG_BOT = (30, 32, 41), (22, 23, 30)
PANEL, PANEL_HI, PANEL_BRD = (44, 47, 59), (58, 62, 78), (18, 19, 25)
SLOT, SLOT_BRD, SLOT_HI = (58, 62, 76), (24, 26, 33), (78, 84, 102)
TXT, SUB, GOLD = (236, 237, 242), (167, 171, 186), (232, 200, 74)
CELL = 66          # slot size
CONN = 40          # connector (arrow/plus) box width
GAP = 10

def font(sz, bold=False):
    for p in (f"/usr/share/fonts/truetype/dejavu/DejaVuSans{'-Bold' if bold else ''}.ttf",
              f"/usr/share/fonts/truetype/noto/NotoSans-{'Bold' if bold else 'Regular'}.ttf"):
        try:
            return ImageFont.truetype(p, sz)
        except OSError:
            continue
    return ImageFont.load_default()

F_TITLE, F_CAP = font(19, True), font(13)
_measure = ImageDraw.Draw(Image.new("RGBA", (8, 8)))

def wrap_caption(text, max_w=120):
    """Wrap into at most two lines; returns (lines, box_width)."""
    words = text.split()
    lines, cur = [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if _measure.textlength(t, font=F_CAP) <= max_w or not cur:
            cur = t
        else:
            lines.append(cur)
            cur = w
    lines.append(cur)
    if len(lines) > 2:  # squeeze leftovers into the second line
        lines = [lines[0], " ".join(lines[1:])]
    return lines, max(int(_measure.textlength(l, font=F_CAP)) for l in lines)

def gradient(w, h):
    im = Image.new("RGBA", (w, h))
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(BG_TOP[i] + (BG_BOT[i] - BG_TOP[i]) * t) for i in range(3))
        ImageDraw.Draw(im).line([(0, y), (w, y)], fill=c + (255,))
    return im

def slot_box(im, d, x, y, icon, size=CELL):
    d.rounded_rectangle([x, y, x + size, y + size], 7, fill=SLOT, outline=SLOT_BRD, width=2)
    d.line([x + 4, y + 3, x + size - 4, y + 3], fill=SLOT_HI, width=1)
    if icon is not None:
        k = size - 14
        im.alpha_composite(icon.resize((k, k), Image.NEAREST), (x + 7, y + 7))

def chevron(d, cx, cy):
    d.polygon([(cx - 8, cy - 10), (cx + 8, cy), (cx - 8, cy + 10)], fill=GOLD)

def plus_sign(d, cx, cy):
    d.line([cx - 8, cy, cx + 8, cy], fill=SUB, width=4)
    d.line([cx, cy - 8, cx, cy + 8], fill=SUB, width=4)

def render(title, rows, path):
    """rows: list of rows; each row is a list of elements:
    ("slot", 16x16 icon, caption) | ("big", pre-rendered image, caption) | ("plus",) | ("arrow",)"""
    boxes_rows = []
    for row in rows:
        boxes = []
        for e in row:
            if e[0] in ("slot", "big"):
                icon_w = CELL if e[0] == "slot" else e[1].width
                lines, cap_w = wrap_caption(e[2])
                boxes.append((e, max(icon_w, cap_w) + 6, lines))
            else:
                boxes.append((e, CONN, None))
        boxes_rows.append(boxes)

    row_ws = [sum(b[1] for b in boxes) + GAP * (len(boxes) - 1) for boxes in boxes_rows]
    W = max(row_ws) + 48
    row_hs = []
    for boxes in boxes_rows:
        icon_h = max((b[0][1].height if b[0][0] == "big" else CELL)
                     for b in boxes if b[0][0] in ("slot", "big"))
        cap_lines = max((len(b[2]) for b in boxes if b[2]), default=1)
        row_hs.append(icon_h + 10 + cap_lines * 17)
    H = 58 + sum(row_hs) + 14 * (len(rows) - 1) + 18

    im = gradient(W, H)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 7, H - 7], 10, fill=PANEL, outline=PANEL_BRD, width=2)
    d.line([8, 8, W - 9, 8], fill=PANEL_HI, width=1)
    d.text((22, 16), title, font=F_TITLE, fill=TXT)
    d.line([22, 44, 22 + d.textlength(title, font=F_TITLE), 44], fill=GOLD, width=2)

    y = 56
    for boxes, row_w, row_h in zip(boxes_rows, row_ws, row_hs):
        icon_h = row_h - 10 - 17 * max((len(b[2]) for b in boxes if b[2]), default=1)
        x = (W - row_w) // 2
        for (e, bw, lines) in boxes:
            cx = x + bw / 2
            if e[0] == "slot":
                sy = y + (icon_h - CELL) // 2
                slot_box(im, d, int(cx - CELL / 2), sy, e[1])
            elif e[0] == "big":
                im.alpha_composite(e[1], (int(cx - e[1].width / 2), y + (icon_h - e[1].height) // 2))
            elif e[0] == "plus":
                plus_sign(d, cx, y + icon_h / 2)
            else:
                chevron(d, cx, y + icon_h / 2)
            if lines:
                for i, ln in enumerate(lines):
                    d.text((cx - d.textlength(ln, font=F_CAP) / 2, y + icon_h + 8 + i * 17),
                           ln, font=F_CAP, fill=SUB)
            x += bw + GAP
        y += row_h + 14
    im.save(path)
    print("wrote", path.name)

def render_recipe(title, grid, legend, result, result_name, path):
    """3x3 shaped recipe with a legend; result is a ("slot", icon) or ("big", image)."""
    cell, g = 60, 8
    grid_w = 3 * cell + 2 * g
    res_lines, res_w = wrap_caption(result_name, 150)
    res_iw = result[1].width if result[0] == "big" else cell
    right_w = max(res_iw, res_w) + 24
    legend_h = 28 * len(legend) + 8
    body_h = max(3 * cell + 2 * g, (result[1].height if result[0] == "big" else cell))
    W = 24 + grid_w + 52 + right_w + 18
    H = 58 + body_h + 12 + legend_h + 16
    im = gradient(W, H)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([6, 6, W - 7, H - 7], 10, fill=PANEL, outline=PANEL_BRD, width=2)
    d.text((22, 16), title, font=F_TITLE, fill=TXT)
    d.line([22, 44, 22 + d.textlength(title, font=F_TITLE), 44], fill=GOLD, width=2)
    oy = 56 + (body_h - (3 * cell + 2 * g)) // 2
    for r in range(3):
        for c in range(3):
            slot_box(im, d, 22 + c * (cell + g), oy + r * (cell + g), grid[r][c], cell)
    mid_y = 56 + body_h / 2
    chevron(d, 22 + grid_w + 26, mid_y)
    rx = 22 + grid_w + 52
    if result[0] == "big":
        im.alpha_composite(result[1], (int(rx + (right_w - result[1].width) / 2),
                                       int(mid_y - result[1].height / 2)))
    else:
        slot_box(im, d, int(rx + (right_w - cell) / 2), int(mid_y - cell / 2), result[1], cell)
    for i, ln in enumerate(res_lines):
        d.text((rx + right_w / 2 - d.textlength(ln, font=F_CAP) / 2,
                56 + body_h - 4 + i * 17), ln, font=F_CAP, fill=SUB)
    ly = 56 + body_h + 16
    for i, (icon, name) in enumerate(legend):
        im.alpha_composite(icon.resize((22, 22), Image.NEAREST), (24, ly + i * 28))
        d.text((54, ly + i * 28 + 4), name, font=F_CAP, fill=SUB)
    im.save(path)
    print("wrote", path.name)

# ---------------------------------------------------------------- render everything
def main():
    OUT.mkdir(parents=True, exist_ok=True)

    EMERALD = cdmb("kupfernickel_still")
    WATER = tint(van("block/water_still"), (70, 120, 228))
    CAULDRON_W = iso(van("block/cauldron_top"), van("block/cauldron_side"), van("block/cauldron_side"), 4, fluid=WATER)
    CAULDRON_K = iso(van("block/cauldron_top"), van("block/cauldron_side"), van("block/cauldron_side"), 4, fluid=EMERALD)
    LATHE = iso(cdmb("cutting_lathe_top"), cdmb("cutting_lathe_front"), cdmb("cutting_lathe_side"), 4)
    LATHE_D = iso(cdmb("cutting_lathe_top_disc"), cdmb("cutting_lathe_front"), cdmb("cutting_lathe_side"), 4)
    PRESS = iso(cdmb("record_press_top"), cdmb("record_press_front"), cdmb("record_press_side"), 4)
    PRESS_D = iso(cdmb("record_press_top_disc"), cdmb("record_press_front"), cdmb("record_press_side"), 4)
    PACK = iso(cdmb("packaging_table_top"), cdmb("packaging_table_front"), cdmb("packaging_table_side"), 4)
    JUKEBOX = iso(van("block/jukebox_top"), van("block/jukebox_side"), van("block/jukebox_side"), 4)
    BIG = dict(k=5)
    LATHE_XL = iso(cdmb("cutting_lathe_top"), cdmb("cutting_lathe_front"), cdmb("cutting_lathe_side"), **BIG)
    LATHE_D_XL = iso(cdmb("cutting_lathe_top_disc"), cdmb("cutting_lathe_front"), cdmb("cutting_lathe_side"), **BIG)
    PRESS_XL = iso(cdmb("record_press_top"), cdmb("record_press_front"), cdmb("record_press_side"), **BIG)
    PRESS_D_XL = iso(cdmb("record_press_top_disc"), cdmb("record_press_front"), cdmb("record_press_side"), **BIG)
    PACK_XL = iso(cdmb("packaging_table_top"), cdmb("packaging_table_front"), cdmb("packaging_table_side"), **BIG)

    render("The production pipeline", [
        [("slot", cdm("blank_disc"), "Blank Disc"), ("arrow",),
         ("big", LATHE_D, "Cutting Lathe"), ("arrow",),
         ("slot", cdm("disc_master"), "Master Disc"), ("arrow",),
         ("big", CAULDRON_K, "Kupfernickel Bath"), ("arrow",)],
        [("slot", cdm("matrix"), "Galvanic Matrix"), ("arrow",),
         ("big", PRESS_D, "Record Press"), ("arrow",),
         ("slot", disc("ribbon", (20, 20, 20), (232, 200, 74)), "Music Disc"), ("arrow",),
         ("big", JUKEBOX, "Jukebox")],
    ], OUT / "pipeline.png")

    I, C, R = van("item/iron_ingot"), van("item/copper_ingot"), van("item/redstone")
    N, P, PL = van("block/note_block"), van("block/piston_top"), van("block/oak_planks")
    ST, PA, E = van("item/string"), van("item/paper"), None

    render_recipe("Blank Disc", [[E, I, E], [I, E, I], [E, I, E]],
                  [(I, "Iron Ingot ×4")],
                  ("slot", cdm("blank_disc")), "Blank Disc", OUT / "recipe_blank_disc.png")
    render_recipe("Cutting Lathe", [[I, C, I], [C, N, C], [I, R, I]],
                  [(I, "Iron Ingot ×4"), (C, "Copper Ingot ×3"), (N, "Note Block"), (R, "Redstone Dust")],
                  ("big", LATHE), "Cutting Lathe", OUT / "recipe_cutting_lathe.png")
    render_recipe("Record Press", [[I, I, I], [I, P, I], [C, C, C]],
                  [(I, "Iron Ingot ×5"), (P, "Piston"), (C, "Copper Ingot ×3")],
                  ("big", PRESS), "Record Press", OUT / "recipe_record_press.png")
    render_recipe("Packaging Table", [[PL, PL, PL], [PL, ST, PL], [PL, PL, PL]],
                  [(PL, "Any Planks ×8"), (ST, "String")],
                  ("big", PACK), "Packaging Table", OUT / "recipe_packaging_table.png")
    render_recipe("Blank Sleeve", [[PA, PA, PA], [PA, E, PA], [PA, PA, PA]],
                  [(PA, "Paper ×8")],
                  ("slot", cdm("sleeve_blank")), "Blank Sleeve", OUT / "recipe_sleeve_blank.png")

    render("Brewing the kupfernickel bath", [[
        ("slot", van("item/copper_ingot"), "Copper Ingot"), ("plus",),
        ("slot", van("item/spider_eye"), "Spider Eye"), ("plus",),
        ("big", CAULDRON_W, "Water Cauldron"), ("arrow",),
        ("big", CAULDRON_K, "Kupfernickel Bath"),
    ]], OUT / "brew_bath.png")

    render("Electroforming the matrix", [[
        ("slot", cdm("disc_master"), "Master Disc"), ("plus",),
        ("big", CAULDRON_K, "Kupfernickel Bath"), ("arrow",),
        ("slot", cdm("matrix"), "Galvanic Matrix"),
    ]], OUT / "electroform.png")

    render("Pressing a record", [[
        ("slot", cdm("matrix"), "Galvanic Matrix"), ("plus",),
        ("slot", van("block/blackstone"), "Blackstone"), ("plus",),
        ("slot", van("item/black_dye"), "Black Dye"), ("plus",),
        ("slot", cdm("pattern_stripes"), "Stripes Pattern Template"), ("plus",),
        ("slot", van("item/white_dye"), "White Dye"), ("arrow",),
        ("slot", disc("stripes", (20, 20, 20), (240, 240, 240)), "Music Disc"),
    ]], OUT / "press_inputs.png")

    V1, V2, L1, L2 = (20, 20, 20), (176, 46, 46), (240, 240, 240), (232, 200, 74)
    render("Disc styles and wear", [[
        ("slot", cdm("disc_master"), "Master Disc"),
        ("slot", disc("plain", V1, L1), "Plain"),
        ("slot", disc("stripes", V2, L1), "Stripes"),
        ("slot", disc("ribbon", V1, L2), "Ribbon"),
        ("slot", disc("dots", V2, L2), "Dots"),
        ("slot", disc("dots", V2, L2, broken=True), "Fully worn"),
        ("slot", cdm("disc_master_broken"), "Worn master"),
    ]], OUT / "disc_styles.png")

    render("Sleeves and stencils", [[
        ("slot", cdm("sleeve_blank"), "Blank Sleeve"),
        ("slot", sleeve((26, 140, 130), (244, 238, 214), "stripes"), "Record Sleeve"),
        ("slot", comp(cdm("disc_peek"), cdm("sleeve_blank")), "Blank + record"),
        ("slot", sleeve((24, 24, 28), (232, 200, 74), "ribbon", filled=True), "Sealed sleeve"),
        ("slot", comp(cdm("sleeve_stencil"), tint(cdm("stencil_window"), (168, 32, 44)),
                      tint(cdm("stencil_sticker_3"), (245, 245, 245))), "Sleeve Stencil"),
    ]], OUT / "sleeves.png")

    render("The three worktables", [
        [("big", LATHE_XL, "Cutting Lathe"), ("big", LATHE_D_XL, "…with a disc"),
         ("big", PRESS_XL, "Record Press")],
        [("big", PRESS_D_XL, "…freshly pressed"), ("big", PACK_XL, "Packaging Table")],
    ], OUT / "worktables.png")

    # Static copies kept in sync: the mod logo and the lathe editor skin.
    shutil.copyfile(ROOT / "src/main/resources/logo.png", OUT / "logo.png")
    shutil.copyfile(ROOT / "src/main/resources/assets/cdm/textures/gui/cutting_lathe.png",
                    OUT / "lathe_editor_skin.png")
    print("done")

if __name__ == "__main__":
    main()
