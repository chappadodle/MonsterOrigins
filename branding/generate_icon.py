#!/usr/bin/env python3
"""Generate the Modrinth project icon: four origin Eye icons arranged in a diamond
over a dark violet radial-vignette background, native pixel art upscaled to 512x512."""

import math
import os

from PIL import Image

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEXTURE_DIR = os.path.join(
    REPO_ROOT, "src", "main", "resources", "assets", "monster_origins", "textures", "item"
)
OUTPUT_PATH = os.path.join(REPO_ROOT, "branding", "modrinth_icon.png")

CANVAS_SIZE = 64
FINAL_SIZE = 512
EYE_DRAW_SIZE = 20
GLOW_COLOR = (170, 120, 220)  # light violet
BG_INNER = (42, 26, 61)  # dark violet
BG_OUTER = (8, 5, 12)  # near black

# (filename stem, center x, center y) — diamond points on the 64x64 canvas
EYE_PLACEMENTS = [
    ("arachne_eye", 32, 12),  # top
    ("harpy_eye", 52, 32),  # right
    ("mermaid_eye", 32, 52),  # bottom
    ("medusa_eye", 12, 32),  # left
]


def radial_background(size, inner, outer):
    bg = Image.new("RGB", (size, size))
    px = bg.load()
    cx = cy = (size - 1) / 2
    max_dist = math.hypot(cx, cy)
    for y in range(size):
        for x in range(size):
            dist = math.hypot(x - cx, y - cy) / max_dist
            dist = min(dist, 1.0)
            r = round(inner[0] + (outer[0] - inner[0]) * dist)
            g = round(inner[1] + (outer[1] - inner[1]) * dist)
            b = round(inner[2] + (outer[2] - inner[2]) * dist)
            px[x, y] = (r, g, b)
    return bg


def glow_layer(icon, color, canvas_size, cx, cy):
    """A soft colored halo the same shape as icon's alpha, offset by one pixel
    in each direction to fake a cheap outline/glow at pixel-art scale."""
    layer = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    alpha = icon.split()[3]
    glow_solid = Image.new("RGBA", icon.size, color + (140,))
    glow_solid.putalpha(alpha)
    half = icon.size[0] // 2
    for ox, oy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        layer.alpha_composite(glow_solid, (cx - half + ox, cy - half + oy))
    return layer


def main():
    canvas = radial_background(CANVAS_SIZE, BG_INNER, BG_OUTER).convert("RGBA")

    for stem, cx, cy in EYE_PLACEMENTS:
        icon = Image.open(os.path.join(TEXTURE_DIR, f"{stem}.png")).convert("RGBA")
        icon = icon.resize((EYE_DRAW_SIZE, EYE_DRAW_SIZE), Image.NEAREST)

        glow = glow_layer(icon, GLOW_COLOR, CANVAS_SIZE, cx, cy)
        canvas.alpha_composite(glow)

        half = EYE_DRAW_SIZE // 2
        icon_layer = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
        icon_layer.alpha_composite(icon, (cx - half, cy - half))
        canvas.alpha_composite(icon_layer)

    final = canvas.convert("RGB").resize((FINAL_SIZE, FINAL_SIZE), Image.NEAREST)

    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    final.save(OUTPUT_PATH, "PNG", optimize=True)

    size_kib = os.path.getsize(OUTPUT_PATH) / 1024
    print(f"Wrote {OUTPUT_PATH} ({FINAL_SIZE}x{FINAL_SIZE}, {size_kib:.1f} KiB)")


if __name__ == "__main__":
    main()
