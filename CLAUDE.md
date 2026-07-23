# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**Origin Mod Study** — an **experimental** Minecraft **1.20.1 / Fabric** addon for the
[Origins](https://modrinth.com/mod/origins) mod (a learning/hobby project). Adds two origins,
**Arachne** (a humanoid spider) and **Medusa** (a gorgon), as worked, documented examples of a
data-driven pattern for adding more origins later — see **TEMPLATE.md** for that pattern and its
decision checklist. The two are deliberately built as opposites (fragile/fast/poison vs.
tanky/slow/petrify) so the pattern gets exercised against two different design directions, not
just two reskins of the same kit.

## Critical environment facts (read before building)

- **Mappings are Mojang official** (`loom.officialMojangMappings()`), NOT Yarn — same convention
  as this author's other Fabric projects (e.g. `mythicarsenal`). Origins/Apoli's own docs and
  GitHub source are written in Yarn names; see TEMPLATE.md §4 for the translation table and why
  it only matters for vanilla Minecraft classes, never third-party mod classes.
- **JDK requirement is split:** Gradle needs **Java 21+ to run**, but the mod compiles for
  **Java 17** (`options.release = 17`). A JDK 17 will be *rejected* by Gradle; a JRE (no `javac`)
  fails with "does not provide JAVA_COMPILER". This environment ships only a JRE by default — a
  portable Temurin 21 JDK was downloaded to `~/.local/jdks/temurin-21` for build verification;
  point `JAVA_HOME`/`org.gradle.java.home` at a JDK 21+ before running Gradle.
- **Almost everything is data**, not Java. Arachne's own powers are 3 references to base Origins
  power IDs + 6 small custom power JSON files + one origin file + two entity-type tags — see
  `src/main/resources/data/arachne/`. Only one requirement (arthropods staying passive until
  attacked) needed real code, because Origins has no data-driven way to modify mob AI targeting
  (tracked upstream as `apace100/origins-fabric#144`, still open).
- **Compile-time-only dependencies for the one custom-code power:**
  `ArthropodPassiveTargetMixin.java` calls Origins' own `OriginComponent` API, which pulls in
  Cardinal Components API and Calio — both are already embedded inside Origins' published jar via
  Fabric's jar-in-jar mechanism (so they're present at runtime for free), but **not** exposed to
  javac from that jar, so they're declared separately as `modCompileOnly` in `build.gradle` from
  their own Maven repos (Ladysnake's maven for Cardinal Components, JitPack for Calio — see the
  repo comments for exactly why each one was needed and how those coordinates were found).
- **Powers under `powers/<origin_id>/` need the subfolder in their ID.** A file at
  `data/arachne/powers/arachne/foo.json` is referenced as `arachne:arachne/foo`, not
  `arachne:foo` — Origins resolves power IDs as a literal file path relative to the `powers`
  folder. Got this wrong once already (all six custom powers silently failed to apply until
  fixed); see TEMPLATE.md §1.
- **`origins:execute_command` runs Pehkui's `/scale` command with a subtlety that broke it
  twice.** (1) Argument order is `scale <operation> <scale_type> <value> [<targets>]` — verified
  directly from `ScaleCommand.java` in Pehkui's own source, not from docs/forum posts, several of
  which describe it wrong. (2) `<scale_type>` needs an explicit `pehkui:` namespace
  (`pehkui:base`, not `base`) — `ScaleTypeArgumentType` parses it as a plain Minecraft
  `Identifier`, which defaults an unqualified word to the `minecraft:` namespace and then fails
  to find it in Pehkui's registry. The sibling `operation` argument (`set`/`add`/...) *does*
  default to Pehkui's own namespace when unqualified, which is what made this so easy to miss —
  the two arguments look symmetric in a command string but are parsed by different argument
  types with different fallback behavior. Both bugs failed *silently*: no in-game error, no
  crash, the command just never took effect. (3) Separately, `entity_action_added` only fires on
  world join/leave, not on choosing the origin — `entity_action_gained`/`entity_action_lost` are
  needed too so the effect applies immediately on selection, not just after a relog.
- **A power that silently does nothing might not be loading at all — check `logs/latest.log`
  before touching the JSON further.** (Historical lesson from a "Latch On" climb-onto-another-
  player power, since removed at the user's request — kept here because the debugging lesson
  still applies to any future power.) It went through several plausible-looking fix attempts
  (wrong key, wrong `block` flag, a condition that seemed reasonable) that each changed real
  behavior but produced *zero* observable difference in-game. The actual cause was
  `origins:raycast`'s `distance` field: origins-docs states it's optional, but this project's
  pinned Origins/Apoli version requires it. Without it, the whole power failed a schema check and
  got skipped at data-load time — logged as `ERROR: There was a problem reading power file ...
  (skipping)` in the client log, not shown to the player anywhere in-game. No amount of reasoning
  about key bindings or raycast mechanics would have found this; the log line named the exact
  field immediately. When a power "does nothing" and a plausible fix doesn't change that at all,
  check the load-time log before iterating further — it's a much faster path than re-deriving
  Apoli's internals from source.
- **Dependency versions are pinned to what's actually installed** in this machine's PrismLauncher
  test instances ("SOLO origin" and "1.20.1"), not just "whatever's newest" — see
  `gradle.properties` for the full list and reasoning.
- **The mixin's core trick needs no custom persistent state.** Vanilla `LivingEntity` already
  tracks `getLastHurtByMob()` on every entity; checking that against the potential target inside
  `TargetGoal#canAttack` gives "friendly until this specific mob is hit by this specific player,
  then hostile toward them" for free, without inventing any new NBT/Cardinal Components data.
- **A mixin targeting the wrong class is a hard crash, not a graceful skip — unlike a missing
  JSON field.** (Also from the since-removed "Latch On" feature — `RidingOffsetMixin` no longer
  exists in this repo, but the lesson applies to any future mixin.) It targeted `Player.class` for
  `getPassengersRidingOffset()`, a method actually declared on `Entity` and never overridden by
  `Player`. Missing JSON fields get logged and skipped; a mixin injector that can't find its
  target method in the specified class fails the whole mixin apply pass, which crashes Minecraft
  at launch (well before any of this mod's own content is reachable). Verify a mixin's target
  method is actually declared in the class you're mixing into — `javap` on the class straight out
  of Loom's own mapped Minecraft jar (`.gradle/loom-cache/minecraftMaven/...`) settles it directly,
  same as every power-type field in this project gets checked against real source before shipping.
- **`bientity_condition` has no `and`/`or` combinator.** `origins:and`/`origins:or` exist for
  plain entity conditions (`meta_condition_types`) but there's no equivalent
  `meta_bientity_condition_types` — confirmed by checking what condition-type directories
  origins-docs actually has. Wanting two independent bientity checks at once (e.g. "target has a
  tag" *and* "actor can see target") needs restructuring, not a combinator; Medusa's Dreadful
  Presence power dropped the `can_see` requirement rather than fight this, since a modest radius +
  tag filter delivers the same "nearby hostiles" flavor without it.

## Build / verify

```bash
JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew build   # compile + build mod jar -> build/libs/
```

`runClient` needs a display and hasn't been run in this environment — in-game verification (does
the origin appear in the picker, do the powers behave as specified, does the mixin correctly
suppress/restore arthropod hostility) is a manual step: drop the built jar into either
PrismLauncher instance's `mods/` folder alongside the already-installed Origins/Origins
Minus/Pehkui/Fabric API jars.

JSON-lint new data pack files before building:
```bash
python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]"
```

One benign warning appears on every clean build: Loom's `remapSourcesJar` step fails to remap
`ArthropodPassiveTargetMixin.java` specifically (a `Mercury`/ECJ source-remapper limitation,
unrelated to the pattern-matching `instanceof` avoided in that file) — this only affects the
auxiliary `-sources.jar`'s IDE-navigation copy of that one file, not compilation or the real mod
jar. `BUILD SUCCESSFUL` with this warning present is expected, not a regression.

## Layout

- `src/main/java/com/example/originmodstudy/`
  - `OriginModStudy.java` — main init. Calls `ModItems.registerModItems()`; the mixin needs no
    Java-side registration (declared in `arachne.mixins.json` instead).
  - `item/ModItems.java` — the real items this mod adds: `GOLDEN_SPIDER_EYE` (a craftable, edible
    carnivore-diet food), `ARACHNE_EYE`/`MEDUSA_EYE` (icon-only, no recipe, not in any creative
    tab — exist purely to give each origin a real picker icon instead of a borrowed vanilla
    item), and `FANG`/`PETRIFYING_TRIDENT` (craftable weapons — anyone can craft/swing either,
    but the poison/petrify on-hit effect only triggers if the wielder has the matching origin,
    checked via `OriginUtil` in `hurtEnemy`; see `util/OriginUtil.java` below for why that's a
    hit-time check, not a recipe restriction).
  - `util/OriginUtil.java` — `hasOrigin(LivingEntity, ResourceLocation)`, the same
    `ModComponents.ORIGIN`/`OriginLayers` lookup `ArthropodPassiveTargetMixin` uses, pulled out so
    the two origin-gated weapons don't duplicate it. **Vanilla's `CraftingRecipe#matches` has no
    access to which player is crafting** (confirmed via `javap` on `Recipe`/`CraftingRecipe` in
    Loom's mapped jar) — a true recipe-level origin restriction would need a custom recipe type
    plus a way to identify the crafting player, real complexity for a "study" project. Gating the
    weapon's *effect* at hit-time instead is simpler, more reliable, and arguably the more correct
    place to enforce "whose weapon this is" for a melee weapon regardless.
  - `mixin/ArthropodPassiveTargetMixin.java` — the one custom-code *power* (requirement: friendly
    arthropods) — distinct from the two items above, which are custom-code for a different reason
    (real new content, not a power Origins/Apoli has no data-driven path for).
- `src/main/resources/data/arachne/`
  - `origins/arachne.json` — the origin: name, description, icon (`arachne:arachne_eye`), and its
    power list (16 entries as of this writing — a mix of references to base-Origins/Origins Minus
    power IDs and this addon's own custom powers).
  - `origins/medusa.json` — the second origin, tanky/petrify-focused (12 hearts, +6 armor,
    on-hit and AOE petrify, immune to her own petrify effects, weakened by direct sunlight).
    The real second worked example of the per-origin pattern — `example_stub.json` is still the
    minimal empty-file starting point, but Medusa is what a filled-in one actually looks like.
  - `origins/example_stub.json` — TEMPLATE.md's worked-example starting point; deliberately not
    wired into the origin picker.
  - `powers/arachne/*.json`, `powers/medusa/*.json` — each addon origin's own custom powers, one
    subfolder per origin (the convention TEMPLATE.md documents, to keep same-named powers across
    different origins from colliding). Each power has an inline `name`/`description` — Origins
    supports these as plain strings directly on the power JSON, so no separate lang file entries
    were needed for powers (items are different, see below).
  - `recipes/golden_spider_eye.json` — mirrors vanilla's real `golden_apple` recipe shape exactly
    (8 gold ingots around the center item), just swapping the center for a spider eye.
  - `tags/entity_types/enemies.json` — curated hostile-mob list, originally for Arachne's
    tracking-glow power, reused as-is by Medusa's Dreadful Presence and Stone Gaze Burst (same
    mod/namespace, so cross-origin tag reuse is just a normal reference, not a hack).
  - `tags/entity_types/friendly_arthropods.json` — spider/cave_spider/silverfish/endermite (the
    vanilla arthropod grouping; bees deliberately excluded, they aren't in it).
- `src/main/resources/assets/arachne/`
  - `lang/en_us.json` — display names for the two real items. Items (unlike powers) always need a
    translation key; there's no inline-string option for them.
  - `textures/item/*.png` — every item texture is a *vanilla* texture (`spider_eye.png` for the
    food and Arachne's icon, `ender_eye.png` for Medusa's icon — extracted from Loom's mapped
    Minecraft jar) run through a Pillow luminance-remap script with a different color gradient per
    item (gold, violet, stone-green) — not hand-drawn art.
- `src/main/resources/data/origins/` — files here are **overriding/extending Origins' own
  namespace**, not this addon's:
  - `origin_layers/origin.json` — the merge file that actually adds Arachne and Medusa to the
    standard origin-picker GUI (see TEMPLATE.md §2 for why this path/format).
  - `powers/master_of_webs.json` — a full-content override (via `loading_priority`) of Origins'
    own `master_of_webs` power, changing only the on-hit cobweb cooldown (120 → 40 ticks). If
    Origins ever changes that power's structure upstream, this override goes stale silently — no
    build-time way to detect that from here.
  - `tags/items/meat.json` — additive (default tag-merge behavior, not an override) — just adds
    `arachne:golden_spider_eye` to Origins' existing meat list.

## Conventions & gotchas

- Only commit when asked. Build artifacts (`build/`, `.gradle/`, `run/`) are gitignored.
- When adding a new custom power JSON, verify its power-type schema against the real thing before
  guessing field names — either the [origins-docs](https://github.com/apace100/origins-docs)
  markdown source (more reliable than the rendered readthedocs site, which 404s on some paths) or
  a real power file from `apace100/origins-fabric`'s `1.20` branch via `gh api`. Every power type
  used in this repo was verified this way, not assumed from memory.
