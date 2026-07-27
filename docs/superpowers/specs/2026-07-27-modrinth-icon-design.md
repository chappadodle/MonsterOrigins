# Modrinth project icon design

## Context

The mod's Modrinth listing (`monster-origins`, currently pending approval) uses the Fang weapon
texture as its project icon — a leftover from when the mod only had the Arachne origin. Now that
it has four origins (Arachne, Medusa, Harpy, Mermaid), a spider-themed weapon icon no longer
represents the mod. This is a Modrinth-listing-only asset: it is unrelated to `fabric.mod.json`'s
`icon` field (`assets/monster_origins/icon.png`), which currently doesn't exist anywhere in this
repo despite being referenced — a separate, pre-existing gap not addressed by this spec.

## Design

**Composition**: a square canvas with a soft dark radial/vignette background (dark violet fading
to black), not tied to any single origin's own color. The four existing origin "Eye" item
textures — `arachne_eye.png`, `medusa_eye.png`, `harpy_eye.png`, `mermaid_eye.png`
(`src/main/resources/assets/monster_origins/textures/item/`) — are composited as four points of a
diamond (top/right/bottom/left), equal size, each with a slight glow/outline so they read as one
set rather than four unrelated icons pasted together. No text/wordmark — Modrinth already displays
the project name next to the icon.

**Resolution & technique**: built as native pixel art on a small grid (consistent with this
project's existing convention of remapped/recolored textures, not hand-drawn art), then upscaled
via nearest-neighbor to a 512×512 PNG. Comfortably fits Modrinth's accepted format list and 256KiB
size cap. Produced with a Python/Pillow script, the same tool this project already uses for its
item textures — pulling the four Eye PNGs directly rather than redrawing them.

**Output location**: `branding/modrinth_icon.png` in this repo (new top-level `branding/`
directory — this asset is not part of the mod's own resources/jar, so it doesn't belong under
`src/main/resources`). Version-controlled and reproducible via the generation script, rather than
a one-off file that only exists locally. Uploading it to the Modrinth listing itself remains a
manual step (no Modrinth API access from this environment).

## Testing / verification

Not gameplay-affecting — no in-game or build verification applies. Verification is visual: render
the generated PNG (e.g. open it, or describe pixel layout) and confirm the four eyes are
distinguishable, roughly equal in size/weight, and the whole image still reads clearly at a small
thumbnail size (Modrinth's browse grid shrinks icons significantly).

## Out of scope

- The missing in-game `assets/monster_origins/icon.png` (`fabric.mod.json` references a file that
  isn't in the repo) — a separate, pre-existing issue not part of this request.
- Uploading the finished PNG to the actual Modrinth listing — manual step, no tooling access here.
