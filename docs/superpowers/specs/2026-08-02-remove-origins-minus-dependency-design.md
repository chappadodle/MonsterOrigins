# Remove the Origins Minus dependency

## Context

This mod depends on Origins Minus for exactly one thing: Arachne's `origins_minus:rappel` power
(hold jump under a ceiling to climb via a clamped creative-flight-style ascend). We already
override that power's file in full (`data/origins_minus/powers/rappel.json`, via
`loading_priority`) to double its climb speed, per an earlier request this session — so the whole
power's real behavior already lives in our own repo, just still declared as an *override* of an
external mod's power rather than as our own native content.

The goal: stop requiring players and contributors to install Origins Minus at all. This means
porting Rappel into our own namespace as native content, and removing every trace of the
dependency from the build, the shipped mod metadata, and the docs.

Investigation this session confirmed the dependency's *only* real usage is this one power — a
separate wildcard condition elsewhere (`"power": "*:climbing_toggle"`, in `master_of_webs.json`)
looked like it might be another Origins Minus tie-in, but is actually inherited unchanged from
vanilla Origins' own `master_of_webs.json` (verified by extracting Origins' own jar), and no
`climbing_toggle` power exists anywhere in Origins Minus's jar either. Nothing else in this repo
references the `origins_minus` namespace.

## What changes

### 1. Rappel becomes native content, redesigned (not a verbatim port)

New file: `src/main/resources/data/monster_origins/powers/arachne/rappel.json` (a real, owned
power — no `loading_priority`, since nothing external is being overridden anymore). Referenced
from `arachne.json`'s power list as `monster_origins:arachne/rappel`, replacing
`origins_minus:rappel` in the exact same list position.

Kept from the original mechanic, since it's genuinely the point of the power and already tuned:
- `ascend`: `origins:creative_flight`, gated on not being exposed to sky and not being deep in
  water (same two conditions as before) — this is the actual "climb" behavior.
- `stop`/`slow`: `origins:modify_velocity` powers that zero horizontal drift and clamp vertical
  speed to ±0.4 blocks/tick (our already-doubled value from earlier this session) while
  creative-flying.
- Start/stop sound cues (`item.crossbow.shoot` on rising, `entity.item.break` on stopping) and the
  particle trail (`origins:spawn_particles`, a standard action already used directly on the power,
  not routed through anything external) — cheap, real player feedback for an ability with no other
  visual tell.
- The keybind toggle badge, pointed at a new lang key in our own namespace
  (`tooltip.monster_origins.rappel.toggle`) instead of Origins Minus's.

Dropped as vestigial or unrelated flourish, not core to "climb a wall":
- The `res` resource bar (0–1, rendered in the HUD but never drained by anything in the original
  power file or anywhere else in Origins Minus's data — confirmed by grepping its whole data
  folder for any power that modifies this resource; genuinely dead weight).
- The `affinity` +400% mining-speed bonus while creative-flying (unrelated to climbing; reads as a
  Weaver-kit-specific balance choice for Origins Minus's own economy, not something Arachne's kit
  needs).
- The summoned-snowball-projectile visual (`execute_command` summoning a tagged snowball with
  upward motion, meant to look like a shot climbing rope) — redundant with the particle trail the
  power already spawns directly on the entity; dropping it removes an `execute_command` dependency
  for a visual effect that's already covered another way.

### 2. Dependency removed from build & metadata

- `build.gradle`: remove the `modImplementation "maven.modrinth:origins-minus:${project.origins_minus_version}"` line (and its comment).
- `gradle.properties`: remove `origins_minus_version=2.4.0+1.20` and its comment.
- `src/main/resources/fabric.mod.json`: remove `"origins_minus": ">=2.4.0"` from `depends`.
- Delete `src/main/resources/data/origins_minus/` entirely (just the one override file, now
  unneeded).

### 3. Docs updated

- `README.md`: drop Origins Minus from the numbered install list (renumber the rest), update the
  credits paragraph to no longer list it as something this mod is "built against," and reword the
  Rappel bullet in the power list (it currently says "same as Origins Minus's Weaver origin" —
  keep that as a design-credit note, just phrased as historical inspiration rather than a live
  dependency).
- `CREDITS.md`: add a new entry crediting Origins Minus's Weaver origin as the original design
  inspiration for Rappel's mechanic, per the decision to keep attribution even after the dependency
  is gone. This file currently has no code/design-credit section (only an audio-assets section) —
  add a short new section for this.
- `TUTORIAL.md`: both mentions describe a beginner's dev environment as having "Origins, Origins
  Minus, Pehkui, and Fabric API" pre-installed and needed for building/running — update both to
  drop Origins Minus from that list, since a fresh contributor following this tutorial after this
  change genuinely won't need it installed anymore.
- `TEMPLATE.md`: its one mention is generic teaching material (explaining that a power ID can
  reference any loaded mod's namespace, using Origins Minus only as one example alongside base
  Origins) — not a dependency claim, and still true regardless of whether this project uses it.
  Left unchanged.

## Verification

- `python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]"` — lint every touched/new JSON file.
- `grep -rn "origins_minus" .` (excluding `.git`, `build/`, `.gradle/`, and the unrelated
  `.claude/worktrees/` leftover directory) should return zero matches once this is done, confirming
  no stray reference survives anywhere in tracked files.
- `JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew build` — confirm the mod still compiles and
  packages cleanly with the dependency removed (this also proves nothing else in the Java code was
  silently relying on an Origins Minus class/API — nothing currently does, per the investigation
  above, but the build is the real proof).
- This is data + doc + build-config only, no Java behavior change beyond what the JSON already
  encodes — no mixin, no new class. In-game verification (does Rappel still climb, at the doubled
  speed, with sound/particles, and does the game start at all without Origins Minus installed)
  needs a real playtest; this headless environment can't confirm it. Flag this explicitly when
  reporting completion.
