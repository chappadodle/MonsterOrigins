# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**Origin Mod Study** — an **experimental** Minecraft **1.20.1 / Fabric** addon for the
[Origins](https://modrinth.com/mod/origins) mod (a learning/hobby project). Adds four origins,
**Arachne** (a humanoid spider), **Medusa** (a gorgon), **Harpy** (a storm-wind bird-woman), and
**Siren** (a singer of the deep), as worked, documented examples of a data-driven pattern for
adding more origins later — see **TEMPLATE.md** for that pattern and its decision checklist. Each
plays differently on purpose (fragile/fast/poison, tanky/slow/petrify, aerial/fragile/knockback,
aquatic/support/crowd-control) so the pattern gets exercised against genuinely different design
directions, not reskins of the same kit. Harpy was the first origin needing real custom code
*beyond* a single mixin — a new status effect and a custom Apoli action type. Siren needed a
second custom status effect plus two more mixins (a generalized version of the same
friendly-mob-targeting trick Arachne's arthropods use), but turned out to be *more* data-driven
than expected once real Origins source was checked — see the gotchas below for both origins.

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
- **Vanilla Poison/Wither/Regeneration are not implemented via `MobEffect` subclassing — they're
  hardcoded by identity-check inside the shared base class.** Confirmed via `javap` on the actual
  compiled classes: `MobEffects.POISON` etc. are constructed as plain `new MobEffect(...)`
  (there's exactly one anonymous `MobEffect` subclass in the whole vanilla package, and it's Bad
  Omen's raid-triggering logic, not Poison's). The real tick-damage logic — `if (this ==
  MobEffects.POISON) { ... damage 1, capped so it can't kill ... }` — lives directly inside
  `MobEffect.applyEffectTick()`/`isDurationEffectTick()`, comparing by reference against the
  literal vanilla singleton. **This means a brand-new custom effect gets none of that behavior for
  free, even if constructed identically** — `BleedMobEffect` (`effect/BleedMobEffect.java`)
  exists specifically to reproduce Poison's exact real formula (interval `25 >> amplifier`, 1
  damage via `damageSources().magic()`, capped at 1 HP) as actual subclass overrides. The same
  hardcoding pattern applies to Poison's undead immunity (`LivingEntity#canBeAffected` only
  special-cases the literal vanilla Poison/Regeneration objects) — a new effect doesn't inherit
  that either; enforce it at the application site instead (see `talons.json`'s `target_condition`
  excluding the undead entity group), the same pattern Arachne's own on-hit poison already uses.
- **Some things genuinely have no data-driven path in Origins/Apoli, and it's worth actually
  checking rather than assuming a workaround exists.** Harpy's Scream needed "knock back only
  what's in front of the caster." `origins:relative_rotation` looked like the answer but (read its
  actual Java source, not just the doc's natural-language description) compares two entities' own
  independent facing directions to each other, not "is the target positioned in front of the
  actor's position" — the wrong computation entirely. `area_of_effect`'s `shape` field is only
  `cube`/`star`/`sphere`, no cone. `add_velocity` only takes a fixed vector, no "radiate outward
  from caster" mode. All three needed checking against real source/docs before concluding none of
  them fit — see `power/ScreamConeAction.java` for the resulting custom Apoli
  `EntityActionType` registration (a dot-product cone check + per-target outward knockback), the
  correct escalation once the data-driven options are actually exhausted, not just assumed absent.
- **A thrown trident-type item's mid-air visual is hardcoded, completely separate from its item
  model — fixed for the Harpy Javelin with a dedicated entity type + renderer, not worked around.**
  Decompiled (via a locally-fetched CFR jar, since Loom's cache had no Minecraft sources jar — see
  "Build / verify" below for how) `TridentItem.releaseUsing` and `ThrownTridentRenderer` directly
  rather than assume: `releaseUsing` always constructs `new ThrownTrident(level, player,
  itemStack)` — no override point for a different thrown-entity type — and `ThrownTridentRenderer`
  doesn't even look at the carried itemstack for its visual: it renders a dedicated vanilla
  `TridentModel` baked from `ModelLayers.TRIDENT` with a hardcoded `textures/entity/trident.png`,
  full stop. Any plain `TridentItem` subclass (Petrifying Trident) is stuck with that — its custom
  model only shows in-hand/inventory/equipped-on-back, never in flight. `HarpyJavelinItem` instead
  overrides `releaseUsing` (a faithful reproduction of the real decompiled method, since there's no
  extension point to build on top of) to spawn `ThrownJavelin` — a real second `EntityType`
  extending `ThrownTrident` (see `entity/ThrownJavelin.java`) — with its own client renderer
  (`client/ThrownHarpyJavelinRenderer.java`) extending vanilla's `ThrownItemRenderer`, the same
  base class used for snowballs/eggs/ender pearls, which renders the entity's own `ItemStack` via
  `ItemRenderer` instead of a hardcoded model. No third-party rendering library needed or used —
  confirmed by web search that this is exactly the mechanism the Fabric wiki's own "Creating a
  Custom Projectile" tutorial teaches, not a gap that needs a dependency to fill.
- **Vanilla's real on-hit damage for a thrown trident bypasses `ItemStack.hurtEnemy` entirely.**
  Also found by decompiling rather than assuming: `ThrownTrident.onHitEntity` calls
  `entity.hurt(damageSource, f)` directly — `hurtEnemy` on the `TridentItem`/subclass is *only*
  ever invoked for a melee swing (`LivingEntity.doHurtTarget`'s own code path). A weapon that
  wants its bonus on-hit behavior (Harpy Javelin's Bleed + airborne bonus damage) to also apply to
  a *thrown* hit needs a second mechanism entirely — `ThrownTridentMixin` handles this via
  `@ModifyArg` on the `entity.hurt(...)` call (to add bonus damage) and a TAIL `@Inject` (to apply
  Bleed), both gated on `tridentItem.getItem() instanceof HarpyJavelinItem`.
- **`@Shadow` a method only when it's actually declared on the mixin's target class — inherited
  superclass methods don't need it and are safer reached by a plain cast.** `ThrownTrident`'s
  `getOwner()` is real but declared on `Projectile` (a superclass), not on `ThrownTrident` itself
  (confirmed via `javap`). Rather than risk a `@Shadow` stub against a method that isn't literally
  on the target class, `ThrownTridentMixin` casts `this` to the real, already-compiled `Projectile`
  type (`((Projectile) (Object) this).getOwner()`) and calls the already-public method directly —
  always valid at runtime since `ThrownTrident` genuinely *is* a `Projectile`, and it sidesteps any
  uncertainty about how far up the hierarchy Mixin's shadow resolution actually looks.
- **No Minecraft sources jar was cached, so `javap` alone couldn't show method bodies — a locally
  downloaded CFR jar (`org.benf:cfr` from Maven Central, a well-known public Java decompiler)
  filled that gap.** `javap -p` on classes extracted from Loom's mapped jar (the project's
  long-standing verification technique) only gives signatures, which was enough for every prior
  gotcha in this file but not for understanding what `TridentItem.releaseUsing`/`ThrownTrident
  .onHitEntity`/`ThrownTridentRenderer.render` actually *do* internally. Running CFR against the
  same extracted `.class` files produced real (if variable-name-mangled) Java source, same
  Mojmap signatures throughout since it decompiles the already-remapped classes — reusable for any
  future gotcha investigation that needs method bodies, not just signatures.
- **Registering into Apoli's own registries (not just Origins') needs Apoli itself as a compile
  dependency, and its POM pulls in more than you'd expect.** `ApoliRegistries`/`ActionFactory`
  (used by `ScreamConeAction`) live in Apoli's own package, so `com.github.apace100:apoli` had to
  be added as `modCompileOnly` — every prior custom-code piece in this project only ever needed
  Origins' classes. Apoli's published POM transitively pulls in Cloth Config, Mod Menu, and
  Reach Entity Attributes (its optional config-screen/attack-range integrations), none of which
  this project has any other reason to depend on or host a Maven repo for. Excluding those three
  modules directly on the dependency declaration (`exclude group: ..., module: ...`) resolved it
  cleanly — simpler and more robust than chasing each transitive dependency's own Maven host one
  repository at a time, and the compile still succeeds because `ActionFactory`/`ApoliRegistries`
  don't actually reference those excluded modules' types in their own public signatures.

- **`multiply_base` attribute modifier values are the *added* fraction, not the final
  multiplier — easy to get backwards, and I did, once, in this project's own planning doc.**
  Vanilla's `AttributeModifier.Operation.MULTIPLY_BASE` computes `final = base * (1 + value)`, so
  a value of `0.2` means +20% (matches Avian's real `tailwind` power, verified via `gh api`
  against `apace100/origins-fabric`). While planning Siren's swim speed, I initially read
  Merling's own `swim_speed` power (`value: 1.5`) as "exactly the 1.5x the user asked for" —
  wrong: a `multiply_base` value of `1.5` actually gives `base * 2.5`, not `base * 1.5`. Caught
  and corrected during implementation (not left in the shipped power) by re-deriving the formula
  directly rather than trusting the earlier plan-time claim. The lesson: when a requested multiplier
  is "N times normal," the JSON value for `multiply_base` is `N - 1`, not `N` — worth
  double-checking every time, since the mistake is easy to make and easy to miss in review.
- **Origins' own `swim_speed` power depends on a third `additionalentityattributes:water_speed`
  attribute (from the separate "Additional Entity Attributes" mod, already present at runtime via
  Origins' own jar-in-jar) — and it's computed *from* the current `generic.movement_speed` value
  every tick, not independently.** Decompiled `AdditionalEntityAttributes`' own
  `LivingEntityMixin` directly: its `@ModifyArg` on `LivingEntity.travel()` takes the
  already-computed land-speed float (`original`, which already reflects any `generic
  .movement_speed` modifiers), sets that as `water_speed`'s *base* value, then applies
  `water_speed`'s own modifiers on top. Consequence: a flat land-speed penalty and a flat
  swim-speed bonus **multiply together** rather than acting as two independent numbers — Siren's
  `land_slowness.json` (0.8x land) and `swim_speed.json` had to have the water value corrected
  to `0.875` (not the naively-expected `0.5` for "1.5x") specifically to cancel out the land
  penalty's bleed-through and land on the literal "1.5x swim / 0.8x walk" the user asked for.
  Verify this kind of cross-attribute interaction from source before assuming two numbers are
  independent, especially when a third-party attribute is involved.
- **Some Origins/Apoli power types support a generic top-level `"condition"` field, but only if
  their factory explicitly opts in via `.allowCondition()` — it's not universal across every power
  type the way `"inverted"` is universal across every *condition* type.** Checked
  `AttributePower.java` (no `.allowCondition()` call) vs. `DamageOverTimePower.java`/
  `ConditionedAttributePower.java` (both call it) directly rather than assume a plain
  `origins:attribute` power could be conditionally gated — it can't; a conditional attribute needs
  the dedicated `origins:conditioned_attribute` power type instead (not used in this project yet,
  since Siren's land/water speed split was solved by correcting the numbers instead — see above —
  rather than introducing a power type with no confirmed real-world JSON example to crib from).
- **A base Origins origin can be the fastest path to verifying a whole cluster of requirements at
  once.** Before writing any of Siren's aquatic powers, checking `apace100/origins-fabric`'s own
  `merling.json` (a real, already-shipped aquatic origin) directly answered "does Origins have a
  data-driven way to do underwater breathing/vision/mining speed" in one search, instead of
  guessing at power type names one at a time. Worth checking whether a existing base-mod origin
  already covers a requested theme before designing power-by-power from scratch.
- **`origins:damage_over_time`'s `onset_delay` is a continuous-condition timer with a small
  reset-grace window, not a one-shot delay — verified from `DamageOverTimePower.java`'s actual
  tick logic, not assumed from the field name.** It only starts dealing damage once its
  `condition` has been true for `onset_delay` ticks *in a row*, and the whole counter resets to
  zero if the condition goes false for more than 20 ticks straight (a deliberate 1-second grace
  buffer against flicker, not an accident). This is exactly what Siren's `dehydration.json` needed
  for "5 minutes out of water" — brief water contact doesn't fully interrupt an already-dry spell
  within that grace window, but stepping back into water for real does reset the clock. Confirmed
  reusable "no water for gills" damage type/death message already exists in base Origins
  (`origins:no_water_for_gills`), used as-is rather than defining a new one.
- **Guardian's laser attack still goes through the same `TargetGoal#canAttack` choke point every
  other hostile mob's targeting does — verified by decompiling `Guardian.registerGoals()`
  directly, not assumed from its unusual attack animation.** It adds a real
  `NearestAttackableTargetGoal` (a `TargetGoal` subclass) at `targetSelector` priority 1 for
  target *acquisition*; the laser beam itself (`GuardianAttackGoal`) only ever fires against a
  target already acquired that way. This meant generalizing `ArthropodPassiveTargetMixin`'s
  technique to Siren's friendly-sea-creatures requirement (which explicitly includes Guardians)
  needed no special-casing at all — the existing injection point already covers it.
- **A vanilla `ArmorItem` subclass's own item model has no effect on how it looks *worn on the
  body*.** Confirmed via `javap`/decompilation the same way as every other custom-code piece in
  this project: the in-hand/inventory appearance comes from the item's own model+texture (fully
  custom, same technique as every other item here), but the actual 3D-worn appearance is
  determined by the `ArmorMaterial` passed to the constructor (`SirenCrownItem` reuses
  `ArmorMaterials.DIAMOND`) via vanilla's own per-material armor layer texture system — a
  genuinely different, separate texture pipeline this project hasn't touched. `SirenCrownItem`
  deliberately doesn't attempt to override this (same proportionate-scope reasoning as the Harpy
  Javelin's mid-flight-visual decision) — the crown has its own custom icon, but renders as a
  generic diamond helmet when actually worn.

- **`SwordItem`'s real damage formula is `attackDamageModifier + tier.getAttackDamageBonus()`,
  added directly as the tooltip-visible attribute value** — confirmed by decompiling `SwordItem`
  and `Tiers` together (`Tiers.DIAMOND` bonus `3.0`, `Tiers.NETHERITE` bonus `4.0`), used to land
  Venomfang and Widowfang on exact requested/round damage numbers (6 and 7) rather than guessing a
  modifier and checking in-game.
- **A vanilla item's tooltip is a real, simple override (`Item#appendHoverText`), not an Origins/
  Apoli mechanism** — confirmed via `javap`. `OriginUtil.addOriginGatedTooltip` is a tiny shared
  helper so every origin-gated weapon's "who this is really for" line stays visually consistent
  (gray italic) without repeating `Component`/`ChatFormatting` boilerplate in each item class.
- **A real vanilla `smithing_transform` recipe (`template`/`base`/`addition`/`result`) doesn't
  require its `template` slot to hold an actual vanilla smithing template item** — read directly
  from the game's own `data/minecraft/recipes/netherite_sword_smithing.json` (extracted from the
  same mapped jar already used for `javap`, no `gh api` needed since vanilla's own data is
  bundled in it) rather than assumed. Widowfang's recipe repurposes that slot for a Golden Spider
  Eye instead of a smithing template — mechanically just a third ingredient slot, no special
  unlock/advancement gating attached the way vanilla's real template item has.
- **A real data-driven toggle (press once to turn an effect on, press again to turn it off) is
  possible without any custom Java, but needs `origins:if_else` + `origins:status_effect`, not a
  dedicated "toggle" power type** — `origins:toggle_night_vision` turned out to be a one-off power
  specific to night vision, not a generic pattern (confirmed via `gh api`, no generic sibling
  power exists). Siren's Dolphin's Grace toggle instead checks `origins:status_effect` (does the
  caster currently have `minecraft:dolphins_grace`) inside `origins:if_else`, then either
  `origins:clear_effect`s it or re-`apply_effect`s it with a very long duration — the effect's own
  presence/absence *is* the toggle state, no extra tracking needed.
- **`origins:apply_effect`'s status effect data supports `is_ambient`/`show_particles`/
  `show_icon` fields, not just `effect`/`amplifier`/`duration`** — found by reading Calio's real
  `SerializationHelper.readStatusEffect` directly rather than assuming the effect JSON shape was
  already fully known from earlier use. `show_particles: false` is what actually suppresses the
  bubble-trail spam from Siren's periodically-reapplied Water Breathing (and reused for Dolphin's
  Grace's own particles, for the same reason).

- **A vertical pixel-art silhouette doesn't read as a Minecraft weapon — the diagonal
  alternating-tone construction vanilla's own sword icons use is what actually sells it.** The
  Fang tiers' first pass (a vertical blade/guard/grip stack) was wrong despite being a clean,
  readable dagger *shape* in isolation — it didn't match the visual language every other tool/
  weapon icon in the game uses. Fixed by dumping vanilla's real `diamond_sword.png`/
  `iron_sword.png`/`netherite_sword.png` pixel grids directly (extracted from the mapped jar, same
  technique as every other texture in this project) and reusing their exact technique — a -45°
  diagonal blade alternating between a light and mid blade tone with a dark outline, a wider
  crossguard, then a handle/pommel — just with several of the blade's taper rows removed (and the
  guard/handle/pommel shifted to reconnect) to make it read as a short dagger instead of a full
  sword, rather than inventing a new pixel-art style from scratch.
- **`origins:modify_damage_taken` takes a `damage_condition` (an `origins:in_tag` check against a
  *damage type* tag, e.g. `minecraft:is_fire`), not an entity condition** — confirmed via a real
  working example (`more_kinetic_damage.json` in `apace100/origins-fabric`) before writing
  Arachne's and Harpy's fire-vulnerability powers. Vanilla's own `minecraft:is_fire` tag (`
  in_fire`, `on_fire`, `lava`, `hot_floor`, plus fireball projectile damage types) was reused
  directly rather than hand-listing fire-related damage types.

- **`hud_render`'s `bar_index`/`icon_index` are coordinates into Apoli's own shared
  `resource_bar.png` atlas, not per-power texture references — and that atlas only has real
  art for indices 0–8.** Extracted the actual texture straight from the Apoli jar (already a
  `modCompileOnly` dependency, no download needed) and sampled every row's pixel color
  programmatically rather than eyeballing it: indices 0–8 are distinct hand-drawn colors, and
  *everything from index 9 up to the texture's full 256px height* is solid placeholder magenta
  (`#d67fff`) — Apoli's own "unassigned slot" filler, not a missing-texture rendering failure.
  Several powers added across Medusa/Harpy/Siren had drifted past 8 (9, 10, 13, 15) simply by
  incrementing without checking real bounds, which is exactly what showed up as "purple texture
  and purple line" in-game. All `bar_index` values in this mod are now unique and within 0–8 —
  worth checking against this real limit (not just "must be a small number") whenever a new
  key-bound/cooldown power gets a `hud_render` block.

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

A second benign warning (`warning: unknown enum constant Env.CLIENT` /
`com.demonwav.mcdev.annotations.Env not found`) started appearing once this project added its
first client-only code (`client/OriginModStudyClient.java` and friends, for the Harpy Javelin's
renderer) — an IntelliJ/MCDev annotation-processor shim referenced by vanilla's own
`@Environment`-annotated classes, not something this mod's code triggers directly. Also expected,
also not a regression.

## Layout

- `src/main/java/com/example/originmodstudy/`
  - `OriginModStudy.java` — main init. Calls every registrar (`ModItems`, `ModEffects`,
    `ModSounds`, `ScreamConeAction`); the mixin needs no Java-side registration (declared in
    `arachne.mixins.json` instead).
  - `item/ModItems.java` — the real items this mod adds: `GOLDEN_SPIDER_EYE` (a craftable, edible
    carnivore-diet food), `ARACHNE_EYE`/`MEDUSA_EYE`/`HARPY_EYE`/`SIREN_EYE` (icon-only, no recipe,
    not in any creative tab — exist purely to give each origin a real picker icon instead of a
    borrowed vanilla item), `SILK` (a plain crafting material, no functionality yet), and
    `FANG`/`VENOMFANG`/`WIDOWFANG`/`PETRIFYING_TRIDENT`/`HARPY_JAVELIN` (craftable weapons —
    anyone can craft/swing any of them, but the poison/bleed/wither/petrify on-hit effect only
    triggers if the wielder has the matching origin, checked via `OriginUtil` in `hurtEnemy`; see
    `util/OriginUtil.java` below for why that's a hit-time check, not a recipe restriction — and
    `FangItem`'s own class doc for the three-tier progression). Every origin-gated weapon also
    overrides `appendHoverText` (via `OriginUtil.addOriginGatedTooltip`) so a player can tell
    who's-weapon-is-this from the tooltip alone. `HARPY_JAVELIN` is also this project's first item
    with a real custom 3D model (Blockbench-authored by the user, not a recolored flat icon like
    every other item here) — see `assets/arachne/models/item/harpy_javelin.json`. `SIREN_CROWN` is
    `SirenCrownItem` (below).
  - `item/SirenCrownItem.java` — Siren's exclusive armor: `+2` hearts via
    `getDefaultAttributeModifiers` (`HEAD` slot, same technique `HarpyJavelinItem` uses for
    `MAINHAND`) plus continuous Regeneration via `inventoryTick`, guarded by an actually-worn
    check. See the gotcha above for why its worn-on-body appearance is still a generic vanilla
    Diamond helmet regardless of its own custom icon.
  - `util/OriginUtil.java` — `hasOrigin(LivingEntity, ResourceLocation)`, the same
    `ModComponents.ORIGIN`/`OriginLayers` lookup `ArthropodPassiveTargetMixin` uses, pulled out so
    the two origin-gated weapons don't duplicate it. **Vanilla's `CraftingRecipe#matches` has no
    access to which player is crafting** (confirmed via `javap` on `Recipe`/`CraftingRecipe` in
    Loom's mapped jar) — a true recipe-level origin restriction would need a custom recipe type
    plus a way to identify the crafting player, real complexity for a "study" project. Gating the
    weapon's *effect* at hit-time instead is simpler, more reliable, and arguably the more correct
    place to enforce "whose weapon this is" for a melee weapon regardless.
  - `effect/BleedMobEffect.java`, `effect/ModEffects.java` — Harpy's Bleed status effect and its
    registration. See the gotcha above on why this needed to reproduce Poison's real tick logic
    from scratch rather than just subclass something.
  - `sound/ModSounds.java` — registers the `arachne:harpy_scream` and `arachne:mermaid_song` sound
    events; the actual audio lives in `assets/arachne/sounds/*.ogg` + `assets/arachne/sounds.json`
    (see CREDITS.md for both files' licenses).
  - `power/ScreamConeAction.java` — the one custom Apoli `EntityActionType` in this project
    (`arachne:cone_knockback`), registered into `ApoliRegistries.ENTITY_ACTION` directly rather
    than expressed in JSON. See the gotcha above for why nothing data-driven could do this.
  - `mixin/ArthropodPassiveTargetMixin.java` — the one custom-code *power* implemented as a mixin
    specifically (requirement: friendly arthropods) — distinct from the items/effect/action above,
    each of which is custom code for a different reason (real new content, or a registry Apoli
    itself is designed for addons to extend into).
  - `mixin/ThrownTridentMixin.java` — makes a thrown Harpy Javelin's on-hit damage go through the
    same Harpy-origin-gated Bleed/airborne-bonus rules a melee hit gets, since vanilla's real
    thrown-trident damage path (`ThrownTrident.onHitEntity`) completely bypasses `hurtEnemy`. See
    the gotchas above for the decompilation that found this.
  - `mixin/ThrownTridentAccessor.java` — a Mixin `@Accessor` interface exposing `ThrownTrident`'s
    private `tridentItem` field, since `ThrownJavelin` (below) needs to set/read it directly and
    nothing else can reach a private field from outside its own class.
  - `mixin/SeaCreaturePassiveTargetMixin.java`, `mixin/CharmedPassiveTargetMixin.java` — the same
    `TargetGoal#canAttack` suppression technique `ArthropodPassiveTargetMixin` uses, generalized
    for Siren: one keyed off a new entity-type tag (sea creatures, including Drowned/Guardian —
    see the Guardian AI gotcha above), one keyed off a new passive marker status effect
    (`ModEffects.CHARMED`, applied by Siren's Call) instead of a static tag. Kept as two separate
    files rather than one shared abstraction, matching this project's one-mixin-one-purpose
    convention.
- `src/main/java/com/example/originmodstudy/entity/`
  - `ThrownJavelin.java`, `ModEntities.java` — the dedicated entity type a thrown Harpy Javelin
    actually spawns as (registered with `EntityType.TRIDENT`'s own real size/tracking-range
    parameters), so its client renderer can draw the javelin's real item model instead of vanilla's
    hardcoded trident shape. See the gotcha above for why this was necessary at all.
- `src/main/java/com/example/originmodstudy/client/`
  - `OriginModStudyClient.java` — this mod's only client-only entrypoint (registered in
    `fabric.mod.json`'s `"client"` list, separate from the common `"main"` one), needed because
    `EntityRendererRegistry.register` doesn't exist on a dedicated server's classpath.
  - `ThrownHarpyJavelinRenderer.java` — renders `ThrownJavelin` via vanilla's `ThrownItemRenderer`
    (the same base class snowballs/eggs/ender pearls use), instead of writing a renderer by hand.
- `src/main/resources/data/arachne/`
  - `origins/arachne.json` — the origin: name, description, icon (`arachne:arachne_eye`), and its
    power list (16 entries as of this writing — a mix of references to base-Origins/Origins Minus
    power IDs and this addon's own custom powers).
  - `origins/medusa.json` — the second origin, tanky/petrify-focused (12 hearts, +6 armor,
    on-hit and AOE petrify, immune to her own petrify effects, weakened by direct sunlight).
    The real second worked example of the per-origin pattern — `example_stub.json` is still the
    minimal empty-file starting point, but Medusa is what a filled-in one actually looks like.
  - `origins/harpy.json` — the third origin, true flight + fragile + aerial skirmisher (8 hearts,
    permanent elytra-less flight, a flight-speed boost power, bare-fist bonus damage + the custom
    Bleed effect, a directional knockback scream). The one origin in this mod needing real custom
    code beyond a single mixin.
  - `origins/siren.json` — the fourth origin, aquatic support/crowd-control (breathes/sees/mines
    underwater with no penalty, a 5-minute out-of-water grace period before suffocating, a
    speed/land tradeoff, a real-audio buff-everyone/pacify-hostiles song, a Dolphin's Grace
    on/off toggle for the secondary key, every sea creature friendly). Turned out to be the *most*
    data-driven origin in this mod, despite looking like the most custom-code-heavy one going in —
    see the gotchas above for why (including the toggle, which needed no new Java at all).
  - `origins/example_stub.json` — TEMPLATE.md's worked-example starting point; deliberately not
    wired into the origin picker.
  - `powers/arachne/*.json`, `powers/medusa/*.json`, `powers/harpy/*.json`, `powers/siren/*.json` —
    each addon origin's own custom powers, one subfolder per origin (the convention TEMPLATE.md
    documents, to keep same-named powers across different origins from colliding). Each power has
    an inline `name`/`description` — Origins supports these as plain strings directly on the power
    JSON, so no separate lang file entries were needed for powers (items are different, see below).
  - `recipes/golden_spider_eye.json` — mirrors vanilla's real `golden_apple` recipe shape exactly
    (8 gold ingots around the center item), just swapping the center for a spider eye.
  - `recipes/trident.json` — a new global recipe for the vanilla trident (3 prismarine shards + 2
    sticks), craftable by anyone, not Siren-exclusive — same recipe-can't-see-the-player
    limitation documented under `util/OriginUtil.java` above.
  - `recipes/venomfang.json`, `recipes/widowfang.json` — Fang's tier-2 upgrade is a plain
    `minecraft:crafting_shaped`/shapeless recipe; tier-3 is a real `minecraft:smithing_transform`
    (verified against vanilla's own `netherite_sword_smithing.json`, extracted straight from the
    mapped jar — see the gotcha above), consuming the previous tier item as its `base` ingredient.
  - `tags/entity_types/enemies.json` — curated hostile-mob list, originally for Arachne's
    tracking-glow power, reused as-is by Medusa's Dreadful Presence/Stone Gaze Burst and Siren's
    Call (same mod/namespace, so cross-origin tag reuse is just a normal reference, not a hack).
  - `tags/entity_types/friendly_arthropods.json` — spider/cave_spider/silverfish/endermite (the
    vanilla arthropod grouping; bees deliberately excluded, they aren't in it).
  - `tags/entity_types/friendly_sea_creatures.json` — the vanilla aquatic mobs plus Drowned and
    Guardian/Elder Guardian (verified their targeting AI still routes through the same choke point
    this mixin technique intercepts — see the Guardian gotcha above).
  - `tags/items/raw_meat.json` — the raw-meat items (plus rotten flesh) Harpy's Hardy Stomach power
    checks against.
- `src/main/resources/assets/arachne/`
  - `lang/en_us.json` — display names for every real item. Items (unlike powers) always need a
    translation key; there's no inline-string option for them.
  - `textures/item/*.png` — every item texture except the Harpy Javelin is a *vanilla* texture
    (`spider_eye.png`, `ender_eye.png`, `feather.png`, `iron_sword.png`, `trident.png`,
    `string.png`, `diamond_helmet.png` — extracted from Loom's mapped Minecraft jar as needed) run
    through a Pillow luminance-remap script with a different color gradient per item — not
    hand-drawn art.
  - `sounds/harpy_scream.ogg`, `sounds.json` — Harpy's Scream audio (see CREDITS.md for license);
    the one non-recolored, non-code-generated asset in this project.
- `src/main/resources/data/origins/` — files here are **overriding/extending Origins' own
  namespace**, not this addon's:
  - `origin_layers/origin.json` — the merge file that actually adds Arachne, Medusa, Harpy, and
    Siren to the standard origin-picker GUI (see TEMPLATE.md §2 for why this path/format).
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
