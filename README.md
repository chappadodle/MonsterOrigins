# Monster Origins

> ⚠️ **This project is an experiment.** It's a learning/hobby project for exploring how to build
> an addon for an existing Fabric mod (Origins) — not a polished, production-ready mod. Expect
> rough edges. (In-game mod name: **Monster Origins**. Internally — mod ID, package names, file
> paths — it's still `arachne`/`originmodstudy`, unchanged from this project's original working
> name to avoid breaking any existing world that already has this mod's items/origins in it.)

A **Minecraft 1.20.1 / Fabric** addon for [Origins](https://modrinth.com/mod/origins) that adds
four origins — **Arachne** (a humanoid spider), **Medusa** (a gorgon), **Harpy** (a storm-wind
bird-woman), and **Siren** (a singer of the deep) — each built around a different playstyle:
fragile fast assassin, tanky bruiser, aerial skirmisher, and aquatic support/crowd-control.

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
- Takes 50% more damage from fire — brittle, dry, and flammable.
- Produces 6 string in your inventory every 20 minutes.
- **Quickness**: noticeably faster base movement speed (+35%, well past Avian's own barely-there
  +20% tailwind) — spiders are quick for their size. A flat attribute bonus, not a potion effect.
- **Golden Spider Eye**: a new food item, the carnivore-diet equivalent of a golden apple (which
  Arachne can't eat — apples aren't meat). Same crafting cost (8 gold ingots around the center
  item) and the same buff (Regeneration II for 5s, Absorption I for 2 minutes), just built around
  a spider eye instead of an apple.
- **Silk**: a new crafting material (4 string → 1 silk), craftable by anyone but thematically
  Arachne's own — no use yet, reserved for future recipes.

### Medusa

- Carnivore-only diet. 12 hearts of health, solid natural armor — a bruiser, not a glass cannon.
- **Petrifying Bite**: attacks briefly turn the target to stone — 3 seconds of heavy Slowness,
  Mining Fatigue, Blindness, and Darkness. 5 second cooldown.
- **Stone-Cursed**: immune to Slowness and Mining Fatigue — her own curse can't touch her.
- Immune to fall damage.
- Permanent night vision, but weakened by direct sunlight — a monster of the dark.
- **Dreadful Presence**: hostile creatures within 8 blocks are continuously weakened just by being
  near her.
- **Stone Gaze Burst** (secondary key, unbound by default): petrifies every hostile creature
  within 5 blocks (same effects as Petrifying Bite, including Darkness) for 3 seconds. 30 second
  cooldown.

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
- **Hardy Stomach**: raw meat and rotten flesh are perfectly safe to eat — no Hunger effect, just
  a brief 2-second burst of Regeneration.
- Takes 50% more damage from fire — hollow bones and feathers catch fast.

### Siren

- Carnivore-only diet. 90% of normal height/width (via Pehkui).
- **Fully at home in the water**: breathes, sees, and swims underwater with no trouble, and mines
  at full (Aqua Affinity) speed while submerged.
- **Landbound**: can stay out of water (rain counts as wet too) for 5 minutes at a time before
  starting to suffocate.
- **1.5x swim speed, 0.8x walking speed** — she belongs in the water, not on land.
- **Siren's Call** (primary key, unbound by default): a real singing sound effect, and a song that
  gives everyone nearby, including herself, Saturation and Regeneration — while hostile creatures
  caught in it are also slowed, blinded, and charmed into leaving everyone alone entirely for the
  duration. 4 second duration, 20 second cooldown.
- **Dolphin's Grace** (secondary key, unbound by default): toggles extra swim speed on or off —
  press once to go faster, press again to drop back down.
- **Every sea creature is friendly** unless attacked first — including Drowned and Guardians, not
  just the obviously-passive fish.
- **Siren's Crown**: exclusive armor, crafted from diamonds and a Heart of the Sea. Grants +2
  hearts and continuous Regeneration while worn.
- A new global recipe unlocks crafting a vanilla trident (3 prismarine shards + 2 sticks) —
  craftable by anyone, not exclusive to Siren, same recipe-can't-see-the-player limitation as
  every other weapon in this mod.
- No water-breathing bubble particles while it's active — just the effect, not the visual spam.

### Weapons

Every origin gets a themed craftable weapon. Anyone can craft or swing either one, but the bonus
on-hit effect only triggers for the matching origin — they're each origin's own signature weapon,
not generically useful loot:

- **Fang**: a light, fast dagger with a real dagger-shaped icon — a shortened diagonal blade built
  the same way vanilla's own sword icons are (alternating light/mid tones, a dark outline, a
  distinct guard), not a recolored sword — in three tiers, each with its own blade and grip
  color. Poisons on every hit, undead excluded, Arachne only. Every tier's tooltip spells out
  exactly what it does and who it's for.
  - **Fang** (base): iron-tier stats, Poison only. Crafted from a spider eye, an iron ingot, and a
    stick.
  - **Venomfang**: diamond-tier (6 attack damage), adds Bleed. Upgraded on a crafting table from a
    Fang + a diamond + a Golden Spider Eye.
  - **Widowfang**: netherite-tier (7 attack damage), adds Wither on top of Poison and Bleed.
    Upgraded on a **smithing table** from a Venomfang + a netherite ingot + another Golden Spider
    Eye — matching vanilla's own Diamond→Netherite upgrade convention rather than a plain
    crafting recipe.
- **Petrifying Trident**: a reskinned vanilla trident (same stats/behavior — throwable, riptide)
  that briefly petrifies whatever it hits (a lighter dose than Medusa's own Petrifying Bite) —
  Medusa only.
  Crafted from stone and iron ingots.
- **Harpy Javelin**: a real 3D-modeled throwing spear (a custom Blockbench model, not a flat
  icon), lighter and faster-swinging than the vanilla trident it's built on. Renders as its own
  model everywhere, including in flight and lying on the ground after landing — via a dedicated
  thrown-entity type and renderer, since (unlike the item model) that's hardcoded per-projectile
  in vanilla and not something a simple reskin gets for free. Causes Bleed on hit (undead
  excluded), whether thrown or swung, and deals bonus damage on a thrown hit landed while flying —
  Harpy only. Crafted from an iron ingot, a stick, and a feather.

All origin-gated weapons spell this out directly in their tooltip, not just in the README — no
player should have to guess whose weapon they're holding.

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
