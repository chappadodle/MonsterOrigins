# Monster Origins × the-system Integration — Design

Status: design doc, no implementation started. Spans two repos: this one (`origins-mod-study`,
mod id `monster_origins`) as the addon, and `the-system` (mod id `thesystem`) for two small
prerequisite core additions.

## Why this exists

Monster Origins currently has no connection to `the-system` at all — its four origins (Arachne,
Medusa, Harpy, Mermaid) are pure Origins/Apoli content with a handful of custom mixins/items for
things Origins can't do natively. Meanwhile `the-system`'s player-side stats/leveling/Skills
framework already proved its addon pattern with one worked example (`addon-example`'s Appraisal
skill). This design wires Monster Origins into that framework as a full addon: origin selection
drives stat bias, every existing signature ability becomes a leveling Skill, and a future
evolution hook gets a placeholder.

The user's classes-are-being-deprecated direction (a "Blacksmith" class becomes a "Blacksmithing"
skill line, tracked separately in `the-system`'s own evolving design) means origin selection
becomes a full replacement for the old class-selection screen for any player using this addon,
not just an additional modifier on top of it.

## Non-goals

- **Evolution mechanics** — a reminder paragraph only (see below), no schema or logic.
- **Mob-side species evolution** — already tracked as a separate, not-yet-designed future doc
  alongside `the-system`'s mob-leveling design (`the-system/docs/superpowers/specs/2026-07-30-mob-leveling-design.md`).
- **Requirement gating changes** — this addon doesn't touch `RequirementRegistry`.
- **Converting every Origins power to a Skill** — only powers with real "use"/ability semantics
  migrate; purely passive movement/attribute powers (flight speed, climb speed, swim speed, etc.)
  stay untouched Origins powers.

## Core prerequisites (land in `the-system`, not this repo)

Two small, fully generic additions — neither is Monster-Origins-specific, any future addon
benefits from both.

### 1. Skill category tag

`SkillDefinition` (`skill/SkillRegistry.java` in `thesystem` — currently name/description/
max_level only) gains a `category` field accepting one or more of: `ACTIVE`, `PASSIVE`,
`ORIGIN_SPECIFIC`. A skill can carry more than one tag (e.g. `PASSIVE` + `ORIGIN_SPECIFIC`).
Purely additive to the existing JSON schema — no behavior change for skills that omit it.

### 2. Passive skill trigger path

The existing `SkillBehavior.onUse(ServerPlayer, LivingEntity, int level)` only fires from a
player-triggered network packet (`network/SkillUseNetworking.java`) — correct for active skills,
wrong shape for a passive one that should fire automatically (e.g. venom applying on-hit, with
no player input). Adds a second interface, `PassiveSkillBehavior`, invoked directly from whatever
real event triggers it (an on-hit mixin, a tick event, etc.) rather than the use-packet. Both
interfaces share the same underlying `SkillHolder` storage and `SkillProgressHandler` proficiency-
XP awarding — leveling/proficiency works identically regardless of which interface a skill
implements.

## Origin → stat bias

Each origin rolls a stat bias at selection time using the existing `rollStat`/`StatRange` formula
(`classdef/CharacterCreation.java`/`classdef/StatRange.java` in `thesystem`), but triggered off
Origins' own "origin chosen" event rather than the-system's own character-creation screen — this
addon bypasses that screen entirely for its own origins. Illustrative biases (exact numbers
tuned later): Arachne biases Dexterity/Luck, Medusa biases Defense, Harpy biases Dexterity,
Mermaid biases Vitality/Luck. Free stat-point allocation on top still lets a player reshape their
build — origin nudges the roll, it never locks anything, matching the existing class-bias
philosophy.

## Skill migration

Every existing signature ability across all four origins is re-expressed as a `thesystem` Skill
(registered via `SkillBehaviorRegistry`, metadata via a `skills/` JSON under this mod's own
`monster_origins` namespace), tagged `ORIGIN_SPECIFIC` plus either `ACTIVE` or `PASSIVE`:

| Origin  | Ability                         | Category            |
|---------|----------------------------------|----------------------|
| Arachne | Venomous Bite (on-hit poison)     | Passive, Origin-specific |
| Medusa  | Petrify Gaze / Stone Gaze         | Active, Origin-specific |
| Medusa  | Dreadful Presence                 | Passive, Origin-specific |
| Harpy   | Scream (cone knockback)           | Active, Origin-specific |
| Mermaid | Dolphin's Grace (toggle)          | Passive, Origin-specific |
| Mermaid | Trident bonus damage (Barbed Tip) | Passive, Origin-specific |

Each gains proficiency through use exactly like any other `thesystem` Skill (the existing
usage-scaled-by-target-level XP curve, `SkillXpCurve`). Purely cosmetic or movement/attribute-only
powers (flight, climb speed, swim speed, land/water slowness) have no "use" semantics and stay as
plain, unmodified Origins powers — nothing in this migration forces every power into the Skills
system, only the ones that are actually abilities.

Concretely: each migrated ability's current implementation (JSON power, mixin, or item hook)
becomes the body of a `SkillBehavior`/`PassiveSkillBehavior` implementation registered by this
mod's own `ModInitializer`, and the original always-on Origins power JSON for that ability is
removed (not left running in parallel) so the ability only ever fires through its new Skill form.

## Evolution — reminder paragraph

Not designed, not scheduled. A future `EvolutionDefinition` extension point could let an origin
(player-side, e.g. Arachne evolving into a stronger spider-form) or a species (mob-side, tracked
separately in `the-system`'s mob-leveling design) define a next-stage identity unlocked past a
level or proficiency threshold. No schema, no trigger condition, no visual/mechanical shape
decided here — this paragraph exists purely so neither future design starts from zero context.

## Verification

Documentation-only deliverable for this pass. When implementation eventually happens: the two
core prerequisites need their own `thesystem` version bump + `publishToMavenLocal` before this
addon can build against them (same dependency-staleness risk already documented in
`thesystem/docs/ADDONS.md` for `addon-example`). Manual in-game verification of origin-triggered
stat bias and passive-skill-on-hit firing will need the same PrismLauncher instance workflow
already used for the rest of this project.
