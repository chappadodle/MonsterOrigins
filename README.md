# Monster Origins

A **Minecraft 1.21.1 / Fabric** addon for [Origins](https://modrinth.com/mod/origins) that adds
four origins: **Arachne** (a humanoid spider), **Medusa** (a gorgon), **Harpy** (a swift, stormy
bird woman), and **Mermaid** (a singer of the deep). Each one plays completely differently: a
fragile fast assassin, a tanky bruiser, an aerial skirmisher, and aquatic support with crowd
control.

### Arachne

- Carnivore only diet, can climb any surface (1.65x normal climb speed), traverses and crafts
  cobwebs freely. Immune to poison.
- Dangerous creatures glow with an outline once spotted, for as long as you can see them.
- About 85% of normal height and width (via [Pehkui](https://modrinth.com/mod/pehkui)).
- Attacks poison the target for 10 seconds (undead immune, 5s cooldown).
- Every arthropod (spider, cave spider, silverfish, endermite) is friendly until you attack it.
- Night vision. 6 hearts of health, a small amount of natural armor.
- **Rappel** (double tap space, a mechanic originally inspired by Origins Minus's Weaver origin,
  now built in natively): pull yourself upward on a strand of web. Only works indoors, under a
  ceiling or overhang.
- **Leap** (secondary key, unbound by default): a short pounce forward and up, about 5 blocks.
  3 second cooldown.
- Can't use a shield.
- Never takes fall damage, no matter how far she drops.
- Weakened while touching water, a spider out of its element.
- Takes 50% more damage from fire: brittle, dry, and flammable.
- Naturally produces 3 silk every 20 minutes.
- **Quickness**: +35% base movement speed, well past a token buff. Spiders are quick for their
  size. A flat attribute bonus, not a potion effect.
- **Golden Spider Eye**: a new food item, the carnivore diet equivalent of a golden apple (which
  Arachne can't eat since apples aren't meat). Same crafting cost (8 gold ingots around the center
  item) and the same buff (Regeneration II for 5s, Absorption I for 2 minutes), just built around
  a spider eye instead of an apple.
- **Silk**: a new crafting material (3 string makes 1 silk), craftable by anyone but thematically
  Arachne's own, used in the Silk Net Shooter's own recipe (see Weapons below).

### Medusa

- Carnivore only diet. 12 hearts of health, 8 armor points: a bruiser, not a glass cannon.
- **Petrifying Bite**: unarmed strikes, or hits with the Petrifying Trident, briefly turn the
  target to stone (3 seconds of heavy Slowness, Mining Fatigue, Blindness, and Darkness). 5 second
  cooldown.
- **Stone Cursed**: immune to Slowness and Mining Fatigue. Her own curse can't touch her.
- Immune to fall damage.
- Permanent night vision, a monster of the dark.
- **Stone Gaze Burst** (secondary key, unbound by default): petrifies every hostile creature (plus
  villagers and iron golems) within 10 blocks for 5 seconds, with a boulder impact sound. 30 second
  cooldown.
- Naturally sheds 6 Medusa Scales every 10 minutes (used in the Serpent Aegis's own recipe).
- Iron golems see through her and attack on sight; villagers refuse to trade with her and flinch
  away in fear whenever she gets close.

### Harpy

- Carnivore only diet. 8 hearts of health, light hollow bones. Can't use a shield.
- **Featherfall**: never takes fall damage, no matter how far she drops.
- **Wings**: true flight, always. No elytra item needed. Holding forward while flying gradually
  builds speed up to a cap of 300% of normal elytra speed (a quick initial jump, tapering off as
  you approach the cap); holding backward actively brakes; letting go of both lets speed drift
  back down on its own. Gliding at a steady speed is free; actively gaining speed costs hunger,
  so flight has a real cost on long trips.
- **Talons**: bare handed strikes deal 4 extra hearts of damage and inflict **Bleed** for 10
  seconds (undead immune), a status effect added by this mod that ticks like Poison but, unlike
  Poison, can kill. The faster she's flying when a hit (thrown or melee) lands, the more bonus
  damage on top, up to +7 extra hearts at full flight speed.
- 85% of normal height and width (via Pehkui).
- **Hardy Stomach**: raw meat and rotten flesh are perfectly safe to eat, no Hunger effect, just a
  brief burst of Saturation instead.
- Takes 50% more damage from fire: hollow bones and feathers catch fast.
- Naturally sheds 3 feathers every 10 minutes (used in the Storm Trident's own recipe).

### Mermaid

- Carnivore only diet. 90% of normal height and width (via Pehkui).
- **Fully at home in the water**: breathes, sees, and swims underwater with no trouble, and mines
  at full (Aqua Affinity) speed while submerged.
- **Landbound**: can stay out of water (rain counts as wet too) for 5 minutes at a time before
  starting to suffocate. A HUD bar shows how much of that time is left, once any of it has passed.
- **Swift Current**: much higher swimming speed, and turns/steers as responsively as walking on
  land does, not the sluggish drift vanilla swimming normally has.
- **Landlegs**: 0.8x normal walking speed. She belongs in the water, not on land.
- **Mermaid's Call** (primary key, unbound by default): a real singing sound effect. Everyone
  nearby, including herself, is bathed in Saturation and Regeneration, while hostile creatures
  caught in it are slowed, blinded, weakened, and charmed into leaving everyone alone entirely for
  the duration. 4 second duration, 20 second cooldown.
- **Riptide** (secondary key, unbound by default): a quick forward burst, the same push strength
  as a real Riptide III enchantment, for a fast way to get moving. 5 second cooldown.
- Mining a live coral block bare handed (or with any tool lacking Silk Touch) gets the live block
  instead of vanilla's usual dead coral fallback.
- Killing a fish (Cod, Salmon, Pufferfish, or Tropical Fish) has a 5% chance to additionally drop
  a Prismarine Shard.
- Takes 50% more damage from fire: dries out fast.
- No water breathing bubble particles while it's active, just the effect, not the visual spam.

### Weapons & armor

Every origin gets a themed craftable weapon. Anyone can craft or swing any of them, but the bonus
on hit effect only triggers for the matching origin. Each is its own origin's signature weapon,
not generically useful loot, and every one spells out exactly what it does and who it's for
directly in its tooltip:

- **Fang**: a light, fast dagger with a real dagger shaped icon: a shortened diagonal blade built
  the same way vanilla's own sword icons are, not a recolored sword. Three tiers, each with its
  own tiered Poison (escalating on repeated hits within 8 seconds), Bleed, and Wither. Arachne only.
  - **Fang** (base, iron tier): Poison. Crafted from a spider eye, an iron ingot, and a stick.
  - **Venomfang** (diamond tier, 6 attack damage): adds Bleed. Upgraded on a crafting table from a
    Fang, a diamond, and a Golden Spider Eye.
  - **Widowfang** (netherite tier, 7 attack damage): adds Wither on top. Upgraded on a **smithing
    table** from a Venomfang, a netherite ingot, and another Golden Spider Eye.
- **Silk Net Shooter**: a throwable weapon (100 uses) that traps its target in cobwebs and heavy
  Slowness on a hit. Arachne only. Crafted from silk and sticks.
- **Petrifying Trident**: vanilla trident stats and behavior (throwable, riptide compatible), but
  petrifies whatever it hits, thrown or melee. Medusa only. Crafted from stone and iron ingots.
- **Serpent Aegis**: a real off hand shield (3000 durability). Blocking a melee attack slows the
  attacker and reflects half the blocked damage back at them; nearby allies take half damage from
  anything while she's actively blocking with it. Immune to Ghast fireballs. Medusa only. Crafted
  from Medusa Scales and iron ingots.
- **Storm Trident**: a real 3D modeled throwing spear built on vanilla's actual Trident geometry,
  lighter and faster swinging than a plain vanilla trident. Causes Bleed on hit (undead excluded),
  whether thrown or swung, plus flight speed scaled bonus damage. A thrown hit that lands also
  calls down a real lightning bolt with area damage, on a 30 second cooldown. Harpy only. Crafted
  from a diamond, an iron ingot, and a Harpy Feather.
- **Living Coral Trident**: Mermaid's own late game weapon (3500 durability), also built on real
  vanilla Trident geometry. Symbiosis (small hunger restore on every hit), Barbed Tip (bonus
  damage against a target currently in water), Bleeding Current (Bleed on hit while she's in
  water), and +1 block reach while held. Mermaid only. Crafted from prismarine shards, a fire
  coral block, and a diamond.

Every trident style weapon (Storm Trident, Living Coral Trident, Petrifying Trident) renders as
real 3D geometry when held, thrown, and stuck in a target, but as a flat icon in the hotbar and
when dropped on the ground, matching vanilla's own real Trident behavior exactly.

It's also written as a **worked, documented example** of a data driven pattern for adding more
origins. See **[`TEMPLATE.md`](TEMPLATE.md)**. See `CLAUDE.md` for the full technical history and
every gotcha found along the way.

## Requirements (to play)

Drop these into your `mods/` folder (all for **1.21.1 Fabric**). Each version below is a
**minimum**, not an exact pin: any newer release of the same mod for 1.21.1 works too, matching
what this mod's own `fabric.mod.json` actually declares.

1. [Fabric API](https://modrinth.com/mod/fabric-api) (`0.116.15+1.21.1` or newer)
2. [Origins](https://modrinth.com/mod/origins) (`1.13.0-pre.3+mc.1.21.1` or newer, currently the
   newest build available for this Minecraft version. No stable, non prerelease Origins build
   exists for 1.21.1 yet, so a prerelease is the real requirement here, not a corner being cut)
3. [Pehkui](https://modrinth.com/mod/pehkui) (`3.8.3+1.14.4-1.21` or newer, the same jar already
   used on the 1.20.1 release, its own versioning already spans both)
4. This mod's jar (build it with `./gradlew build`, output in `build/libs/`)

Java 21 or newer is required to even launch this version, both for Gradle itself and for the mod's
own compiled bytecode, since 1.21.1 era Fabric mods require it (confirmed directly from Origins'
own `fabric.mod.json`).

## Building / developing

See **[`TUTORIAL.md`](TUTORIAL.md)** for a full beginner walkthrough: what an Origins addon even
is, environment setup, the data pack first approach, and how the custom code pieces work. Needs
**JDK 21+**, both for Gradle itself and for the mod's own compile target.

```bash
./gradlew build       # compile + build the mod jar
./gradlew runClient   # launch a dev Minecraft with the mod loaded
```

## Credits & licenses

- Built against [Origins](https://github.com/apace100/origins-fabric) by its authors. This mod
  is an addon for their work, not affiliated with them.
- This mod's own code, data, and textures are available under CC0 (the Fabric template's
  license), with two exceptions: its two sound effects are royalty free stock audio, not CC0.
  See [`CREDITS.md`](CREDITS.md) for details.
