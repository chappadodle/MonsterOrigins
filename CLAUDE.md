# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

**Monster Origins** (mod ID `monster_origins`; this repo's own local directory is still named
`origins-mod-study`, unrelated and not worth renaming) — a Minecraft **1.21.1 / Fabric** addon
(ported from an original 1.20.1 codebase — `JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew clean
build` reports `BUILD SUCCESSFUL` as of that port) for
the [Origins](https://modrinth.com/mod/origins) mod. Adds four origins,
**Arachne** (a humanoid spider), **Medusa** (a gorgon), **Harpy** (a storm-wind bird-woman), and
**Mermaid** (a singer of the deep), as worked, documented examples of a data-driven pattern for
adding more origins later — see **TEMPLATE.md** for that pattern and its decision checklist. Each
plays differently on purpose (fragile/fast/poison, tanky/slow/petrify, aerial/fragile/knockback,
aquatic/support/crowd-control) so the pattern gets exercised against genuinely different design
directions, not reskins of the same kit. Harpy was the first origin needing real custom code
*beyond* a single mixin — a new status effect and a custom Apoli action type. Mermaid needed a
second custom status effect plus two more mixins (a generalized version of the same
friendly-mob-targeting trick Arachne's arthropods use), but turned out to be *more* data-driven
than expected once real Origins source was checked — see the gotchas below for both origins.

## Critical environment facts (read before building)

- **Mappings are Mojang official** (`loom.officialMojangMappings()`), NOT Yarn — same convention
  as this author's other Fabric projects (e.g. `mythicarsenal`). Origins/Apoli's own docs and
  GitHub source are written in Yarn names; see TEMPLATE.md §4 for the translation table and why
  it only matters for vanilla Minecraft classes, never third-party mod classes.
- **JDK requirement (as of the 1.21.1 port):** both Gradle itself and the mod's own compile
  target need **Java 21+** (`options.release = 21`) — no more 17/21 split, since 1.21.1-era
  Fabric mods (confirmed from Origins' own `fabric.mod.json`, which declares `"java": ">=21"`)
  require 21 as their actual compile target too. A JRE (no `javac`) still fails with "does not
  provide JAVA_COMPILER" either way. This environment ships only a JRE by default — a portable
  Temurin 21 JDK is available at `~/.local/jdks/temurin-21`; point `JAVA_HOME`/
  `org.gradle.java.home` at it (or any JDK 21+) before running Gradle.
- **Almost everything is data**, not Java. Arachne's own powers are 3 references to base Origins
  power IDs + 6 small custom power JSON files + one origin file + two entity-type tags — see
  `src/main/resources/data/monster_origins/`. Only one requirement (arthropods staying passive until
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
  `data/monster_origins/powers/arachne/foo.json` is referenced as `monster_origins:arachne/foo`, not
  `monster_origins:foo` — Origins resolves power IDs as a literal file path relative to the `powers`
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
  `gradle.properties` for the full list and reasoning. Re-checked against Modrinth's API on
  2026-07-27 (player-facing concern: the README's version list read like a strict exact pin): all
  four (Fabric API, Origins, Origins Minus, Pehkui) were already the newest release covering
  1.20.1 at that date, so nothing needed bumping. Separately, `fabric.mod.json`'s own `depends`
  block was already minimum-version/wildcard (`>=`/`*`), not exact-pinned, so Fabric Loader already
  accepts newer releases than whatever's compiled against here — only the README's wording needed
  to say so. If a genuinely newer release exists next time this is checked, re-verify the same way
  (Modrinth's `GET /v2/project/<slug>/version?game_versions=["1.20.1"]&loaders=["fabric"]`, newest
  entry first) before bumping `gradle.properties`.
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
  against `apace100/origins-fabric`). While planning Mermaid's swim speed, I initially read
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
  swim-speed bonus **multiply together** rather than acting as two independent numbers — Mermaid's
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
  since Mermaid's land/water speed split was solved by correcting the numbers instead — see above —
  rather than introducing a power type with no confirmed real-world JSON example to crib from).
- **A base Origins origin can be the fastest path to verifying a whole cluster of requirements at
  once.** Before writing any of Mermaid's aquatic powers, checking `apace100/origins-fabric`'s own
  `merling.json` (a real, already-shipped aquatic origin) directly answered "does Origins have a
  data-driven way to do underwater breathing/vision/mining speed" in one search, instead of
  guessing at power type names one at a time. Worth checking whether a existing base-mod origin
  already covers a requested theme before designing power-by-power from scratch.
- **`origins:damage_over_time`'s `onset_delay` is a continuous-condition timer with a small
  reset-grace window, not a one-shot delay — verified from `DamageOverTimePower.java`'s actual
  tick logic, not assumed from the field name.** It only starts dealing damage once its
  `condition` has been true for `onset_delay` ticks *in a row*, and the whole counter resets to
  zero if the condition goes false for more than 20 ticks straight (a deliberate 1-second grace
  buffer against flicker, not an accident). This is exactly what Mermaid's `dehydration.json` needed
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
  technique to Mermaid's friendly-sea-creatures requirement (which explicitly includes Guardians)
  needed no special-casing at all — the existing injection point already covers it.
- **A vanilla `ArmorItem` subclass's own item model has no effect on how it looks *worn on the
  body*.** Confirmed via `javap`/decompilation the same way as every other custom-code piece in
  this project: the in-hand/inventory appearance comes from the item's own model+texture (fully
  custom, same technique as every other item here), but the actual 3D-worn appearance is
  determined by the `ArmorMaterial` passed to the constructor (`MermaidCrownItem` reuses
  `ArmorMaterials.DIAMOND`) via vanilla's own per-material armor layer texture system — a
  genuinely different, separate texture pipeline this project hasn't touched. `MermaidCrownItem`
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
  power exists). Mermaid's Dolphin's Grace toggle instead checks `origins:status_effect` (does the
  caster currently have `minecraft:dolphins_grace`) inside `origins:if_else`, then either
  `origins:clear_effect`s it or re-`apply_effect`s it with a very long duration — the effect's own
  presence/absence *is* the toggle state, no extra tracking needed.
- **`origins:apply_effect`'s status effect data supports `is_ambient`/`show_particles`/
  `show_icon` fields, not just `effect`/`amplifier`/`duration`** — found by reading Calio's real
  `SerializationHelper.readStatusEffect` directly rather than assuming the effect JSON shape was
  already fully known from earlier use. `show_particles: false` is what actually suppresses the
  bubble-trail spam from Mermaid's periodically-reapplied Water Breathing (and reused for Dolphin's
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
  Several powers added across Medusa/Harpy/Mermaid had drifted past 8 (9, 10, 13, 15) simply by
  incrementing without checking real bounds, which is exactly what showed up as "purple texture
  and purple line" in-game. All `bar_index` values in this mod are now unique and within 0–8 —
  worth checking against this real limit (not just "must be a small number") whenever a new
  key-bound/cooldown power gets a `hud_render` block.

- **The mod's technical ID went through two separate rename decisions, not one — worth knowing
  the history if old references turn up anywhere (a stray old-world save, an old link, etc).**
  First pass: display name only (`fabric.mod.json`'s `"name"` became "Monster Origins" while
  `"id"` stayed `arachne`), specifically to avoid breaking existing test worlds — the ID is baked
  into every single resource/data path and every item/origin/power identifier, so a full rename
  meant moving every folder and rewriting every namespaced string across the whole project. Second
  pass, once the user decided to actually cut an official release: did the full rename anyway
  (`arachne` → `monster_origins`, `id` field, both top-level `data/`/`assets/` folders via `git mv`
  to preserve history, every hardcoded `new ResourceLocation("arachne", ...)` in Java, the mixins
  config filename, and — easy to miss — every **lang key** too, since translation keys are
  namespace-prefixed the same as item IDs (`item.arachne.fang` → `item.monster_origins.fang`);
  missing that would have silently shown every item/effect/subtitle name as its raw untranslated
  key, the exact bug already documented above for Bleed's missing lang entry. Accepted
  consequence, explicitly: any world with this mod's items already in an inventory or an origin
  already selected now shows those as missing, since Minecraft saves reference the full
  namespaced ID. `settings.gradle`'s `rootProject.name` (its own comment literally says "should
  match your modid") drives the actual built jar's filename — it was still `originsmodstudy`
  after the ID rename until caught separately; the built jar is now `monster_origins-<version>.jar`.

- **A custom `MobEffect` needs its own icon texture and lang key, same as a custom item —
  neither is optional or auto-generated, and both were missing for Bleed (and Charmed) until
  this was reported as "the effect doesn't seem to apply."** Confirmed against vanilla's own
  `poison.png`/`wither.png` (18×18, `assets/<namespace>/textures/mob_effect/<path>.png`) that
  this is a plain convention-based lookup, no Java override needed — `BleedMobEffect`/the
  anonymous `CHARMED` effect never needed any icon-related code, just the missing asset files.
  Without the texture, the effect's HUD/inventory icon silently fails to render; without the lang
  key (`effect.<namespace>.<path>`), its name shows as the raw untranslated key
  (`effect.arachne.bleed`) instead of "Bleed" — both easy to mistake for "the effect isn't
  actually applying" when it's really just rendering badly. Ruled out the user's own "same tick"
  theory first by re-reading `on_hit_poison.json` (Arachne's innate Venomous Bite power) directly:
  it only ever touches `minecraft:poison`, so it has no mechanism to affect Bleed or Wither at
  all, applied by completely separate code (`FangItem.hurtEnemy`).

- **Vanilla's undead immunity (`LivingEntity#canBeAffected`) only actually special-cases Poison
  and Regeneration — not every harmful effect, and not Wither.** `FangItem`'s original blanket
  "skip everything if the target is undead" check (copied from the Poison-only precedent set by
  the original Fang) was accidentally also blocking Widowfang's Wither from ever reaching undead
  targets, which vanilla itself never actually blocks. Confirmed directly from
  `LivingEntity.canBeAffected`'s real body (`getMobType() != UNDEAD || (effect != REGENERATION &&
  effect != POISON)`) rather than assuming "undead = immune to everything harmful." Widowfang's
  Wither now applies to undead too; Poison/Bleed stay undead-gated as before. Separately,
  `WitherBoss` has its own `canBeAffected` override rejecting `MobEffects.WITHER` specifically —
  already enforced automatically by vanilla's own `addEffect`, so "except the Wither boss itself"
  needed no extra code at all once the undead over-blocking was fixed.

- **The fourth origin was renamed Siren → Mermaid after already shipping under the old name,
  same disruptive-but-thorough treatment as the mod ID rename, not a display-only patch.** Every
  file/folder touching it moved (`origins/siren.json` → `mermaid.json`,
  `powers/siren/` → `powers/mermaid/`, `sirens_call.json` → `mermaids_call.json`,
  `SirenCrownItem.java` → `MermaidCrownItem.java`, item IDs `siren_eye`/`siren_crown` →
  `mermaid_eye`/`mermaid_crown`), every namespaced JSON reference and lang key updated, and the
  origin's own path within the namespace changed too (`monster_origins:siren` →
  `monster_origins:mermaid`) — so, same as the mod ID rename, any world with this origin already
  selected will show it as missing now.

- **`texture_size` is a Blockbench-project-file convention, not a real vanilla model JSON field —
  vanilla always reads UV coordinates in a fixed 16-unit space, full stop.** Confirmed two ways
  before touching the Living Coral Trident's supplied model: `javap` on `FaceBakery` shows its UV
  interpolation hardcodes `16.0f` literals, never a variable texture size; and grepping every
  `.class` file in the entire client+server jar plus every vanilla model file bundled in it for the
  literal string `"texture_size"` returns zero matches anywhere. The file the user supplied
  (`~/Downloads/Mermaid trident.json`) was a raw Blockbench project save (`"format_version"`,
  `"groups"` — Blockbench-only keys), not an actual exported vanilla model, so its UV numbers were
  authored against Blockbench's own internal `texture_size:[32,32]` editing canvas. Shipped fix:
  every face's UV rectangle rescaled by `16 / texture_size[0]` (a plain unit conversion, not a
  guess) and the field itself dropped, since keeping a value vanilla silently ignores would only
  mislead a future reader. Whether the rescaled UVs land pixel-correct on the real supplied texture
  is a separate question this environment can't render an answer to — flagged for the user's own
  in-game check, same as every other visual asset in this project.
- **Vanilla 1.20.1 has no reach attribute at all — that was only added in Minecraft 1.21 — and
  Origins' own `extra_reach.json` power (checked via `gh api` before assuming a mixin was needed)
  depends entirely on the third-party Reach Entity Attributes mod's two attributes for its bonus.**
  That mod isn't in this project's dependency list (in fact `build.gradle` already deliberately
  excludes it as a transitive Apoli dependency) or the user's installed mod list. The Living Coral
  Trident's requested "+1 block reach" instead mixes into `MultiPlayerGameMode.getPickRange()`
  directly — decompiled to confirm it's a plain hardcoded `return isCreative() ? 5.0F : 4.5F`, the
  real client-side raycast range `GameRenderer#pick` uses to build the player's hit result every
  frame, driving both block and entity interaction. (Separately confirmed
  `ServerGamePacketListenerImpl.MAX_INTERACTION_DISTANCE`, a loose 6-block server-side sanity bound
  used in `handleUseItemOn`/`handleInteract`, is not the real per-gamemode reach limit and doesn't
  need touching — extending the client-side value is what actually changes the player's felt
  reach.) A mixin on vanilla's own constant was judged more proportionate than asking the player to
  install an entirely new mod for one weapon's minor stat, consistent with this project's existing
  bias (see the cobweb/climb-speed mixins above) toward a mixin over a new dependency whenever
  vanilla hardcodes the behavior being changed.

- **`@ModifyArg` handler parameters declare which argument(s) of the *invoked* call to modify —
  they are not a local-capture mechanism for the *enclosing* method's own parameters, unlike
  `@Inject`.** Learned the hard way: `MermaidTridentBonusDamageMixin`/`ThrownTridentMixin`'s Barbed
  Tip handlers originally appended an extra parameter (the melee target / `EntityHitResult`) to an
  `@ModifyArg` handler, assuming it worked like the enclosing-method parameter capture `@Inject`
  handlers already use successfully elsewhere in this file (e.g. `harpyJavelin$applyBleedOnThrow`).
  It doesn't — Mixin instead read the extra parameter as declaring the *invoked* method's expected
  signature, producing `InvalidInjectionException: ... targets a method with an invalid signature
  ..., expected (F, Entity)` against the real `Entity.hurt(DamageSource, float)`, which crashed the
  entire mixin transform of the target class (`Player`/`ThrownTrident`) at game launch, before any
  menu loads — confirmed from the user's own crash log, not caught at compile time, since Mixin's
  own annotation processor doesn't validate this. Fixed by adding a plain `@Inject` at
  `@At("HEAD")` that captures the enclosing method's own real parameter into a new mixin-added
  field, letting the `@ModifyArg` handler go back to its original, valid single-argument shape and
  read that field instead. MixinExtras (bundled by Fabric Loader itself per this project's own
  launch log) offers a `@Local` sugar annotation that could do this more directly, but its
  availability on this project's own compile classpath wasn't confirmed, and this environment has
  no display to verify a mixin fix by actually launching the game — the field-plus-HEAD-inject
  approach was chosen specifically because it only reuses a pattern already proven working
  elsewhere in this exact file, not because it's the only possible fix.

- **Vanilla's own `ThrownItemRenderer` (used for snowballs/eggs/ender pearls) billboards the item
  to face the camera and renders it with a fixed `ItemDisplayContext.GROUND` transform — it never
  applies the entity's own flight-direction rotation, unlike vanilla's real
  `ThrownTridentRenderer`.** Both are correct for what they're built for (a snowball has no "pointy
  end" that needs to face anywhere in particular), but the Harpy Javelin and Living Coral Trident
  originally reused `ThrownItemRenderer` anyway (to get the "render the item's own model instead of
  a hardcoded vanilla model" fix — see the entry below), which is why both always rendered
  "vertical" in flight and stuck in the ground, regardless of real throw direction — a genuine
  rendering bug, not a texture/model problem, confirmed by decompiling both renderer classes
  directly rather than assumed. Fixed with a new shared `client/DirectionalThrownItemRenderer.java`
  that extends `EntityRenderer<T>` directly and reproduces `ThrownTridentRenderer`'s exact pose
  transform (`Axis.YP.rotationDegrees(lerp(partialTick, yRotO, getYRot()) - 90)` then
  `Axis.ZP.rotationDegrees(lerp(partialTick, xRotO, getXRot()) + 90)`) before rendering the item's
  own baked model via `ItemRenderer.renderStatic` with `ItemDisplayContext.NONE` (raw model space,
  since the custom rotation already does the pointing job `GROUND`'s fixed transform would
  otherwise do). Both `ThrownJavelin` and `ThrownMermaidTrident` get this rotation data for free —
  it's computed every tick by the inherited `ThrownTrident`/`AbstractArrow` movement logic, the
  renderer just wasn't reading it.
- **Reusing vanilla's exact rotation offsets for a differently-authored model, and reusing
  vanilla's exact interpolation call, are two separate risks — real playtesting caught both.**
  After shipping `DirectionalThrownItemRenderer` above, the user reported the javelin/trident
  landing blade-up (shaft planted, not the blade) and a brief rapid spin right at impact. (1)
  Vanilla's `ThrownTridentRenderer` offsets (`yaw - 90`, `pitch + 90`) were tuned for vanilla's own
  baked `TridentModel`'s own local-space convention — nothing guarantees a completely
  separately-authored item model's own modeled tip happens to align with those same numbers, and it
  didn't; fixed with an extra 180° on the pitch term (`pitch - 90` instead of `+ 90`), the term
  that governs which end faces the direction of travel. Flagged as a best-effort correction based
  on the reported symptom, not something this headless environment can verify visually — if a
  future case needs the *other* direction, try the yaw term instead, or an added standalone
  `Axis.XP.rotationDegrees(180.0F)`. (2) Vanilla's own renderer interpolates `yRot`/`xRot` with
  plain `Mth.lerp`, confirmed by decompiling it again — this blends the two angle numbers directly,
  not the shortest angular path, so an angle jump across the 0°/360° wrap (plausible right at an
  impact bounce) makes it spin the long way around for that frame. `Mth.rotLerp` (confirmed present
  via `javap` on `Mth`) is vanilla's own angle-aware interpolation, already used elsewhere in
  vanilla for exactly this reason — just not by `ThrownTridentRenderer` itself, a real, minor
  pre-existing vanilla imperfection easy to miss on the small stock trident model but obvious on
  these mods' larger, more detailed ones. Lesson: decompiling vanilla and reproducing its technique
  exactly is the right first move, but "vanilla does it this way" doesn't mean vanilla's way is
  bug-free or tuned for a different asset — both still needed real gameplay feedback to catch.
- **`TextureAtlas.LOCATION_BLOCKS` is deprecated in this Minecraft version — use
  `InventoryMenu.BLOCK_ATLAS` instead, the real non-deprecated constant for the same resource
  location.** Caught via `-Xlint:deprecation` (not the default compiler output) while adding the
  `DirectionalThrownItemRenderer` above, which needs some placeholder texture location purely to
  satisfy `EntityRenderer`'s abstract `getTextureLocation` (never actually sampled, since the real
  texture comes through the rendered item model).

- **A vanilla block/item element's rotation angle is restricted to exactly {0, ±22.5, ±45} —
  confirmed directly from `BlockElement$Deserializer.getAngle`'s own real validation logic (a plain
  `if (angle != 0 && abs(angle) != 22.5 && abs(angle) != 45) throw JsonParseException`), not just
  the commonly-cited wiki rule.** A genuine `90°` single-axis rotation (e.g. a piece meant to face
  sideways) isn't approximable by snapping to the nearest allowed angle without visible distortion —
  the correct, lossless fix for an axis-aligned box is to recompute its `from`/`to` directly via the
  actual rotation formula (for -90° around Y: `(dx, dz) → (-dz, dx)` relative to the rotation
  origin, applied to both corners, then take per-axis min/max) and remap its face names to match
  (a -90° Y rotation turns north→east, east→south, south→west, west→north; up/down unaffected),
  rather than leaving a `rotation` block in place at all. Smaller stray angles (a handful of
  degrees off, not a full 90°) are a much safer case for a plain nearest-allowed-angle snap, since
  the visual difference is proportionally tiny.
- **A dict `.get(key, default)` fallback chain silently returns the wrong thing when an earlier key
  *exists* but holds a legitimately falsy value (here, a real `0`) — this cost a full round of
  wrong guidance to the user.** An early pass at auditing the Living Coral Trident model's rotation
  angles used `rot.get('angle', rot.get('z', rot.get('x', rot.get('y'))))` to pull "the" angle out
  of Blockbench's multi-axis rotation objects — for an element rotated only on `y` (e.g. a real
  genuine `y: -90`, with `x: 0, z: 0` also present in the object), this evaluated `rot.get('z', ...)`
  first, found the `z` *key* present (value `0`), and returned that `0` as "the angle," completely
  hiding the real, invalid `-90` on `y`. `.get()`'s default only ever fires when the key is
  *missing*, never when its value is falsy — checked each axis's actual value directly
  (`rot.get(ax, 0) != 0`) instead once this was caught. Worth remembering for any future ad hoc
  JSON-auditing script in this project: prefer explicit per-key value checks over a `.get()`
  fallback chain whenever a legitimate value of `0`/`False`/`""` is possible on an earlier key.

- **`origins:and`/`origins:inverted` combine freely with `origins:status_effect` and
  `origins:in_tag` inside a `bientity_condition`'s `target_condition` wrapper — the "no
  `meta_bientity_condition_types` combinator" gotcha above only blocks combining *bientity*
  conditions directly, not the plain entity conditions wrapped inside `target_condition`.** Used
  for the new passive Stone Gaze power's one-shot-per-target gate (hostile tag AND not already on
  cooldown) without needing to restructure around the missing bientity combinator the way Medusa's
  Dreadful Presence originally had to.
- **Origins/Apoli has a real, dedicated `origins:toggle` power type (checked via `gh api` before
  building Innocent Form) — but this project deliberately did *not* switch to it, staying with the
  already-proven `origins:active_self` + `origins:if_else` + status-effect-as-toggle-state pattern
  instead.** `origins:toggle` provides a genuine on/off state queryable via
  `origins:power_active`, which would have been the more "textbook" mechanism — but wiring up the
  actual `/scale` command side-effect on each transition still needs a *separate* action-triggering
  power referencing it (via the `origins:toggle` entity action type), a second unverified mechanism
  on top of the first. The existing marker-effect pattern (already proven for Dolphin's Grace)
  achieves the identical result — a queryable on/off state other powers can gate on via
  `origins:status_effect` — with zero new mechanisms to verify. Reused, not reinvented.
- **`LivingEntity.blockedByShield(LivingEntity attacker)` is vanilla's real, minimal hook for "a
  raised shield just blocked a melee hit"** — confirmed via `javap`/decompile: called on the
  defender with the attacker as its only parameter, and by default just applies a small knockback
  to the attacker. A TAIL `@Inject` here, checking `defender.getUseItem() instanceof
  SerpentAegisItem`, is the shield-equivalent of every other on-hit mixin in this project — and
  confirms the Serpent Aegis needed no custom block-detection path at all, just extending vanilla
  `ShieldItem` directly and injecting into the one real hook vanilla already calls when a block
  actually lands.
- **Iron golems have no existing AI goal this mod could gate/redirect for "attack this specific
  player" — confirmed by decompiling `IronGolem.registerGoals()` directly: both its
  `NearestAttackableTargetGoal`s are typed to `Mob.class`, and `Player` isn't a `Mob` subclass at
  all.** Vanilla's real "golems attack players who anger the village" behavior lives in the
  separate village-reputation/anger system, not a target goal this project could intercept the
  same way the `*PassiveTargetMixin` family intercepts `TargetGoal#canAttack`. Innocent Form's
  "iron golems see through the disguise" instead directly calls the already-public
  `Mob#setTarget(LivingEntity)` from a plain periodic Fabric `ServerTickEvents.END_SERVER_TICK`
  scan — simpler and more reliable than fighting to inject into a targeting system that doesn't
  actually reach players in the first place.
- **A full villager "flee and stay away" reaction needs 1.20.1's Brain/Sensor/MemoryModuleType
  system, not a simple AI goal — judged disproportionate scope for this project and not
  attempted.** Villager fear/panic behavior is driven by that Brain system, not the
  goal-based AI this project's other mob-behavior mixins already intercept successfully (iron
  golems, arthropods, sea creatures). Innocent Form's villager reaction settles for an immediate,
  repeatable knockback-away + vanilla's own `SoundEvents.VILLAGER_NO` on proximity (from the same
  periodic tick scan as the iron golem check above), plus an outright `mobInteract` cancellation
  (`VillagerNoTradeMixin`) so trading is blocked regardless — a proportionate-scope call matching
  this project's own precedent (see `util/OriginUtil.java`'s reasoning for a similar tradeoff).

- **A per-tick multiplicative velocity boost with nothing to cancel it back down compounds
  geometrically, not linearly — a real bug caught by reasoning through the math, not by observing
  it happen, since this environment has no display to playtest a runaway-acceleration bug with.**
  Building the "+50% flying speed" fixing-doc request, a first draft injected at the tail of
  `LivingEntity.travel()` and unconditionally multiplied the fall-flying entity's velocity by 1.5
  every tick. Vanilla's own fall-flying physics (decompiled directly, see the next entry) only
  reach a stable cruising speed because their own internal drag (a fixed 1-2% per tick) and their
  own direction-seeking pull are balanced by construction; bolting a flat >1.0 multiplier onto the
  *output* of that already-balanced system every tick, with no counteracting term, has nothing
  stopping it from compounding tick over tick (1.5x, 2.25x, 3.375x, ...) into an unbounded runaway.
  Fixed by clamping the boosted horizontal speed to a fixed ceiling every tick instead of letting
  the multiply apply freely — this guarantees the result can never exceed that ceiling regardless
  of how long a flight lasts, self-correcting even after the ceiling is hit (since `min(speed *
  1.5, ceiling)` only reapplies the multiplier meaningfully while under three-quarters of the
  ceiling). A flat per-tick multiplier on a physics quantity is a real hazard worth checking for
  arithmetically before shipping, any time neither side of the interaction can be observed running
  in this headless environment.
- **`LivingEntity.travel(Vec3)` is one large method with mutually exclusive water/lava/fall-flying
  branches, and `LivingEntity.getWaterSlowDown()` (a plain, protected, non-final method returning a
  flat `0.8f`) is the real per-tick horizontal momentum-retention constant applied to anyone in
  water — confirmed by decompiling both directly (a locally fetched CFR jar, same technique as
  every other "need a real method body, not just a signature" gotcha in this file).** This is the
  actual mechanism behind an already-documented finding above (Mermaid's swim speed "feels like
  sliding on ice" no matter how high `swim_speed.json`'s own attribute value goes) — that data
  value only ever affects a separate `water_speed` attribute, never this hardcoded retention
  constant. Being a plain non-final method (unlike the fall-flying branch's own tangle of local
  variables reassigned across mutually exclusive branches, judged too fragile to target directly —
  see `HarpyFlightSpeedMixin`'s own doc), it's a clean, low-risk `@Inject`-at-`RETURN` target:
  overridden to `0.98f` for Mermaid-origin players specifically, leaving vanilla's real 0.8 for
  everyone else. Deliberately leaves the *vertical* retention alone — a separate literal `0.8f`
  hardcoded directly in the same `multiply(...)` call, not read from this method at all, and the
  reported complaint (sliding, hard to turn) is specifically about horizontal steering.
- **A toggle that "does nothing" can mean the effect it toggles was never wired to anything, not
  that the toggle logic itself is broken — worth checking what actually reads the toggled effect
  before touching the toggle power at all.** Harpy's old Slow Falling toggle correctly applied and
  cleared vanilla's own `minecraft:slow_falling` on each press (confirmed the JSON was structurally
  fine), but `glide.json` (the actual passive drift-down-slowly power) never had any `condition`
  checking that effect — it only ever checked sneaking. Toggling `slow_falling` therefore had zero
  observable effect on anything, since nothing in this mod's own powers ever looked at it. Fixed by
  introducing a dedicated marker effect (`HARPY_GLIDE_DISABLED`) that both the toggle power and
  `glide.json`'s own `condition` reference directly, rather than continuing to rely on an
  intermediary vanilla effect nothing here actually reads. As a side effect, this also fixed a
  separate-seeming complaint ("climbing is easier than descending"): `glide.json`'s permanent
  downward velocity cap had been unconditionally fighting descent the whole time, regardless of the
  toggle, precisely because the toggle was never actually connected to it — gating Glide behind the
  toggle made turning it off remove the cap entirely, resolving both complaints from one fix.
- **A brand-new no-cooldown toggle power for "disable/re-enable flight" needed the exact same
  marker-effect-as-toggle-state pattern as the fixed Glide toggle above, applied to
  `origins:elytra_flight` instead of `origins:modify_falling`** — confirmed `elytra_flight`
  supports a top-level `condition` via `.allowCondition()` (checked via `gh api` against
  `apace100/apoli`'s `ElytraFlightPower.java`) before gating it, the same verification step every
  conditional power type in this project gets. This replaced Sudden Gust (a burst-speed power that
  became redundant once flight became permanent) on the same primary key, rather than keeping both
  bound to one key.
- **Sharing one speed-to-damage formula between a thrown hit and a melee hit needs the formula
  itself pulled into the item class, not duplicated in each mixin** — `HarpyJavelinItem
  .airborneBonusDamage(LivingEntity)` is called from both `ThrownTridentMixin` (thrown, already
  existed for the old flat +3) and the new `HarpyJavelinSpeedDamageMixin` (melee, brand new — no
  melee airborne bonus existed before this). It reuses `HarpyJavelinItem
  .MAX_BOOSTED_HORIZONTAL_SPEED` as the same reference ceiling flight itself is capped at, so "full
  bonus damage" and "flying about as fast as this mod ever lets you go" line up by construction
  rather than as two independently-tuned numbers that could drift apart.
- **A mixin class cannot declare a non-private static field — Sponge Mixin's pre-processor rejects
  it outright, and this is a launch-time crash, not a compile error.** `MAX_BOOSTED_HORIZONTAL_SPEED`
  was first added directly to `HarpyFlightSpeedMixin` as `public static final` specifically so
  `HarpyJavelinItem`/`HarpyJavelinSpeedDamageMixin` could reference the same number — this compiled
  fine (javac has no opinion on this) but crashed the whole mixin transform of `LivingEntity` the
  moment the game actually launched: `InvalidMixinException: ... contains non-private static field
  MAX_BOOSTED_HORIZONTAL_SPEED`, caught by the user's own real playtest, not by anything in this
  headless environment. The reason: a mixin's fields get merged directly into the target class's
  own bytecode, and Mixin only allows that for `private` members — a `public`/package-private one
  would have to become a genuinely new public symbol on `LivingEntity` itself, which the
  pre-processor's `validateField` check refuses. `private static final` fields (e.g. this same
  mixin's own `HARPY_ORIGIN_ID`) are completely fine and already used throughout this project's
  other mixins — the fix here was simply moving the shared constant onto `HarpyJavelinItem` (a
  plain class, where `public static` is unremarkable) and having the mixin read it from there as
  its own `private static final` copy instead of declaring the canonical version itself. Worth
  checking for on every future mixin that wants to expose a constant to outside classes: the
  constant's canonical home has to be a non-mixin class, full stop.
- **`assets/minecraft/models/item/trident.json` really is a plain `"parent":
  "minecraft:item/generated"` — but that's only the GUI/dropped-item fallback, not the whole
  story, and treating it as "vanilla trident has no true 3D rendering" was a real, corrected
  mistake this round.** `trident.json` genuinely is flat (confirmed twice, from two different
  jars). But real vanilla ships a *second* model, `trident_in_hand.json`
  (`"parent": "builtin/entity"`, same special-dispatch mechanism the shield uses), and
  `BlockEntityWithoutLevelRenderer.renderByItem` has its own `itemStack.is(Items.TRIDENT)` branch
  (decompiled directly, found by accident while researching the *shield's* rendering, not
  something the first pass ever went looking for) that renders a real `TridentModel` baked from
  `ModelLayers.TRIDENT` — this is the actual code path a held/thrown trident renders through, and
  it's genuinely 3D. The first pass at this round's Javelin/Coral Trident texture fix only ever
  looked at `trident.json`, concluded "vanilla trident is just a flat icon," and used that
  (incorrect) conclusion to justify replacing both weapons' hand-authored 3D models with flat
  `item/generated` ones — which visibly broke their "looks 3D when equipped" appearance, caught by
  the user's own real playtest, not by anything checked here first. **Corrected**: the Harpy
  Javelin now has its own `client/HarpyJavelinRenderer.java` (a `BuiltinItemRendererRegistry
  .DynamicItemRenderer`, registered in `OriginModStudyClient`) reproducing vanilla's real
  `Items.TRIDENT` branch — bakes `ModelLayers.TRIDENT` into a `TridentModel`, renders it with the
  Javelin's own texture instead of vanilla's `textures/entity/trident.png`. `TridentModel
  .createLayer()` bakes at 32x32 (confirmed via decompile), which is exactly why the user's
  replacement texture was 32x32, not this mod's usual 16x16 icon size — they painted it directly
  onto vanilla's real UV layout via a Blockbench plugin that reads `.minecraft`'s own files, the
  same technique already used for the Serpent Aegis's shield texture. Because `ItemRenderer
  .renderStatic` dispatches to this registered renderer the same way regardless of caller,
  `DirectionalThrownItemRenderer` (used for the Javelin's mid-flight/stuck-in-target visual) picks
  this real geometry up automatically, with no separate thrown-specific renderer needed. The
  Living Coral Trident got the identical treatment right after (`client/MermaidTridentRenderer
  .java` — a second, separate `DynamicItemRenderer` class, not reused across both items, since
  each needs its own lazily-baked `TridentModel` instance and its own texture constant; not to be
  confused with the pre-existing `ThrownMermaidTridentRenderer`, which is the unrelated thrown-
  entity renderer), once the user made a matching 32x32 texture for it too. The lesson generalizes:
  a vanilla item "looking 3D when held" is
  not proof by itself that its item model has real geometry — some vanilla items achieve that via
  the exact same flat-icon-plus-diagonal-shading illusion Fang/Venomfang/Widowfang use (still true
  for e.g. swords), but others (shield, trident, and apparently others) have a completely separate
  `builtin/entity` + hardcoded-Java-branch path backing their held appearance, and the two look
  identical from a screenshot — only decompiling `BlockEntityWithoutLevelRenderer` itself
  distinguishes them, checking the flat model file alone is not sufficient.
- **A vanilla `ShieldItem` subclass never gets vanilla's real 3D shield rendering for free — it
  needs its own `BuiltinItemRendererRegistry` hookup, confirmed by decompiling
  `BlockEntityWithoutLevelRenderer.renderByItem` directly rather than assumed.** Its shield branch
  is gated on `itemStack.is(Items.SHIELD)`, an exact identity check against the single vanilla
  item, same as the pre-existing (never-implemented) research this project had flagged for this —
  `instanceof ShieldItem` is never checked anywhere in that method. The real rendering, once
  reached, is straightforward: push pose, `poseStack.scale(1, -1, -1)`, build a foil-aware vertex
  consumer via `shieldModel.renderType(textureLocation)` (a plain `RenderType.entitySolid`, per
  `ShieldModel`'s own constructor `super(RenderType::entitySolid)` — no atlas/`Material` involved
  unless you're also juggling vanilla's banner-pattern system, which the Serpent Aegis never
  needs), then render `shieldModel.handle()` followed by `shieldModel.plate()`. `ShieldModel` is
  baked from the already-registered `ModelLayers.SHIELD` layer (`plate`: 12x22x1 box at
  texOffs(0,0); `handle`: 2x6x6 box at texOffs(26,0); baked at 64x64) — reusing it via
  `Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD)` needs no new
  `EntityModelLayerRegistry` registration at all, since we're baking an *existing* vanilla layer
  definition, not defining a new one. `SerpentAegisRenderer.java` (a Fabric API
  `BuiltinItemRendererRegistry.DynamicItemRenderer`, registered in `OriginModStudyClient`)
  reproduces this exact sequence with the Aegis's own 64x64 texture in place of vanilla's
  `shield_base`/`shield_base_nopattern`, skipping the banner-pattern branch entirely (dead code for
  an item that never carries dye/pattern NBT). The item model itself just needs
  `"parent": "builtin/entity"` (copied from vanilla's real `shield.json`/`shield_blocking.json`,
  including their exact `display` transform blocks and the `blocking: 1` predicate override) — its
  own `"textures"` block is irrelevant to the actual rendered surface (only supplies a `particle`
  sprite for break/hit particles, same inert placeholder vanilla itself uses,
  `block/dark_oak_planks`); the real texture is hardcoded directly in the Java renderer instead,
  completely bypassing the model JSON's texture declarations.
- **Vanilla's real shield-durability-on-block loss is a THIRD occurrence of the same "exact item
  identity, not `instanceof`" anti-pattern already found twice for 3D rendering.** Decompiled
  `Player.hurtCurrentlyUsedShield(float)` directly to find why the Serpent Aegis never lost
  durability blocking a hit ("indestructible," reported by real playtest): `LivingEntity`'s own
  base version of this method is a complete no-op; `Player`'s override starts with
  `if (!this.useItem.is(Items.SHIELD)) return;` before any of the real logic (the `f >= 3.0f`
  threshold, `hurtAndBreak`, the item-used stat, breaking the shield outright at zero durability).
  A plain `ShieldItem` subclass never satisfies that check. Fixed with a HEAD `@Inject` on the same
  method (`SerpentAegisDurabilityMixin`) reproducing the real logic for `SerpentAegisItem`
  specifically — composes safely alongside the original vanilla body, which still runs afterward
  and continues to no-op for our item either way, so nothing needs cancelling.
- **A registered `BuiltinItemRendererRegistry` model's own `display` transform block still applies
  before the custom renderer runs, for every `ItemDisplayContext` including the one this project's
  `DirectionalThrownItemRenderer` was using for thrown/stuck items — and that caused a real,
  reported regression.** `ItemRenderer.renderStatic` applies `bakedModel.getTransforms()
  .getTransform(itemDisplayContext).apply(...)` to the pose stack *before* dispatching to any
  custom renderer, vanilla's own or ours, unconditionally. `DirectionalThrownItemRenderer` used
  `ItemDisplayContext.GROUND` (deliberately, per an earlier fix, for the *old* oversized custom
  models' own `display.ground` scale override). Once the Harpy Javelin/Coral Trident/Petrifying
  Trident switched to real vanilla Trident geometry (copied verbatim from `trident_in_hand.json`,
  including its own `"ground": {"scale": 0.25}` entry), `GROUND` started applying a *second*,
  redundant 0.25 shrink on top of geometry that's already correctly, vanilla-scaled — reported as
  "ridiculously small" once stuck in a block. Real vanilla's own `ThrownTridentRenderer` never
  goes through `ItemDisplayContext` at all for this (it calls `TridentModel.renderToBuffer`
  directly, true scale, no transform layered on top) — switched to `ItemDisplayContext.NONE`
  (identity transform) to match. The lesson: a shared renderer class's own tuned constants
  (display context choice, translation offsets) can silently stop being correct once the
  *content* being rendered through it changes shape/scale, even though the renderer class itself
  didn't change — worth re-checking, not just trusting the old tuning, whenever the underlying
  model swaps to a fundamentally different kind of geometry.
- **A real vanilla trident/spyglass has a third, separate "charging a throw" pose
  (`trident_throwing.json`/`item/trident_throwing`), swapped in via the item model's own
  `"overrides": [{"predicate": {"throwing": 1}, ...}]` — easy to miss entirely, since it only
  shows up while actively charging a throw, not in any of the other, more obvious poses.** Missing
  it doesn't crash or error; it just leaves the weapon in its normal *holding* pose the whole time
  a throw is being charged, which reads as "held backwards" once combined with vanilla's own
  generic arm-pull-back charging animation (tuned to work *with* the dedicated throwing pose, not
  against the normal one). The Harpy Javelin, Living Coral Trident, and Petrifying Trident all
  needed their own `_throwing.json` copy of this override once they switched to real Trident
  geometry, mirroring vanilla's real values verbatim (only the `thirdperson_righthand`/
  `thirdperson_lefthand` rotation/translation actually differ from the normal holding pose;
  first-person is identical either way, which is exactly why the bug was only ever visible in
  third person).
- **Reused vanilla's own reference `elytra_flight`/`slow_falling` no-condition JSON verbatim
  (`apace100/origins-fabric`'s own `powers/elytra.json`, confirmed via `gh api`) after a
  conditional toggle mechanism for both flight and Glide turned out to leave both completely
  non-functional in a real playtest, for a reason never fully root-caused.** Every JSON file
  involved was schema-valid and pattern-identical to two other already-proven-working toggles in
  this same codebase (Innocent Form, Dolphin's Grace); `.allowCondition()` support on both
  `elytra_flight` and `modify_falling` was directly confirmed against Apoli's own source. Static
  analysis alone couldn't find the actual defect. Per the user's own explicit call ("forget about
  the power to disable... let's have the elytra always equipped"), the whole toggle mechanism
  (two power files, two marker effects, two key bindings) was removed rather than debugged
  further — Harpy's flight and Glide are unconditional again, matching how base Origins' own
  `elytra.json` power is actually written (no condition field at all). Worth remembering: not
  every bug is worth chasing to a root cause before removing the feature that introduced it,
  especially when the simpler prior design already worked.
- **`LivingEntity.zza`/`xxa` are plain public vanilla fields holding the entity's current forward/
  strafe movement input impulse (-1 to 1), already synced server-side for a real player every tick
  (the server needs it for its own authoritative movement simulation) — no custom networking
  needed to read "is this player holding backward" from a server-side mixin.** Used to make Harpy
  flight actually decelerate when holding S, rather than just capping the boosted top speed lower:
  `HarpyFlightSpeedMixin` now `@Shadow`s `zza` and checks `zza < 0.0F` (holding backward) to apply
  a gentle per-tick deceleration instead of the usual forward boost. Directly addresses the user's
  own diagnosis of why a slower flight speed was even requested in the first place — vanilla's
  elytra physics have no drag mechanism strong enough to ever slow back down once this mod's own
  boost has built up speed, so a hard speed cap alone can only ever raise or lower the ceiling,
  never give the player a real way to actively slow down mid-flight.
- **Fabric API's `ModelLoadingPlugin.register(...)` is a plain static method callable directly from
  client init code — no `fabric.mod.json` entrypoint needed at all** (confirmed by decompiling the
  interface itself, not assumed from the Fabric Wiki tutorial's own registration example, which
  uses the entrypoint form). `Context.addModels(ResourceLocation...)` registers extra, independently
  -addressable model resources — used to give the Storm Trident/Coral Trident/Petrifying Trident
  each a "flat icon" fallback model that isn't any item's own default registration
  (`TridentStyleFlatModels.register()`, called from `OriginModStudyClient`).
- **A `ModelResourceLocation` passed to `Context.addModels(...)` needs the *bare* model id, no
  `item/`/`block/` folder prefix — a real, shipped bug this round, not just a theoretical
  gotcha.** Confirmed directly against vanilla's own real code: `ItemRenderer`'s
  `TRIDENT_MODEL`/`TRIDENT_IN_HAND_MODEL` constants both wrap `ModelResourceLocation.vanilla(
  "trident", ...)` — bare `"trident"`, never `"item/trident"` — and the Fabric Wiki's own tutorial
  registers both its block-state and item variants under the same bare path, differing only in the
  variant string (`""` vs `"inventory"`). The first version of `TridentStyleFlatModels` used
  `"item/storm_trident_flat"` as the underlying path, which resolves to a nonexistent
  `models/item/item/storm_trident_flat.json` (folder prefix duplicated) — the model fails to load,
  and a missing *model* renders identically to a missing *texture* (the purple/black checkerboard),
  which is exactly why this was reported back as "wrong texture file," not "wrong model path," at
  first. Fixed by dropping the `item/` prefix from all three `ResourceLocation` constants; the
  `"inventory"` variant on the `ModelResourceLocation` wrapper is what actually tells the loader to
  resolve the bare path under `models/item/`, not anything in the path string itself.
- **Real vanilla's "flat icon in the hotbar, real 3D when held" split (Trident, Spyglass) is a
  hardcoded, item-specific special case inside `ItemRenderer` itself — there is no generic
  per-item toggle for it in vanilla or Fabric, confirmed via web research this round since this
  project's own prior research never covered it.** `ItemRenderer.getModel()` unconditionally
  resolves `Items.TRIDENT` to a *separately loaded* `trident_in_hand` model (a
  `ModelResourceLocation` addressed directly by name, not through the item's own registration);
  `render()` then swaps it back to the plain flat one specifically for `GUI`/`GROUND`/`FIXED`
  contexts. Replicating this for a third-party item that (unlike vanilla's Trident) already has a
  3D `builtin/entity` model as its *default* registration only needs half of vanilla's own dance:
  no `getModel()` swap is needed (held contexts already get 3D for free), just a `render()`-side
  mixin forcing the flat fallback for the same three contexts. Implemented as
  `TridentStyleFlatIconMixin`, using the same HEAD-`@Inject`-captures-into-a-field technique this
  project already established for a different reason (`ThrownTridentMixin`), rather than declaring
  extra `@ModifyVariable` handler parameters to read the enclosing method's other arguments — the
  latter is real, supported Mixin capability (a genuinely different one from the already-documented
  `@ModifyArg` limitation elsewhere in this file), but untested in this project until now, so the
  already-proven capture-to-field pattern was preferred for a player-facing render path this
  environment can't playtest.
- **The same "GUI/GROUND/FIXED forced back to flat" fix also resolved a second, separately
  reported bug — dropped Trident-shaped items looking "super small" on the ground — since it's the
  exact same root cause.** A dropped item entity renders with `ItemDisplayContext.GROUND`; without
  vanilla's own hardcoded flat-model force-back for that context, a custom `builtin/entity` item
  renders its full 3D geometry there too, with the model's own `"ground"` display scale (0.25,
  copied from vanilla) applying on top of whatever additional scaling `ItemEntityRenderer` itself
  assumes for a normal flat icon — two fixes for the price of one once the actual mechanism was
  understood, rather than two separate patches.
- **`Player.hurt(DamageSource, float)` is a real, direct override (confirmed via `javap`, then
  fully decompiled to check), not just inherited from `LivingEntity` — and it calls `super.hurt(
  damageSource, f)` at the very end of its own body, after its player-specific checks (invulnerable
  ability, difficulty scaling).** This matters because it means a HEAD `@Inject` on `LivingEntity
  .hurt` fires for player defenders too (via that `super` call), *and* a HEAD `@Inject` directly on
  `Player.hurt` is equally valid and fires strictly earlier, before any of Player's own difficulty-
  scaling adjustments touch the raw damage number or the `DamageSource`. Used for two separate
  Serpent Aegis features needing information `blockedByShield`/`hurtCurrentlyUsedShield` are never
  given: `SerpentAegisBlockMixin` captures the raw blocked damage (from `LivingEntity.hurt`, since
  reflection logic already lives in that class) to reflect half of it back at the attacker as
  `damageSources().thorns(...)`; `SerpentAegisDurabilityMixin` separately captures the
  `DamageSource` itself (from `Player.hurt`, since that's where the real `hurtCurrentlyUsedShield`
  override lives) to exempt `DamageTypes.FIREBALL` from durability loss entirely. Two independent
  capture points in two independent mixin classes, deliberately not sharing one field across
  classes — Mixin doesn't let unrelated mixin classes read each other's injected fields without an
  explicit accessor, so each mixin captures only what it itself needs, from whichever method
  actually has that information available on its own target class.
- **A HEAD `@Inject` into a method that returns a non-`void` type needs `CallbackInfoReturnable`,
  not plain `CallbackInfo` — even if the handler never touches it, and even at `HEAD` where
  nothing has been returned yet.** Real launch crash, not a compile error (`javac` has no opinion
  on which callback type a handler declares): both new damage-source-capturing injections above
  target `hurt(DamageSource, float)`, which returns `boolean`, but were first written with plain
  `CallbackInfo` (copied from this project's many *other* HEAD injections, all of which happen to
  target `void` methods). `InvalidInjectionException: ... CallbackInfoReturnable is required!` —
  caught by the user's own real playtest, not by anything checked here first, same as the earlier
  non-private-static-field mixin crash. The fix is purely mechanical: swap the last parameter's
  type to `CallbackInfoReturnable<Boolean>` (boxed, matching the primitive return type) — no other
  change needed, since neither handler actually cancels or rewrites the return value. Worth
  checking the real return type of *any* injection target before assuming `CallbackInfo` is safe,
  not just pattern-matching against this project's own existing (mostly-`void`-target) mixins.
- **`LivingEntity.blockedByShield(LivingEntity)` runs on the ATTACKER, with the DEFENDER as its
  parameter — the exact opposite of what this project's original `SerpentAegisBlockMixin` assumed,
  a real bug that shipped and was only caught because damage reflection (added on top of the
  already-"working" Slowness feature) failed completely.** Fresh decompile of the real call chain,
  re-checked from scratch rather than trusted from memory this time: `LivingEntity.hurt(...)`
  calls `this.blockUsingShield(attacker)` (defender calling it); `blockUsingShield(LivingEntity
  livingEntity)` — `this` = defender here — calls `livingEntity.blockedByShield(this)`, i.e.
  `attacker.blockedByShield(defender)`. Vanilla's own default body confirms the direction:
  `livingEntity.knockback(0.5, livingEntity.getX() - this.getX(), ...)` knocks back the
  *parameter* using a vector pointing away from `this` — matching the well-known real vanilla
  behavior that blocking a heavy hit pushes the *blocker* backward, not the attacker. The original
  mixin read `this` as the defender and the parameter as the attacker (exactly backwards), so it
  only ever applied its effects when those roles happened to coincidentally line up (e.g. two
  Aegis-wielding Medusa players fighting each other) — never in the intended "Medusa blocks,
  attacker gets punished" case. The lesson: when a "this is called on X, parameter is Y" claim
  about a vanilla hook was written once and never independently re-verified, a *new* feature
  built on top of it is a real chance to catch it wrong — don't assume the old feature was
  necessarily validated as thoroughly as its own comments claim just because nothing looked broken
  yet.
- **Vanilla's real water-swimming drag is `this.isSprinting() ? 0.9f : this.getWaterSlowDown()`
  — sprint-swimming (the normal way anyone swims fast) takes a hardcoded branch that never calls
  the overridable method at all.** This is why every Mermaid swim-speed multiplier bump this
  session, despite the JSON value and the underlying `additionalentityattributes:water_speed`
  attribute genuinely changing (confirmed via decompile of that mod's own mixin — it modifies
  `moveRelative`'s speed argument unconditionally, sprint or not), never actually felt any
  different: `MermaidWaterTurningMixin`'s own retention override (0.98, near-zero drag) only ever
  applied while *not* sprinting, silently inactive for the one case everyone actually uses.
  `isSprinting()` is declared on `Entity`, not `LivingEntity` (confirmed via `javap` before
  writing a mixin targeting a different class than the existing one).
- **The first attempt at the sprint-swim fix above — forcing `Entity.isSprinting()` itself to
  report `false` while a Mermaid is in water — was a real regression, caught by the user's own
  playtest, not by anything checked here first.** `isSprinting()` isn't only read by the water-drag
  ternary; `Entity.updateSwimming()` uses the *exact same flag* to decide whether to enter the
  swimming pose at all: `this.setSwimming(this.isSprinting() && this.isUnderWater() && ...)`.
  Globally overriding `isSprinting()` for Mermaid-in-water silently disabled the swimming
  animation/pose too — she moved through water while stuck in the upright walking pose, reported
  back as "can't swim, just walks... more similar to gliding." The lesson: overriding a query
  method used as an input to *one* piece of logic can silently break *other* logic that reads the
  same query for a completely different purpose — grep for other real callers of a method before
  assuming an override is scoped to the one call site being targeted, not just the one being
  fixed. **Corrected fix**: `MermaidSprintSwimMixin` now uses `@ModifyConstant` (standard Sponge
  Mixin, not a new dependency — the same annotation Additional Entity Attributes' own mixin
  already uses elsewhere in this project's dependency tree) targeting the literal `0.9f` constant
  in `travel()`'s water-drag ternary directly, confirmed via decompile to appear exactly once in
  the entire method body (no ordinal ambiguity, unlike the local-variable techniques this project
  has otherwise avoided in this same method) — changes only the *value* substituted into that one
  ternary branch, without touching `isSprinting()` or anything else that calls it.
- **Vanilla shields have no spatial hitbox at all — blocking is a pure directional check, not a
  collision volume, confirmed via decompiling `LivingEntity.isDamageSourceBlocked` directly before
  assuming a "make the shield hitbox bigger" request was literally possible.** The real check is
  `this.isBlocking() && dot(viewVector, directionToDamageSource) < 0` — evaluated individually on
  whoever is actually taking damage, with no radius/volume/hitbox concept anywhere in the method.
  "A shield that defends more people" needed a genuinely new mechanic, not a hitbox resize —
  `SerpentAegisAllyProtectionMixin` reduces damage to nearby *other* players (not just Medusa
  herself, and deliberately not extended to arbitrary nearby mobs, to avoid accidentally softening
  hits against hostile mobs standing nearby) while she's actively blocking with the Aegis, via
  `@ModifyVariable` on `hurt(DamageSource, float)`'s own float parameter — the single-parameter
  form of that annotation, matched purely by type, the same safe pattern already established by
  `TridentStyleFlatIconMixin`.

- **Every modded trident-style thrown entity had Loyalty silently non-functional since it was
  first introduced — confirmed by decompiling `ThrownTrident` directly.** Its real
  `(Level, LivingEntity, ItemStack)` constructor is the *only* place vanilla ever populates the
  private synced-data fields `ID_LOYALTY`/`ID_FOIL` (from `EnchantmentHelper.getLoyalty(itemStack)`/
  `itemStack.hasFoil()`) — `tick()` reads `ID_LOYALTY` every tick to decide whether/how fast to
  pull the entity back to its owner. `ThrownJavelin`/`ThrownMermaidTrident`/
  `ThrownPetrifyingTrident` all have to use the `(EntityType, Level)` constructor instead (per the
  `tridentItem`-field gotcha below — vanilla's itemstack constructor hardcodes `EntityType
  .TRIDENT`), which leaves both fields at `defineSynchedData()`'s hardcoded `0`/`false` defaults
  forever, regardless of what enchantments the real thrown item actually has. This is exactly the
  kind of case the `tridentItem`-field gotcha already flagged as a risk ("no extension point to
  build on top of, has to be replicated") but the replication was incomplete — only `tridentItem`
  itself was ever copied over, not the two enchantment-derived synced fields tick() actually reads.
  Fixed by extending `ThrownTridentAccessor` with two more `@Accessor` methods exposing the
  private static `ID_LOYALTY`/`ID_FOIL` keys (Mixin accessors work identically for a static field —
  the generated method just ignores the receiver and reads/writes the class's own static slot), then
  setting both synced values explicitly in all three subclasses' itemstack-carrying constructors,
  matching vanilla's own real constructor line for line.
- **`origins:add_velocity` supports a `"space": "local"` field (facing-relative, not world-space)
  and a `"set": true` field (overrides velocity outright instead of adding to it) — confirmed via
  real Apoli test-resource examples (`trident_jump.json`, `velocity_behind_head.json`,
  `add_velocity.json` in `apace100/apoli`'s own test resources, via `gh api`) before using either
  field.** Mermaid's Riptide dash (`powers/mermaid/riptide_dash.json`) uses both together — a
  fixed local-space velocity along `z`, matching real vanilla Riptide III's own push magnitude
  (`3.0F * (1 + riptideLevel) / 4`, confirmed via decompile of `TridentItem.releaseUsing`) — to
  replicate "a forward burst regardless of which way you're facing" without any custom Java. The
  initial guess (`z: -3.0`) was backward — real playtest confirmed positive `z` (`z: 3.0`) is
  actually forward in Apoli's local-space convention for `add_velocity`, the opposite of
  Minecraft's usual "-Z is forward" world-space convention. Worth remembering for any future
  `add_velocity`/`"space": "local"` power in this project: don't assume the usual -Z-forward
  convention carries over, verify with a real playtest.

- **The 1.20.1 → 1.21.1 port (8 planned tasks, this repo's `mc1.21.1-port` branch) touched a wide
  cluster of vanilla API signatures at once — every one of the following was confirmed via `javap`/
  CFR decompile of the real mapped 1.21.1 jar before being applied, not guessed from a changelog.**
  Recorded here as one block since they were all discovered/fixed together and several interact:
  - **`ItemAttributeModifiers` (a `DataComponentType`) replaces `Item#getDefaultAttributeModifiers
    (EquipmentSlot)`/`SwordItem`'s old `(Tier, int, float, Properties)` constructor overload.**
    `SwordItem` itself now only takes `(Tier, Properties)`; its own real damage/speed formula
    (`ATTACK_DAMAGE = attackDamageModifier + tier.getAttackDamageBonus()`, `ATTACK_SPEED =
    attackSpeedModifier`, both `ADD_VALUE`, `EquipmentSlotGroup.MAINHAND`) survives as a public
    static helper, `SwordItem.createAttributes(tier, i, f)` — reused directly by `FangItem` rather
    than hand-rebuilding the same numbers. A custom item with its own bespoke modifiers (the Harpy
    Javelin) instead builds an `ItemAttributeModifiers` via its own builder and passes it through
    `Properties#attributes(...)` before `super(...)`, the same general pattern. Note `EquipmentSlot`
    (a single slot) and `EquipmentSlotGroup` (e.g. `MAINHAND`, matching *any* hand) are two distinct
    types in this API — `ItemAttributeModifiers` entries are always scoped by the group, not the
    single slot.
  - **`Model#renderToBuffer` and `ModelPart#render` both collapsed their trailing `float red, green,
    blue, alpha` quartet into one packed ARGB `int` — confirmed real vanilla call sites
    (`ThrownTridentRenderer`, and `Model`'s own 4-arg convenience overload) both pass literal `-1`
    (all bits set = opaque white, "no tint") as that packed value, so every `renderToBuffer(...,
    1.0F, 1.0F, 1.0F, 1.0F)`/`.render(..., 1.0F, 1.0F, 1.0F, 1.0F)` call in this project became
    `..., -1)`.** Affects both classes identically since `ModelPart` and `Model` gained the same
    signature change in lockstep — worth checking both, not just the one this session went looking
    for first.
  - **Every real API that consumes a `MobEffect` reference now takes `Holder<MobEffect>`, not a
    plain `MobEffect`** — `MobEffectInstance`'s constructors, `LivingEntity#hasEffect`/
    `#removeEffect`/`#removeEffectNoUpdate`. Vanilla's own `MobEffects` class registers via
    `Registry.registerForHolder(...)` (not the plain `Registry.register(...)` this project's
    `ModEffects` used pre-port) specifically to get a `Holder<MobEffect>` back instead of a bare
    `MobEffect` — `ModEffects`'s own fields are now typed `Holder<MobEffect>` for the same reason;
    changing the registration helper's return type at the one shared call site fixed every
    downstream `addEffect`/`hasEffect` call site in the same pass, rather than wrapping each call
    site individually.
  - **`ItemStack#hurtAndBreak` gained a second overload taking a plain `EquipmentSlot` instead of a
    `Consumer<Item>`/`Consumer<LivingEntity>` break-event callback** — `(int, LivingEntity,
    EquipmentSlot)`, confirmed against vanilla's own real call sites (`TridentItem.releaseUsing`
    uses `LivingEntity.getSlotForHand(player.getUsedItemHand())`; `postHurtEnemy` uses the constant
    `EquipmentSlot.MAINHAND` directly). Every `stack.hurtAndBreak(1, player, p ->
    p.broadcastBreakEvent(hand))`-shaped call in this project (Fang/Petrifying Trident/Mermaid
    Trident/Harpy Javelin/Silk Net Shooter/the Serpent Aegis durability mixin) became
    `stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand))` — the lambda form still
    compiles (a lambda taking one `Player`/`LivingEntity` param looks like it should satisfy either
    overload), but resolves to the wrong one and fails with "EquipmentSlot is not a functional
    interface" once the `Consumer`-based overload stops existing.
  - **Fabric API's `ModelLoadingPlugin.Context#addModels` dropped its `ModelResourceLocation`
    overload entirely — it's plain `ResourceLocation` (varargs or `Collection`) now, since
    `ModelResourceLocation` itself stopped being a `ResourceLocation` subtype in this version (it's
    now a standalone `record` wrapping one plus a variant string).** More subtly, decompiling
    Fabric API 2.1.0's own consumer of these ids (`ModelLoaderMixin#addExtraModel`) shows it loads
    each id through the *raw* model-file path (`models/<path>.json`) with **no automatic `item/`
    prefix** — unlike vanilla's own real item-loading loop
    (`ModelBakery#loadItemModelAndDependencies`, which explicitly calls
    `resourceLocation.withPrefix("item/")` before loading). Since this project's actual flat-icon
    model files live at `models/item/*.json` (unchanged), the `ResourceLocation` constants handed
    to `addModels` now need the `"item/"` segment baked into their own path (e.g.
    `"item/storm_trident_flat"`, not bare `"storm_trident_flat"`) — the exact opposite fix from the
    1.20.1-era version of this same constant, which needed the prefix *removed* to avoid a doubled
    `item/item/` path under the old API. The lookup side (`TridentStyleFlatIconMixin`, wrapping the
    same constant in `ModelResourceLocation.inventory(...)`) didn't need to change at all — both
    ends only ever need to agree with each other, not with vanilla's own per-real-item convention.
  - **Fabric API's `HudRenderCallback#onHudRender` changed its second parameter from a plain `float`
    partial-tick to a `DeltaTracker`** (`net.minecraft.client.DeltaTracker`, exposing
    `getGameTimeDeltaPartialTick(boolean)` for code that actually needs the interpolation value).
    `MermaidAirSupplyHud`'s render callback never used the old float at all (it draws a snapped
    bubble count, no interpolation), so the fix was purely a signature update, accepting and
    ignoring the `DeltaTracker` the same way the old code accepted and ignored the float.
  - Beyond this task's own discoveries, the wider port also touched: `ResourceLocation`'s
    constructor becoming `ResourceLocation.fromNamespaceAndPath(...)`; the Cardinal Components
    package/Maven coordinates moving to `org.ladysnake.cca` 6.1.0; Origins' own API shifting to
    `OriginLayerManager.getNullable(...)`/`Origin.getId()`; a cluster of vanilla `Holder<T>`
    wrapping for enchantments/attributes/food/sounds and the undead-mob-type check; and
    `MobEffect#isDurationEffectTick` being replaced by `MobEffect#shouldApplyEffectTickThisTick`
    (a slightly different check worth re-reading rather than assuming a 1:1 rename — confirmed via
    `javap`/CFR before `BleedMobEffect` was updated to override the new method).

## Build / verify

```bash
JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew build   # compile + build mod jar -> build/libs/
```

`runClient` needs a display and hasn't been run in this environment — in-game verification (does
the origin appear in the picker, do the powers behave as specified, does the mixin correctly
suppress/restore arthropod hostility) is a manual step: drop the built jar into either
PrismLauncher instance's `mods/` folder alongside the already-installed Origins/Pehkui/Fabric API
jars.

JSON-lint new data pack files before building:
```bash
python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]"
```

Benign `Cannot remap <member> because it does not exist in any of the targets ...` warnings appear
on every clean build's `remapSourcesJar`/`remapJar` steps — a `Mercury`/ECJ source-remapper
limitation (or, post-1.21.1-port, Loom's own mixin-remapping pass not always resolving an
`@Accessor`/`@Shadow` target back to its real declaring member) that only affects the auxiliary
`-sources.jar`'s IDE-navigation copy and/or the remap metadata, never compilation or the real mod
jar's actual behavior. Pre-port this only ever named `ArthropodPassiveTargetMixin.java`; post-port
(1.21.1) it instead names `tridentItem` (twice — `ThrownTridentAccessor`'s `@Accessor` for
`ThrownTrident`'s private field) and `getPickRange` (the extra-reach mixin on
`MultiPlayerGameMode`) during `remapSourcesJar`, plus one `tridentItem` recurrence during
`remapJar` itself. `BUILD SUCCESSFUL` with these warnings present is expected, not a regression —
worth re-checking which specific member names show up here after any future mixin/accessor
change, since this list is exactly the kind of thing that silently drifts and misleads a future
reader if left stale.

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
    carnivore-diet food), `ARACHNE_EYE`/`MEDUSA_EYE`/`HARPY_EYE`/`MERMAID_EYE` (icon-only, no recipe,
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
    every other item here) — see `assets/monster_origins/models/item/harpy_javelin.json`. `MERMAID_CROWN` is
    `MermaidCrownItem` (below).
  - `item/MermaidCrownItem.java` — Mermaid's exclusive armor: `+2` hearts via
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
  - `sound/ModSounds.java` — registers the `monster_origins:harpy_scream` and `monster_origins:mermaid_song` sound
    events; the actual audio lives in `assets/monster_origins/sounds/*.ogg` + `assets/monster_origins/sounds.json`
    (see CREDITS.md for both files' licenses).
  - `power/ScreamConeAction.java` — the one custom Apoli `EntityActionType` in this project
    (`monster_origins:cone_knockback`), registered into `ApoliRegistries.ENTITY_ACTION` directly rather
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
    for Mermaid: one keyed off a new entity-type tag (sea creatures, including Drowned/Guardian —
    see the Guardian AI gotcha above), one keyed off a new passive marker status effect
    (`ModEffects.CHARMED`, applied by Mermaid's Call) instead of a static tag. Kept as two separate
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
- `src/main/resources/data/monster_origins/`
  - `origins/arachne.json` — the origin: name, description, icon (`monster_origins:arachne_eye`), and its
    power list (19 entries as of this writing — a mix of references to base-Origins power IDs and
    this addon's own custom powers).
  - `origins/medusa.json` — the second origin, tanky/petrify-focused (12 hearts, +6 armor,
    on-hit and AOE petrify, immune to her own petrify effects, weakened by direct sunlight).
    The real second worked example of the per-origin pattern — `example_stub.json` is still the
    minimal empty-file starting point, but Medusa is what a filled-in one actually looks like.
  - `origins/harpy.json` — the third origin, true flight + fragile + aerial skirmisher (8 hearts,
    permanent elytra-less flight, a flight-speed boost power, bare-fist bonus damage + the custom
    Bleed effect, a directional knockback scream). The one origin in this mod needing real custom
    code beyond a single mixin.
  - `origins/mermaid.json` — the fourth origin, aquatic support/crowd-control (breathes/sees/mines
    underwater with no penalty, a 5-minute out-of-water grace period before suffocating, a
    speed/land tradeoff, a real-audio buff-everyone/pacify-hostiles song, a Dolphin's Grace
    on/off toggle for the secondary key, every sea creature friendly). Turned out to be the *most*
    data-driven origin in this mod, despite looking like the most custom-code-heavy one going in —
    see the gotchas above for why (including the toggle, which needed no new Java at all).
  - `origins/example_stub.json` — TEMPLATE.md's worked-example starting point; deliberately not
    wired into the origin picker.
  - `powers/arachne/*.json`, `powers/medusa/*.json`, `powers/harpy/*.json`, `powers/mermaid/*.json` —
    each addon origin's own custom powers, one subfolder per origin (the convention TEMPLATE.md
    documents, to keep same-named powers across different origins from colliding). Each power has
    an inline `name`/`description` — Origins supports these as plain strings directly on the power
    JSON, so no separate lang file entries were needed for powers (items are different, see below).
  - `recipes/golden_spider_eye.json` — mirrors vanilla's real `golden_apple` recipe shape exactly
    (8 gold ingots around the center item), just swapping the center for a spider eye.
  - `recipes/trident.json` — a new global recipe for the vanilla trident (3 prismarine shards + 2
    sticks), craftable by anyone, not Mermaid-exclusive — same recipe-can't-see-the-player
    limitation documented under `util/OriginUtil.java` above.
  - `recipes/venomfang.json`, `recipes/widowfang.json` — Fang's tier-2 upgrade is a plain
    `minecraft:crafting_shaped`/shapeless recipe; tier-3 is a real `minecraft:smithing_transform`
    (verified against vanilla's own `netherite_sword_smithing.json`, extracted straight from the
    mapped jar — see the gotcha above), consuming the previous tier item as its `base` ingredient.
  - `tags/entity_types/enemies.json` — curated hostile-mob list, originally for Arachne's
    tracking-glow power, reused as-is by Medusa's Dreadful Presence/Stone Gaze Burst and Mermaid's
    Call (same mod/namespace, so cross-origin tag reuse is just a normal reference, not a hack).
  - `tags/entity_types/friendly_arthropods.json` — spider/cave_spider/silverfish/endermite (the
    vanilla arthropod grouping; bees deliberately excluded, they aren't in it).
  - `tags/entity_types/friendly_sea_creatures.json` — the vanilla aquatic mobs plus Drowned and
    Guardian/Elder Guardian (verified their targeting AI still routes through the same choke point
    this mixin technique intercepts — see the Guardian gotcha above).
  - `tags/items/raw_meat.json` — the raw-meat items (plus rotten flesh) Harpy's Hardy Stomach power
    checks against.
  - `recipes/mermaid_trident.json` — the Living Coral Trident's own recipe (Task 13): Tube or Horn
    Coral Block + Prismarine Crystals + Prismarine Shard.
- `src/main/java/com/example/originmodstudy/loot/MermaidLootEvents.java` — the Living Coral
  Trident's two loot-related traits, real Java event hooks since neither has a data-driven path:
  bare-hand coral mining (swaps an already-dropped dead-coral item entity for the live block, since
  fighting the loot table's own silk-touch `alternatives` structure would need a whole new
  registered `LootItemCondition` type just for an origin check) and the Mermaid-only 20%
  prismarine-shard fish drop (`ServerLivingEntityEvents.AFTER_DEATH`, checked against the four
  vanilla fish entity types directly since 1.20.1 has no `EntityTypeTags.FISH`).
- `src/main/resources/assets/monster_origins/`
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
    Mermaid to the standard origin-picker GUI (see TEMPLATE.md §2 for why this path/format).
  - `powers/master_of_webs.json` — a full-content override (via `loading_priority`) of Origins'
    own `master_of_webs` power, changing only the on-hit cobweb cooldown (120 → 40 ticks). If
    Origins ever changes that power's structure upstream, this override goes stale silently — no
    build-time way to detect that from here.
  - `tags/items/meat.json` — additive (default tag-merge behavior, not an override) — just adds
    `monster_origins:golden_spider_eye` to Origins' existing meat list.
## Conventions & gotchas

- Only commit when asked. Build artifacts (`build/`, `.gradle/`, `run/`) are gitignored.
- When adding a new custom power JSON, verify its power-type schema against the real thing before
  guessing field names — either the [origins-docs](https://github.com/apace100/origins-docs)
  markdown source (more reliable than the rendered readthedocs site, which 404s on some paths) or
  a real power file from `apace100/origins-fabric`'s `1.20` branch via `gh api`. Every power type
  used in this repo was verified this way, not assumed from memory.
