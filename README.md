# Origin Mod Study

> ⚠️ **This project is an experiment.** It's a learning/hobby project for exploring how to build
> an addon for an existing Fabric mod (Origins) — not a polished, production-ready mod. Expect
> rough edges.

A **Minecraft 1.20.1 / Fabric** addon for [Origins](https://modrinth.com/mod/origins) that adds
three origins — **Arachne** (a humanoid spider), **Medusa** (a gorgon), and **Harpy** (a
storm-wind bird-woman) — each built around a different playstyle: fragile fast assassin, tanky
bruiser, and aerial skirmisher.

### Arachne

- Carnivore-only diet, can climb any surface, traverses/crafts cobwebs freely. Immune to poison.
- Dangerous creatures glow with an outline once spotted (tracking).
- ~50% of normal height/width (via [Pehkui](https://modrinth.com/mod/pehkui)).
- Attacks poison the target for 10 seconds (undead immune, 5s cooldown) and briefly web-trap them
  (2s cooldown on the web).
- Every arthropod (spider, cave spider, silverfish, endermite) is friendly — until you attack it.
- Night vision. 6 hearts of health, a small amount of natural armor.
- **Rappel** (double-tap space, same as Origins Minus's Weaver origin): pull yourself upward on a
  strand of web. Only works indoors, under a ceiling or overhang. Reuses Origins Minus's own
  `rappel` power directly rather than reimplementing it.
- **Scuttle** (secondary key, unbound by default): a burst of Speed IV for 4 seconds, at the cost
  of burning through food much faster while it's active. 15 second cooldown.
- Can't use a shield.
- No fall damage from falls under 10 blocks.
- Weakened while touching water — a spider out of its element.
- Produces 6 string in your inventory every 20 minutes.
- **Golden Spider Eye**: a new food item, the carnivore-diet equivalent of a golden apple (which
  Arachne can't eat — apples aren't meat). Same crafting cost (8 gold ingots around the center
  item) and the same buff (Regeneration II for 5s, Absorption I for 2 minutes), just built around
  a spider eye instead of an apple.

### Medusa

- Carnivore-only diet. 12 hearts of health, solid natural armor — a bruiser, not a glass cannon.
- **Petrifying Bite**: attacks briefly turn the target to stone — 3 seconds of heavy Slowness,
  Mining Fatigue, and Blindness. 5 second cooldown.
- **Stone-Cursed**: immune to Slowness and Mining Fatigue — her own curse can't touch her.
- Immune to fall damage.
- Permanent night vision, but weakened by direct sunlight — a monster of the dark.
- **Dreadful Presence**: hostile creatures within 8 blocks are continuously weakened just by being
  near her.
- **Stone Gaze Burst** (secondary key, unbound by default): petrifies every hostile creature
  within 5 blocks (same effect as Petrifying Bite) for 3 seconds. 30 second cooldown.

### Harpy

- Carnivore-only diet. 8 hearts of health — light, hollow bones. Can't use a shield.
- **Glide**: drift down slowly when falling; hold sneak to drop out of the glide and fall
  normally.
- **Wings**: true flight, always — no elytra item needed.
- **Sudden Gust** (primary key, unbound by default, hold while flying): a firework-rocket-style
  boost in whatever direction you're currently facing, not a launch into the air. 3.5 second
  cooldown — effectively unlimited flight.
- **Talons**: bare-handed strikes deal 2.5 extra hearts of damage and inflict **Bleed** for 10
  seconds (undead immune) — a brand new status effect added by this mod, ticking just like Poison
  (same real tick-damage timing, verified against the game's actual source) but — unlike Poison —
  it can kill.
- **Scream** (secondary key, unbound by default): a shrieking blast (with a Sonic Boom particle
  burst) that knocks back whatever's in front of you within 8 blocks — behind you is completely
  unaffected. 10 second cooldown. Uses a real barn owl screech recording (CC BY-SA 4.0, credited
  in [`CREDITS.md`](CREDITS.md)).
- 85% of normal height/width (via Pehkui).

### Weapons

Both origins get a themed craftable weapon. Anyone can craft or swing either one, but the bonus
on-hit effect only triggers for the matching origin — they're each origin's own signature weapon,
not generically useful loot:

- **Fang**: a light, fast dagger (built on iron's tier, less damage than an iron sword but a
  faster swing). Poisons on every hit, undead excluded — Arachne only. Crafted from a spider eye,
  an iron ingot, and a stick.
- **Petrifying Trident**: a reskinned vanilla trident (same stats/behavior — throwable, riptide)
  that briefly petrifies whatever it hits (a lighter dose than Medusa's own Petrifying Bite) —
  Medusa only.
  Crafted from stone and iron ingots.

It's also written as a **worked, documented example** of a data-driven pattern for adding more
origins — see **[`TEMPLATE.md`](TEMPLATE.md)**. Medusa is the second worked example (the first
was Arachne); Harpy is the third, and the first origin in this mod that genuinely needs custom
Java beyond a single mixin — a new status effect (Bleed) and a custom Apoli action type (Scream's
forward-cone knockback), since neither has any data-driven path. See `CLAUDE.md` for exactly why.

## Requirements (to play)

Drop these into your `mods/` folder (all for **1.20.1 Fabric**):

1. [Fabric API](https://modrinth.com/mod/fabric-api) (`0.92.x+1.20.1`)
2. [Origins](https://modrinth.com/mod/origins) (`1.10.2+mc.1.20.1`)
3. [Origins Minus](https://modrinth.com/mod/origins-minus) (`2.4.0+1.20`)
4. [Pehkui](https://modrinth.com/mod/pehkui) (`3.8.3+1.14.4-1.21`)
5. This mod's jar (build it with `./gradlew build`, output in `build/libs/`)

## Building / developing

See **[`TUTORIAL.md`](TUTORIAL.md)** — a full beginner walkthrough (what an Origins addon even
is, environment setup, the data-pack-first approach, and how the one piece of custom code works).
Needs **JDK 21+** (the Gradle toolchain requires it; the mod itself targets Java 17).

```bash
./gradlew build       # compile + build the mod jar
./gradlew runClient   # launch a dev Minecraft with the mod loaded
```

## Credits & licenses

- Built against [Origins](https://github.com/apace100/origins-fabric) and
  [Origins Minus](https://github.com/sisby-folk/origins-minus) by their respective authors — this
  mod is an addon for their work, not affiliated with them.
- This mod's own code is available under CC0 (the Fabric template's license).
- Harpy's Scream sound is a real recording used under CC BY-SA 4.0 — see
  [`CREDITS.md`](CREDITS.md) for full attribution.
