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
- Replaced the Harpy Javelin's item model with a new, more detailed 3D model.

### Mermaid

- Mermaid's Crown durability is now an explicit 1000 uses (previously the implicit diamond helmet
  default of 363), matching the explicit override convention every other weapon and armor piece in
  this mod uses. Its Regeneration also now only applies while the wearer is actually in water or
  rain, instead of unconditionally every 40 ticks regardless of surroundings.
- Dolphin's Grace toggle now applies amplifier 2 (was amplifier 1) and no longer shows its
  potion icon in the effects list, matching the already suppressed particles.
- Swift Current now gives 3x normal swimming speed (was 1.5x). Derived from scratch rather
  than doubling the old JSON value directly: the `additionalentityattributes:water_speed`
  attribute this power modifies is computed from the current, already slowed land speed every
  tick (confirmed via decompile in earlier work), so Landlegs' own 0.8x walking penalty bleeds
  through into the water number. Solving for the `multiply_base` value that lands on exactly 3x
  base walking speed after that 0.8x bleed through gives 2.75 (`0.8 * (1 + 2.75) = 3.0`), not the
  naively doubled 1.75. Investigated the separate "feels like gliding on ice" complaint too:
  decompiling vanilla's own `LivingEntity.travel()` and Additional Entity Attributes' mixin shows
  the water speed attribute only affects how hard the player accelerates each tick, never the
  fixed 0.8 (0.9 while sprinting) per tick momentum retention vanilla hardcodes for everyone in
  water. That retention constant is what actually produces the sliding, hard to turn feeling, and
  it cannot be changed by any power or attribute, only by a mixin overriding the vanilla method
  outright, a real code change bigger than a data tuning task should make on its own. Flagging
  this back rather than shipping a data change that would not actually fix the complaint it is
  meant to fix.
- Added a bubble style HUD bar showing how much of the 5 minute out of water grace period is left
  before dehydration damage begins, visible only while playing Mermaid and only once any of that
  time has passed. Checked first whether Origins or Apoli already has a bar for this (the same way
  `merling.json` was checked before Mermaid's other aquatic powers were built) and confirmed they
  only let a power reskin existing bars, not add a new one driven by a custom timer. Also confirmed
  the countdown itself is not something client code can read directly: the power that runs the out
  of water damage keeps its own timer as a private value that is never sent to the client, unlike
  every other stateful power type checked in the same source. Rather than add new server to client
  syncing just for this bar, the client instead keeps its own count of how long the player has been
  out of water and rain, using the same 5 minute delay and the same 1 second grace window before a
  brief return to water resets it, driven from the same "in water or rain" check other Mermaid
  gameplay code already uses. This tracks the real timer under normal play since both sides are
  watching the same thing, and simply resets to safe on rejoin or origin change rather than ever
  showing a stale reading.
- Confirmed, unchanged: Landbound's out of water damage already treats rain as equivalent to
  standing in water (its own condition already includes `origins:in_rain` alongside the water
  check) and a bucket of water placed on the ground already refills the grace period, since the
  power only checks fluid height at the player's position, which a placed water source satisfies
  the same way a natural one does. No changes needed here.

### Medusa
