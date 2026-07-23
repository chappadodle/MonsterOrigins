# Adding another origin

This mod ships one origin (**Arachne**) as a fully worked example of a repeatable, data-driven
pattern. This isn't a code generator — there's no script to run — it's a folder convention plus
a decision checklist for classifying each ability you want to add. Follow it and a new origin is
almost entirely JSON.

## 1. Folder convention

```
src/main/resources/data/<your_mod_id>/
  origins/<origin_id>.json                  — the origin itself: name, description, icon, powers
  powers/<origin_id>/<power_name>.json       — one file per custom power the origin needs
  tags/entity_types/<tag_name>.json          — any custom mob-grouping tags your powers reference
```

Arachne's own files under `data/monster_origins/` are the reference implementation of this layout —
`origins/arachne.json` and `powers/arachne/*.json` are the ones to read alongside this doc.

A power file is referenced from the origin's `"powers"` array as `<namespace>:<path>`, where
`<path>` is the file's path *relative to the `powers` folder, extension dropped* — not just the
filename. Because this repo nests each origin's powers under their own subfolder (so a second
origin's power files never collide by name with Arachne's), that path includes the subfolder:
`monster_origins:arachne/on_hit_poison` for `data/monster_origins/powers/arachne/on_hit_poison.json` (a real bug
in an earlier version of this file referenced these without the subfolder segment — the power
silently failed to resolve, so it just never applied; see `arachne.json`'s `"powers"` array for
the corrected form). You are **not**
limited to your own namespace — Origins itself, Origins Minus, or any other loaded Origins addon's
power IDs work exactly the same way; that's what lets `arachne.json` reference the base Origins
mod's own `origins:carnivore`, `origins:climbing`, and `origins:master_of_webs` powers directly
instead of reimplementing them.

## 2. The origin_layers merge trick

The world-join GUI and the Orb of Origin item both read from an **origin layer** — by default the
one at `origins:origin`. To add your new origin to that existing list without clobbering every
other addon's entries (including the base game's own Arachnid, Avian, Shulk, etc.), merge into it
with `"replace": false`:

```json
// src/main/resources/data/origins/origin_layers/origin.json
{
	"replace": false,
	"origins": [
		"<your_mod_id>:<origin_id>"
	]
}
```

This is a **data pack merge**, not a code registration — any number of mods can each ship a file
at this exact path and Origins concatenates all of their `"origins"` arrays together at load time.
See `data/origins/origin_layers/origin.json` in this repo for the real one wiring in Arachne.

An origin you *don't* want selectable yet (a work-in-progress stub, for instance) simply isn't
listed in any `origin_layers` file — see `data/monster_origins/origins/example_stub.json`, which exists
as a copy-paste starting point but is deliberately not merged into the picker.

## 3. The decision checklist

For each ability your new origin needs, work through these in order:

1. **Does a base-Origins power already do this?** Check the [power types
   docs](https://origins.readthedocs.io/en/latest/types/power_types/) and the vanilla origins'
   own power files (`data/origins/powers/*.json` in the
   [origins-fabric](https://github.com/apace100/origins-fabric) source) for something close
   enough. If so, just reference its ID directly — zero new files.
2. **Can it be configured from an existing power *type* with your own values?** Most abilities
   (stat changes, status effects, glow, night vision, diet restrictions, on-hit triggers) are one
   of Apoli's many generic power types with a `type` field and a handful of config fields — write
   a small JSON file, no Java.
3. **Does it need a command-driven integration with another mod (like Pehkui)?** Use
   `origins:action_on_callback` with an `origins:execute_command` entity action — see
   `data/monster_origins/powers/arachne/scale.json`.
4. **Only if 1–3 don't cover it: does it need real code?** Some things genuinely have no
   data-pack-only path — Arachne's own build hit exactly one: mob AI targeting has no generic
   Origins power to modify it (tracked as an
   [open feature request](https://github.com/apace100/origins-fabric/issues/144)), so
   `ArthropodPassiveTargetMixin.java` exists to suppress hostile-mob targeting for one specific,
   narrow case. Reach for a Fabric **mixin** (behavior hooks into vanilla/library code) before a
   custom Apoli `PowerFactory` (a whole new configurable power type) — a mixin is usually less
   code for a single fixed behavior, while a custom power type is worth it if you want the
   behavior to be *reconfigurable* from JSON like Arachne's other powers are.

Arachne's own requirements, classified this way, are the worked example:

| Requirement | Step that resolved it |
|---|---|
| Carnivore diet | 1 — reused `origins:carnivore` |
| Climb any surface | 1 — reused `origins:climbing` |
| Cobweb traversal + crafting | 1 — reused `origins:master_of_webs` |
| Night vision, health, armor | 2 — `origins:night_vision` / `origins:attribute` with our own values |
| Enemy tracking glow | 2 — `origins:entity_glow` with our own conditions |
| 50% scale | 3 — `origins:action_on_callback` → Pehkui's `/scale` command |
| On-hit poison | 2 — `origins:target_action_on_hit` with our own values |
| Arthropods passive until attacked | 4 — no data path exists; one mixin |

## 4. Mojmap ↔ Yarn note (only matters once you write step-4 custom code)

This project compiles against **Mojang's official mappings** (see `build.gradle`), same as this
author's other Fabric projects. Origins/Apoli's own docs, GitHub source, and community examples
are written in **Yarn** names. The two mapping sets name the same vanilla Minecraft classes and
methods differently, so when translating a Yarn code example into this project, swap names using
the pattern below (non-exhaustive — when in doubt, decompile the mapped Minecraft jar Loom already
downloaded and grep it, as was done to write `ArthropodPassiveTargetMixin.java`):

| Yarn | Mojang (this project) |
|---|---|
| `Identifier` | `ResourceLocation` |
| `World` | `Level` |
| `PlayerEntity` | `Player` |
| `LivingEntity` (same name, unaffected) | `LivingEntity` |
| `Registries` | `BuiltInRegistries` / `Registries` (context-dependent) |

Note this **only applies to vanilla Minecraft classes**. Third-party mod classes — everything
under `io.github.apace100.*`, `dev.onyxstudios.cca.*`, etc. — keep their exact original names
regardless of which Minecraft mapping your project uses; Loom only remaps references to
Minecraft's own obfuscated code, never another mod's own classes.

## 5. Compile-time-only dependencies

If your new power's Java code needs to call into Origins' own API (checking a player's origin,
reading `OriginComponent`, etc. — only relevant for step-4 custom code), you'll likely need
Cardinal Components API and Calio on the compile classpath too, even though Origins already
bundles both internally via jar-in-jar and provides them at runtime for free. See the
`modCompileOnly` dependencies and comments in `build.gradle` for why, and the exact Maven
coordinates/repositories that resolved them.
