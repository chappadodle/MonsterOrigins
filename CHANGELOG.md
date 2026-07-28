# Changelog

All notable player-facing changes to Monster Origins are recorded here, one bullet per feature
change, grouped by origin. Infrastructure/cleanup work with no direct in-game effect gets its own
section instead of an origin's.

## [Unreleased]

### Infrastructure

- Added `PetrifyMobEffect` (an attribute-modifier-based status effect: -90% movement speed, -80%
  attack speed via `MULTIPLY_TOTAL`) and a no-op Stone Gaze cooldown marker effect, both registered
  in `ModEffects.java` with their own icon textures and lang keys. Groundwork for Medusa's upcoming
  petrify rework (Phase 4); nothing references either effect yet, so there is no player-visible
  change from this alone.

### Cleanup

- Removed all hyphens and em dashes from item names, power names, and descriptions by rewriting
  affected phrases naturally (e.g., "storm-wind" → "swift, stormy", "Hollow-Boned" → "Hollow Boned",
  "Bare-handed" → "Unarmed", "firework-rocket-style" → "like a firework rocket", "sure-footed" →
  "sure footed", "scale-like" → "scaly", and changed em dashes in tooltips to periods for
  consistency with established project style).

### Arachne

- Added **Practiced Landing** power: falls over 10 blocks deal 52% of normal damage, matching Feather Falling IV's damage reduction formula. Works in combination with Sure Footed (which still grants zero damage on falls under 10 blocks).
- Arachne now climbs 1.65x faster than everyone else. This raises the upward climb rate on ladders,
  vines, and any surface her climbing power lets her cling to, and it raises the sideways movement
  and downward slide rates on those surfaces by the same amount. Vanilla gives climbing a fixed
  speed with no attribute to adjust, so this is done in code (`ArachneClimbSpeedMixin`) and applies
  only to players who have chosen Arachne. Climbing on powder snow is deliberately left at the
  normal speed.
- Fang, Venomfang, and Widowfang now apply tiered Poison instead of a single flat tier: repeat
  qualifying hits on the same target within 8 seconds climb the amplifier up to each weapon's own
  cap (Fang stays at Poison I, Venomfang climbs to Poison II, Widowfang climbs to Poison III), then
  reset to Poison I once that window lapses without a follow up hit. Wither on Widowfang remains
  flat and untiered.
- Fang, Venomfang, and Widowfang durability is now an explicit override (500, 1500, and 3500 uses)
  instead of inheriting each tool tier's raw durability value.
- Corrected the tiered Poison escalation above to match the original design more closely, after a
  fresh comparison against the source design document. Fang now escalates through Tier 1, 2, and 3
  across three hits instead of staying capped at Tier 1 forever; Venomfang now applies Tier 2 on
  the very first hit and escalates to Tier 3 on a second hit instead of climbing from Tier 1 to
  Tier 2 across two hits; Widowfang now applies Tier 3 instantly on the very first hit, every time,
  instead of needing three hits to reach it. All three still share one underlying hit tracking
  sequence per target, exactly as before, so switching weapons mid fight still climbs the same
  count.
- Poison duration now scales with the tier actually applied: 200, 240, and 280 ticks (10, 12, and
  14 seconds) for Tier 1, 2, and 3, instead of a flat 60 ticks for every tier regardless of which
  one landed.
- Tier 2 and Tier 3 Poison from Fang, Venomfang, and Widowfang now pierce undead immunity; only
  Tier 1 keeps vanilla Poison's own real immunity to the undead. Bleed keeps its own, separate,
  unconditional undead exclusion, unaffected by this change.
- Added a half heart lock: any target currently affected by Arachne's tiered Poison that drops to
  half a heart (1 HP) or below now gets continuous Slowness and Blindness until it heals back above
  that threshold or dies. Since vanilla Poison already caps its own damage and can never actually
  finish a target off, this gives Arachne players a real window to close in and finish the job
  themselves.
- Added the **Silk Net Shooter**: a new throwable weapon crafted from 3 Silk, 20 uses. Anyone can
  craft and throw it, but the net's trap effect (a temporary cobweb plus a heavy 3 second
  Slowness) only triggers if the thrower has chosen Arachne, matching every other origin weapon in
  this mod. The net still flies and hits normally for anyone else, it just does nothing further.
  The cobweb reuses the same auto disappearing block Origins already places for the Master of Webs
  power, so it clears itself without any extra code on this end.
- Silk Net Shooter durability increased from 20 to 100 uses.

### Harpy

- Fixed the Harpy Javelin flying and landing looking like a plain vanilla trident. `ThrownTrident`
  (the class `ThrownJavelin` extends) never syncs its carried item to the client except through
  world save/load NBT, so a thrown javelin's client side copy kept a hardcoded vanilla trident
  stack for its whole flight and after sticking in the ground. `ThrownJavelin#getItem()` now
  always returns a fresh Harpy Javelin stack for rendering, matching the same fix already applied
  to the Silk Net Shooter's in flight render. Server side hit and pickup logic, which reads the
  real carried item directly, is unaffected.
- Hardy Stomach now grants a brief burst of Saturation instead of Regeneration when eating raw
  meat or rotten flesh, so the power reads as "this food fills you up safely" rather than
  granting a small heal on top of removing the Hunger penalty.
- Added the **Talon Gauntlets**: a new worn claw weapon family with three tiers, Iron, Diamond,
  and Netherite (1000, 2000, and 3500 durability). Anyone can craft and swing one, but for a Harpy
  wielder each tier adds 2, 3, or 4 hearts of its own damage plus the exact same bare fist bonus
  damage Talons already grants unarmed, and causes Bleed on hit, tiered the same way Fang's poison
  is: Iron always applies Bleed I, Diamond climbs from Bleed I to Bleed II on a second hit within 8
  seconds, and Netherite climbs all the way to Bleed III across three hits in that window. Crafted
  from 4 iron ingots, then upgraded with 4 diamonds, then upgraded again on a smithing table with a
  netherite ingot, matching Fang's own Diamond to Netherite convention.
- Added the **Storm Javelin**: throwing the Harpy Javelin from above Y=120, or right after a fall
  of 30 or more blocks (captured the instant it leaves the thrower's hand), now calls down a real
  lightning bolt at the impact point and deals bonus damage in a 10 block radius around it on
  landing, the same technique vanilla's own Channeling enchantment uses to spawn lightning. Gated
  behind a 1 minute cooldown per player so it can't be spammed.

### Mermaid

- Mermaid's Crown durability is now an explicit 1000 uses (previously the implicit diamond helmet
  default of 363), matching the explicit override convention every other weapon and armor piece in
  this mod uses. Its Regeneration also now only applies while the wearer is actually in water or
  rain, instead of unconditionally every 40 ticks regardless of surroundings.
- Dolphin's Grace toggle now applies amplifier 2 (was amplifier 1) and no longer shows its
  potion icon in the effects list, matching the already suppressed particles.

### Medusa
