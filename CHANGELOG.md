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
- Added the **Silk Net Shooter**: a new throwable weapon crafted from 3 Silk, 20 uses. Anyone can
  craft and throw it, but the net's trap effect (a temporary cobweb plus a heavy 3 second
  Slowness) only triggers if the thrower has chosen Arachne, matching every other origin weapon in
  this mod. The net still flies and hits normally for anyone else, it just does nothing further.
  The cobweb reuses the same auto disappearing block Origins already places for the Master of Webs
  power, so it clears itself without any extra code on this end.

### Harpy

### Mermaid

### Medusa
