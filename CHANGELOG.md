# Changelog

All notable player-facing changes to Monster Origins are recorded here, one bullet per feature
change, grouped by origin. Infrastructure/cleanup work with no direct in-game effect gets its own
section instead of an origin's.

## [Unreleased]

## [1.1.1] - 2026-07-29

### Harpy

- Added a stamina cost to flight: the first 60 seconds of any continuous flight are free, then
  saturation starts draining until she lands. Balances permanent flight against every other
  origin's own tradeoffs.

## [1.1.0] - 2026-07-29

First official release. Everything below was developed and playtested prior to this tag; nothing
had been published before now.

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
- Venomfang's recipe is now shaped (Golden Spider Eye on top, Diamond in the middle, Fang on the
  bottom) instead of shapeless, same ingredients as before.
- Silk Glands now produces 3 silk every 20 minutes, down from 6.
- Silk Net Shooter's recipe now mirrors the vanilla fishing rod exactly (3 sticks in the same
  shape), with 2 silk in place of the rod's 2 string, instead of 3 plain silk with no shape.

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
- Removed Scream (its cone-knockback, and the custom action type that implemented it) and
  replaced it with a **Slow Falling** toggle on the same secondary key — press to turn Slow
  Falling on, press again to turn it off. Toggles vanilla's own `minecraft:slow_falling` directly,
  the same effect serving as both the real mechanic and its own on/off marker (no new custom
  effect needed), same pattern as Mermaid's Dolphin's Grace toggle.
- Redesigned the Talon Gauntlets into a single talon/claw look (a curved claw blade with a small
  finger-ring band, rather than a full hand-covering gauntlet) — a visual and naming change only
  (dropped "Gauntlet" from the display name); the three tiers, their stats, and their mechanics are
  unchanged. New hand-drawn icon per tier, since there's no existing vanilla asset shaped like a
  single claw to recolor from.
- Wielding a Talon (any tier) now gives a block *shorter* reach than normal, not the standard
  reach — "or else it's too op," per the source doc. Same client-side reach-mixin technique as the
  Living Coral Trident's own +1 and Medusa's Innocent Form's own -1.
- Flying is now roughly 50% faster in normal level flight, capped at a fixed maximum speed so it
  can never run away no matter how long a flight lasts. (A first draft of this — reapplying a flat
  1.5x multiply to velocity every single tick — would have compounded into an unbounded runaway
  acceleration bug; caught by working through the math before shipping it, not by observing it
  happen, since this environment can't playtest a flight mechanic. See CLAUDE.md for the full
  reasoning.) Report back if the cap feels too low or too high — that's a different number to tune
  than the speed multiplier itself.
- Halved the flight speed boost above (1.5x multiplier down to 1.25x, per your own choice between
  halving the multiplier vs. the speed cap).
- Fixed the Slow Falling toggle actually doing nothing: Glide (the passive drift-down-slowly
  power) never checked the vanilla `minecraft:slow_falling` effect the toggle applied, so toggling
  it had zero observable effect either way. Glide now checks a new dedicated marker effect the
  toggle controls directly, so the secondary key genuinely turns the permanent glide on and off.
  Sneaking still always drops you out of the glide and falls you normally, exactly as before.
- Fixed climbing (flying upward) being noticeably easier than descending. This was really the same
  bug as the toggle above: Glide's permanent downward velocity cap was still fighting descent
  regardless of the toggle state, since the toggle never actually reached it. Now that Glide is
  properly gated behind the toggle, turning it off removes that cap entirely, so up and down should
  feel symmetric.
- The primary key ability (previously Sudden Gust, a burst of extra speed) is now a no-cooldown
  toggle that disables and re-enables your own flight entirely, so you can land more easily instead
  of gliding forever. Sudden Gust stopped being useful once permanent flight made a one-off speed
  burst redundant, so it's been replaced rather than kept alongside flight.
- Removed the Talon weapon (all three tiers, Iron/Diamond/Netherite) entirely, along with its
  reach penalty and bare-fist-bonus/Bleed mechanics — "I realised that it would be too much,"
  per your own message. Talons (the innate unarmed bonus damage) is unaffected; only the wearable
  weapon item family is gone.
- The Harpy Javelin now deals bonus damage that scales with how fast you're currently flying,
  instead of a flat +3 whenever simply airborne: up to +7 (base 7, up to 14 total) at or above the
  same speed ceiling flight itself is capped at. Applies to both a thrown hit and, newly, a melee
  hit while flying (previously melee got no airborne bonus at all, only the thrown case did).
- Added the **Harpy Feather**: a new crafting material Harpy naturally sheds (3 every 10 minutes),
  same mechanic as Arachne's Silk Glands and Medusa's Scale Shedding.
- Reworked the Harpy Javelin's recipe: Diamond on top, Iron Ingot in the middle, Harpy Feather on
  the bottom, replacing the old Iron Ingot/Stick/Feather shape.
- Replaced the Harpy Javelin's item model and texture, first with a plain flat icon, then
  (correcting course after real playtesting showed it no longer looked 3D in hand) with vanilla's
  own real Trident rendering mechanism: the Javelin now reuses vanilla's actual Trident geometry
  (`ModelLayers.TRIDENT`) with its own texture, the same "copy the java shield" technique used for
  the Serpent Aegis, rather than a flat icon or a hand-authored custom model. New texture painted
  directly onto vanilla's real Trident UV layout (32x32) via a Blockbench plugin that reads
  `.minecraft`'s own files, based on a copy of a newer Minecraft version's own "Lance" weapon art.
- The Harpy Javelin now also renders this same real 3D geometry mid-flight and stuck in a
  target, automatically, since the thrown-item renderer draws through the same registered
  renderer as the held/inventory view.
- Fixed the Javelin (and Coral Trident, and Petrifying Trident) rendering "ridiculously small"
  once stuck in a block — a display-context double-scaling bug introduced by the switch to real
  Trident geometry above, not present before.
- Fixed the third-person "holding it backwards" look while charging a throw — the Javelin (and
  Coral Trident, and Petrifying Trident) now has its own real "charging a throw" pose, matching
  vanilla's own Trident, instead of just staying in its normal holding pose the whole time.
- Removed the flight-disable/glide-disable toggles entirely and simplified Harpy back down to
  always-on flight (matching base Origins' own reference `elytra_flight` power exactly, with no
  gating condition) — the toggle mechanism introduced this round turned out to leave flight and
  Glide both completely non-functional, and rather than keep chasing the exact cause blind, the
  whole mechanism was removed per the user's own call. Flight works unconditionally again, no
  keybind required.
- Fixed the Serpent Aegis never losing durability when it actually blocked a hit ("indestructible")
  — vanilla's own real durability-loss-on-block code only ever checks for the literal vanilla
  Shield item by identity, the same kind of exact-identity check already found for 3D shield/
  trident rendering, just in a different vanilla method this time.
- Replaced Glide (the slow-falling drift) with **Featherfall**: flat, unconditional immunity to
  fall damage, no drift at all. Simpler than the removed toggle, and matches "hollow bones" flavor
  better than a persistent float-down effect.
- Renamed the Harpy Javelin to the **Storm Trident**, matching the existing Storm Javelin
  lightning ability's own naming — it uses vanilla's real Trident geometry now and looks like a
  trident, not a javelin. Same item otherwise: same recipe, same stats, same abilities.
- Fixed thrown/stuck Storm Tridents (and Coral Tridents, and Petrifying Tridents) rendering
  **floating above the surface** instead of embedded. The thrown-item renderer was reusing the
  held-item rendering code (which includes a coordinate flip specific to that context) for the
  in-flight/stuck visual too — real vanilla's own thrown-trident renderer never goes through that
  code path at all. Rewritten to render the shared Trident model directly, the same way vanilla's
  own thrown-trident visual does, with no leftover flip or offset.
- Fixed the Storm Trident/Coral Trident/Petrifying Trident showing full 3D geometry in the
  hotbar and looking "ridiculously small" when dropped on the ground — both were the same root
  cause: real vanilla's Trident has a hardcoded special case making it look flat specifically in
  the hotbar/dropped-item/item-frame views and full 3D everywhere else, which a plain custom item
  doesn't get for free. All three weapons now replicate that split properly: flat icon in the
  hotbar and when dropped, real 3D when actually held.
- Fixed Harpy flight "catching momentum too quickly and never decelerating" — holding the
  backward (S) key while flying now actively bleeds off speed instead of just capping the top
  speed lower, giving a real way to slow down mid-flight rather than a lower ceiling to run into.
- Fixed the Storm Trident/Coral Trident/Petrifying Trident showing up as a missing texture in the
  hotbar and inventory — a real bug in the previous fix (a wrong model resource path), not a wrong
  texture file after all.
- Raised the flight speed ceiling again, 1.8 → 2.7 (50% more), but changed how you get there:
  speed now builds up gradually over several seconds of sustained flight (like a car accelerating)
  instead of snapping to nearly the ceiling within a couple of ticks.
- Reworked flight momentum into three real states instead of two: holding forward (W) actively
  builds speed toward the cap, holding backward (S) brakes, and holding neither now lets speed
  drift back down on its own instead of holding perfectly steady — matching how a real glide
  should feel, not "accelerate unless braking."
- Raised the flight speed ceiling again to a clean 300% of baseline elytra speed, and changed the
  buildup curve to match the braking curve's own feel: a big jump in speed right as you start
  holding forward, tapering off smoothly as you approach the cap, instead of a flat per-tick
  increase the whole way.
- Renamed the "thrown from high up or after a big fall" Storm Trident lightning requirement to
  just "thrown after a 30+ block fall" — dropped the separate absolute-height alternative
  entirely, one clear requirement instead of two.
- Removed the Storm Trident lightning's fall-distance requirement entirely — throwing it and
  landing a hit now always calls down lightning, gated only by its own 30 second per-player
  cooldown (down from 1 minute).
- Fixed Loyalty not working on any modded trident-style weapon (Storm Trident, Living Coral
  Trident, Petrifying Trident) — a real, long-standing bug, not something new this round.

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
- Added the **Living Coral Trident**: a new late-game weapon, 3500 durability, crafted from Tube
  or Horn Coral, Prismarine Crystals, and a Prismarine Shard. Anyone can craft and throw it, but
  its traits below only trigger for Mermaid, matching every other origin weapon in this mod:
  Symbiosis (a small hunger restore on every hit that lands), Barbed Tip (bonus damage against a
  target that's currently in water), Bleeding Current (Bleed on hit while the wielder is in
  water), and a heavy underwater "Metal Hit" sound layered on top of the normal trident sound
  whenever the hit lands on an underwater target. Also leaves a bubble trail while flying through
  water and, matching the Harpy Javelin's own fix, uses a dedicated thrown entity + renderer so it
  shows its own model in flight and stuck in the ground instead of a plain vanilla trident.
  Grants a genuine +1 block reach while held, via a client-side mixin on
  `MultiPlayerGameMode.getPickRange()` rather than the third-party Reach Entity Attributes mod
  Origins' own equivalent power depends on (confirmed via `gh api`) — that mod isn't one of this
  project's dependencies or the user's installed mods, and vanilla 1.20.1 itself has no reach
  attribute at all (that was only added in Minecraft 1.21).
- A Mermaid mining a live coral block bare-handed (or with any tool lacking Silk Touch) now gets
  the live block instead of vanilla's dead-coral fallback; killing a fish (Cod, Salmon,
  Pufferfish, or Tropical Fish) as Mermaid now has a 20% chance to additionally drop a Prismarine
  Shard. Both are plain Java event hooks (`PlayerBlockBreakEvents`/`ServerLivingEntityEvents`),
  since neither has a data-driven path through Origins/Apoli.
- The Living Coral Trident's 3D model (Blockbench-authored by the user) needed one real fix
  before use: the supplied file was a raw Blockbench project save, not an exported vanilla model —
  its `texture_size` field (and the UV numbers authored against it) are a Blockbench-editor-only
  convention, confirmed completely absent from vanilla's own compiled model-loading code and from
  every vanilla model file in the game jar itself (grepped the whole client+server jar for the
  literal string, zero matches). Vanilla always reads UV coordinates in a fixed 16-unit space
  (`FaceBakery`'s own hardcoded `16.0f` constants, confirmed via `javap`), so every face's UV
  rectangle was rescaled by `16 / texture_size` before shipping, and the non-functional
  `texture_size` field itself was dropped. This is a mechanical, provably-correct unit conversion,
  not a guess — but whether the artwork itself lines up pixel-for-pixel on the model is something
  only a real render can confirm, and this environment has no display to check that with; please
  look at it in-game and let me know if the coral/prismarine texture needs further UV adjustment.
- Replaced the Living Coral Trident's model with the user's own re-exported, corrected version,
  then the same fix again with a further re-export ("sus.json") — the same two pieces
  ("Small mid"/"Small up mid", a real 90° single-axis rotation Minecraft's format can't represent
  at all) and four smaller pieces at 5°/-7.5°/-17.5° needed the same correction both times: the
  90° pieces got their box coordinates recomputed directly via the exact rotation formula
  (lossless for an axis-aligned box, with their face-texture assignments remapped to match) rather
  than an approximate angle snap, and the smaller pieces were snapped to their nearest allowed
  angle. UVs rescaled by 16/32 again, same reasoning as before.
- Fixed the Living Coral Trident rendering "incredibly small" both in flight and stuck in a
  target. The thrown-weapon renderer was deliberately using `ItemDisplayContext.NONE` (raw model
  space) to avoid `GROUND`'s old camera-billboard problem, but that also skipped the trident's own
  `display.ground` entry, which relies on a `scale: [1.7, 1.7, 1.7]` to render at its intended
  size at all — without it, the model rendered at its tiny "true" scale. Switched to
  `ItemDisplayContext.GROUND`: confirmed neither weapon's model has a rotation baked into that
  display entry (only scale/translation), so it layers safely on top of the custom
  flight-direction rotation without fighting it, and the Harpy Javelin (which has no `ground`
  entry at all) is unaffected either way.
- Flipped the sign of the "stick out further" translation from the previous fix — real
  playtesting showed it made both weapons render roughly 90% buried, the opposite of intended,
  confirming the post-rotation local axis didn't map the way the first attempt assumed.
- Found and fixed a real bug in this project's own trident-model conversion step (not something
  the user did wrong): the model's actual face data references texture slot `#1`, but the
  conversion was unconditionally writing a `textures` block keyed `0`, so every face's texture
  reference resolved to nothing. Now reads whichever key(s) the faces actually reference before
  building the `textures` block. Paired with the user's latest matched model+texture export
  (`unhappy.json`/`unhappy.png`) — the true underlying cause of the missing/red-only texture
  turned out to be separate from this bug (see below) but this was a real, independent defect
  regardless.
- Confirmed (via a generated overlay of every face's real UV rectangle onto the actual texture
  image, not guessed) that the missing cyan / all-red look is a genuine mismatch between the
  model's own UV unwrap and where the texture's cyan accents are actually painted: every face's UV
  rectangle is packed into a small corner of the texture that is entirely red, regardless of which
  of the three exports provided so far is used. This needs a real UV unwrap (or texture repaint)
  in Blockbench — it isn't fixable by further conversion-script changes on this end.
- Adopted the user's `veryunhappy.json`/`veryunhappy.png` pair, which repaints cyan directly into
  that same small used corner (confirmed visually before adopting it) — same rotation/UV/texture-key
  conversion applied as every prior model swap.
- Found and fixed the actual "missing texture in some parts" cause: checked every face's real
  sampled pixel region against the texture directly (not guessed) and found 10 faces landing on
  fully or mostly transparent pixels — genuine see-through holes, not a rendering bug. Repainted
  the whole small used region (an 18x18 pixel corner) with a deliberate cyan border/trim around a
  red-toned interior, using the exact colors already present in the user's own texture, with zero
  remaining transparency — replacing the scattered, partially-transparent hand-painted pass.
- Fixed the Harpy Javelin and Living Coral Trident always rendering "vertical" in flight and stuck
  in the ground, regardless of throw direction — both used `ThrownItemRenderer` (vanilla's own
  base class for snowballs/eggs/ender pearls), which billboards the item to face the camera and
  renders it with a fixed "on the ground" transform, never the entity's own flight-direction
  rotation. A new shared `DirectionalThrownItemRenderer` reproduces vanilla's real
  `ThrownTridentRenderer` rotation technique directly (decompiled to confirm) so both weapons now
  visibly point in their real throw direction and stick in at the angle they hit, matching a real
  trident. Two follow-up fixes to that same renderer, both reported from real playtesting: it
  landed blade-up (shaft planted in the ground) instead of blade-down, fixed with an extra 180° on
  the pitch term (vanilla's own rotation offsets were tuned for vanilla's own baked trident model,
  not these mods' differently-authored ones, so there was no reason the same numbers would point
  the right way); and a brief rapid spin right at the moment of impact, fixed by switching from
  vanilla's own plain (and, it turns out, imprecise) `Mth.lerp` to the angle-aware `Mth.rotLerp`
  for the rotation interpolation, which correctly handles an angle jump across the 0°/360° wrap
  instead of always spinning the long way around. Also fixed both weapons burying most of
  themselves into whatever they hit instead of just the tip: item-model rendering always pivots at
  the geometric center of the nominal 16-unit item cube, but the javelin's own model (deliberately
  oversized, extending from -4.25 to 32 on its long axis) put a third of its shaft on the wrong
  side of that pivot. A small added translation (computed from the model's own real coordinates,
  not guessed) shifts the pivot further toward the tip, so more of the shaft now protrudes.
- Mermaid's Call now also applies Weakness (amplifier 0, 4 seconds) to caught hostiles alongside
  Slowness/Blindness/Charmed. The source doc's own reasoning for this ("Weakness would help them")
  doesn't parse mechanically — Weakness only reduces the *target's* own attack damage, so it can
  only hurt an enemy, never help one survive — flagged and confirmed with the user rather than
  guessed: applies to every hostile caught in the song, including the undead, since Weakness has no
  vanilla undead-immunity precedent to match in the first place (unlike Poison/Regeneration).
- Removed the Mermaid Crown entirely — "quite useless," per the source doc.
- Swimming speed raised again, from 3x to 5.7x normal walking speed. Also fixed the real cause of
  "still feels slow/sliding" that a bigger speed number alone couldn't touch (flagged but never
  built in an earlier phase): vanilla applies a flat, hardcoded 20% per-tick momentum loss to
  everyone in water regardless of their actual swim speed stat, which is what actually produces
  the sluggish, hard-to-turn feeling. A new mixin reduces that retention loss to near-zero for
  Mermaid specifically, so turning/steering in water should now feel responsive, not just
  "technically fast in a straight line."
- Changed the Living Coral Trident's recipe again: 3 Prismarine Shards on top, a Fire Coral Block
  in the middle, and a Diamond on the bottom, replacing the Heart of the Sea + Diamond + Prismarine
  Shard shapeless recipe from the previous round.
- Removed the Living Coral Trident's custom "Metal Hit" underwater sound effect entirely — not
  needed, per the user. Symbiosis and Bleeding Current (its other on-hit traits) are unaffected.
- The Living Coral Trident now renders as real 3D vanilla Trident geometry when held, thrown, and
  stuck in a target — the same fix (and the same root cause) as the Harpy Javelin's own entry
  above, resolving the long-running UV/texture mismatch for good this time. New texture painted
  directly onto vanilla's real Trident UV layout (32x32) via the same Blockbench plugin technique.
- Corrected the Mermaid-only prismarine shard fish drop chance to 5%, down from 20% — a mistake in
  the original source doc's own numbers, caught by the user.
- Swimming speed raised again, from 5.7x to 7.41x normal walking speed (30% more).
- Fixed why none of the swim-speed bumps this whole time ever actually felt faster: the earlier
  water-turning fix only applied while *not* sprint-swimming — but sprint-swimming is the normal
  way anyone actually swims fast, and it hardcodes vanilla's own water drag to a fixed value that
  bypasses the fix entirely. It now applies regardless of whether you're holding sprint.
- Fixed a regression from that same fix's first attempt: forcing sprint detection off while
  swimming also silently disabled the actual swimming pose/animation, making Mermaid move through
  water stuck in the upright walking pose instead of swimming — she now swims properly again,
  with the drag fix still applying underneath it.
- Replaced the secondary key's Dolphin's Grace toggle with a **Riptide dash**: a quick forward
  burst on a 5 second cooldown, matching the exact push strength of a real Riptide III
  enchantment, for a quick way to travel faster instead of a passive buff.
- Mermaid now takes 50% more damage from fire, matching the same fire-vulnerability treatment
  Arachne and Harpy already have.

### Medusa

- The Petrifying Trident's petrify now only triggers on a **thrown** hit, not a melee swing (a
  melee hit deals plain vanilla trident damage). Durability raised from 250 to 1000, matching this
  mod's explicit-override convention.
- **Stone Gaze Burst** (secondary key): radius 5 → 10 blocks, duration 3 → 5 seconds, and its
  effect stack switched from raw Slowness/Mining Fatigue to the upgraded `Petrify` effect (already
  used by Petrifying Bite/the trident) + Blindness + Darkness. Now also plays a boulder-impact
  sound on use.
- Added **Stone Gaze**: a new passive power — hostile creatures that wander within 6 blocks are
  turned briefly to stone (3 seconds), once every 8 seconds per creature (a one-shot-then-cooldown
  gate using a marker effect, the same technique this project's other one-shot powers already use).
- Fixed Stone Scales' armor value: 6.0 → 8.0. The old value rendered as 3 armor bars (2 points per
  icon); 8.0 is what actually produces the intended 4 bars.
- Added **Innocent Form**: a new primary-key toggle disguising Medusa as an ordinary person —
  half health, a block shorter reach, 80% walking speed, and no longer weakened by direct
  sunlight, but villagers refuse to trade with her (and flinch away, scared) and iron golems see
  through the disguise and attack. The reach reduction reuses the same client-side mixin technique
  as the Living Coral Trident's own reach bonus (see CLAUDE.md); the villager/golem reactions are
  real Java hooks, since Origins/Apoli has no data-driven path to either.
- Added the **Serpent Aegis**: a new off-hand item (5000 durability) built on vanilla's own real
  shield mechanics (raise, block, disable-on-axe-hit) — blocking a melee attack with it slows the
  attacker (Slowness IV, ~2 seconds), no effect against projectiles, since that's simply what a
  plain shield already does. Crafted from a new **Snake Scale** material, which Medusa naturally
  sheds 6 of every 10 minutes (the same mechanic/interval as Arachne's own Silk Glands).
- Removed Dreadful Presence entirely ("gives weakness in an area, that's bad," per the source doc).
- Petrifying Bite now only triggers unarmed or while wielding the Petrifying Trident, instead of
  on a hit with any weapon at all.
- Fixed the Petrifying Trident always rendering as a plain vanilla trident in flight and stuck in
  a target, regardless of throw direction — same root cause and same fix already used for the
  Harpy Javelin (a dedicated thrown entity + the shared directional renderer), since vanilla's own
  thrown-trident visual hardcodes its texture/model regardless of the real carried item. Melee
  swings were already unaffected either way.
- Stone Gaze Burst also affects villagers now, in addition to hostiles — a deliberate testing aid
  requested in the source doc, so the petrify effect can be verified on a common, easy-to-find mob.
- Remade the Serpent Aegis's texture: a composite of vanilla's own real shield surface texture
  blended with the Snake Scale texture and grey accents, replacing the earlier placeholder
  (a recolored GUI empty-slot icon, not the shield's own real material).
- Renamed Snake Scale to **Medusa Scale**, with a new dark-scaled icon. Same item, same recipe,
  same 6-per-shed drop rate — just the name, icon, and internal id.
- The Serpent Aegis now actually renders as a real 3D shield — raised, blocking, on your back, all
  of it — instead of a flat icon. It reuses vanilla's own shield model and geometry directly (same
  plate/handle shape every vanilla shield has), just with its own texture and the existing
  Medusa-gated Slowness-on-block effect layered on top, unchanged.
- The Petrifying Trident now also renders as real 3D Trident geometry, same as the Harpy Javelin
  and Living Coral Trident got this round — its own stone-grey texture, generated as a recolor of
  vanilla's real Trident texture rather than hand-painted, since it has no user-authored asset.
- Removed **Innocent Form** entirely — the disguise toggle (half health, shorter reach, slower
  gait, no sunburn) turned out to be "quite useless and was a bad idea," per the user. Iron golems
  seeing through Medusa and villagers refusing to trade/fleeing in fear are no longer tied to that
  toggle at all — they're unconditional now, all the time, regardless of form.
- Removed the direct-sunlight weakness from Nocturnal Menace entirely — permanent night vision
  stays, the sunburn penalty (and the debugging work that would have gone into fixing it) is gone.
- Stone Gaze and Stone Gaze Burst now also petrify iron golems, not just tagged hostile mobs (and,
  for the Burst, villagers) — fitting now that golems are always hostile toward Medusa.
- The Serpent Aegis now reflects half the damage from a blocked melee hit back at the attacker,
  thorns-enchantment style, on top of the existing Slowness. Durability changed to an explicit
  3000 (was 5000). Ghast fireballs no longer cost the shield any durability at all when blocked.
- Fixed damage reflection not actually working — a real bug in the block-handling mixin (it had
  the attacker and defender roles swapped), caught by the user's own real playtest.
- Fixed the Petrifying Trident's own tooltip claiming melee swings don't petrify — they do,
  through the separate Petrifying Bite power (which fires on any hit while wielding it), just not
  through the trident's own direct effect. Tooltip now describes what you actually see happen.
- The Serpent Aegis now protects more than just Medusa herself: any other player within 6 blocks
  takes half damage from anything while she's actively blocking with it, no facing requirement —
  a real shield hitbox isn't a thing vanilla actually has (blocking is a pure facing-direction
  check, confirmed via decompile), so this is a new AOE mechanic built to match the request
  rather than a literal hitbox resize.


