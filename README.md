# Origin Mod Study

> ⚠️ **This project is an experiment.** It's a learning/hobby project for exploring how to build
> an addon for an existing Fabric mod (Origins) — not a polished, production-ready mod. Expect
> rough edges.

A **Minecraft 1.20.1 / Fabric** addon for [Origins](https://modrinth.com/mod/origins) that adds
the **Arachne** origin — a humanoid spider:

- Carnivore-only diet, can climb any surface, traverses/crafts cobwebs freely. Immune to poison.
- Dangerous creatures glow with an outline once spotted (tracking).
- ~60% of normal height/width (via [Pehkui](https://modrinth.com/mod/pehkui)).
- Attacks poison the target for 10 seconds (undead immune, 5s cooldown) and briefly web-trap them
  (2s cooldown on the web).
- Every arthropod (spider, cave spider, silverfish, endermite) is friendly — until you attack it.
- Night vision. 6 hearts of health, a small amount of natural armor.
- **Rappel** (double-tap space, same as Origins Minus's Weaver origin): pull yourself upward on a
  strand of web. Only works indoors, under a ceiling or overhang. Reuses Origins Minus's own
  `rappel` power directly rather than reimplementing it.
- **Scuttle** (secondary key, unbound by default): a burst of Speed IV for 4 seconds, at the cost
  of burning through food much faster while it's active. 15 second cooldown.
- **Latch On**: sneak + right-click a nearby player to climb onto them like prey. A perch (you ride
  them), not free movement — sneak again to let go.
- Can't use a shield.
- **Golden Spider Eye**: a new food item, the carnivore-diet equivalent of a golden apple (which
  Arachne can't eat — apples aren't meat). Same crafting cost (8 gold ingots around the center
  item) and the same buff (Regeneration II for 5s, Absorption I for 2 minutes), just built around
  a spider eye instead of an apple.

It's also written as a **worked, documented example** of a data-driven pattern for adding more
origins — see **[`TEMPLATE.md`](TEMPLATE.md)** if you want to add your own.

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
