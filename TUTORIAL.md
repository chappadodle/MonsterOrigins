# Origin Mod Study — A Beginner's Guide to Building an Origins Addon

Welcome! This tutorial walks through a complete, working **addon** for the
[Origins](https://modrinth.com/mod/origins) mod on **Minecraft 1.20.1 / Fabric**. It adds one new
playable origin, **Arachne** (a humanoid spider), and — just as importantly — explains the pattern
well enough that you could add a second, third, or tenth origin of your own afterward.

**The code is already written for you in this folder.** Your job is to set up the tools, learn
what each piece does and *why it's shaped the way it is*, run it, and then make it yours.

> **A note on "mappings."** Same situation as `mythicarsenal`: this project uses **Mojang's
> official mappings**, so vanilla Minecraft class names here (`ResourceLocation`, `Level`,
> `Player`) match Mojang's own naming. Origins' own GitHub source and docs use the older "Yarn"
> mapping set instead (`Identifier`, `World`, `PlayerEntity` for those same classes) — see
> **`TEMPLATE.md`** §4 for the full translation table if you ever write custom power code of your
> own and need to adapt a Yarn-based example.

---

## Phase 0 — Set up your environment

Same Java version balancing act as `mythicarsenal`: **Gradle needs JDK 21+ to run**, while
**Minecraft 1.20.1 itself is written for Java 17**. One modern JDK satisfies both — the build is
already configured to *compile for* Java 17 (`options.release = 17` in `build.gradle`).

```bash
sudo apt update
sudo apt install openjdk-21-jdk
javac -version         # must print: javac 21.x.x   (not "command not found")
ls /usr/lib/jvm         # expect something like: java-21-openjdk-amd64
```

Then point Gradle at it in `gradle.properties` (uncomment and adjust the path):

```properties
org.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64
```

*Why:* if you only have a Java **runtime** (JRE, no `javac`) on your system — which is what this
environment had going in — the build fails with *"does not provide the required capabilities:
[JAVA_COMPILER]"*. A JDK includes the compiler; a JRE doesn't.

You already have real copies of Origins, Origins Minus, Pehkui, and Fabric API installed via
PrismLauncher (the "SOLO origin" and "1.20.1" instances) — that's genuinely useful here, both as a
version reference (see `gradle.properties`) and as a ready place to drop this mod's built jar in
for real testing later (Phase 4).

---

## Phase 1 — What an Origins addon actually is

Origins itself is mostly a **data pack** with a small Java core. An "origin" (Human, Shulk,
Arachnid, and so on) is just a JSON file listing which **powers** a player gets when they pick it.
A power is *also* just a JSON file, picking one of dozens of built-in, generically-configurable
behaviors ("modify an attribute," "apply a status effect on hit," "make certain entities glow")
and filling in the specific numbers.

This matters a lot for how this addon is built: **most of "adding a new origin" is writing JSON,
not Java.** Out of Arachne's 8 original requirements (carnivore diet, web-climbing, tracking,
scaling, poison-on-hit, small size, friendly arthropods, night vision, reduced stats), **all but
one** were expressible as data. Only "arthropods stay friendly until attacked" genuinely needed
real code, because Origins has no built-in, data-driven way to change what a hostile mob is
willing to attack. That single piece of custom code is covered in Phase 3.

### 1.1 The project layout

```
origins-mod-study/
├─ gradle.properties              ← versions live here (Minecraft, Loader, Fabric API, Origins…)
├─ build.gradle                   ← how the project is built (you rarely touch this)
├─ TEMPLATE.md                    ← the pattern for adding MORE origins, with a decision checklist
└─ src/main/
   ├─ java/com/example/originmodstudy/
   │  ├─ OriginModStudy.java              ← the entrypoint (barely does anything — see why below)
   │  └─ mixin/ArthropodPassiveTargetMixin.java  ← the one piece of custom code (Phase 3)
   └─ resources/
      ├─ fabric.mod.json           ← the mod's manifest: id, dependencies, entrypoints
      ├─ arachne.mixins.json       ← tells Fabric which mixin classes to load
      └─ data/
         ├─ arachne/
         │  ├─ origins/arachne.json        ← the origin itself: name, icon, and its power list
         │  ├─ origins/example_stub.json   ← a blank starting point for your own next origin
         │  ├─ powers/arachne/*.json       ← the six custom powers (see Phase 2)
         │  └─ tags/entity_types/*.json    ← mob lists the powers reference (enemies, arthropods)
         └─ origins/origin_layers/origin.json  ← the file that puts Arachne in the picker (Phase 2)
```

Notice `OriginModStudy.java` is almost empty — it doesn't register any powers, items, or
anything else, because there's nothing to register. Everything is picked up automatically by
Origins' own data pack loader just by existing at the right file path. That's the payoff of the
data-driven approach.

---

## Phase 2 — Reading the data: how Arachne's powers actually work

Open `src/main/resources/data/monster_origins/origins/arachne.json`. Its `"powers"` array has 9 entries,
and they fall into two groups:

**Three are references to Origins' own built-in powers** — `origins:carnivore`,
`origins:climbing`, and `origins:master_of_webs`. These already exist inside the Origins mod
itself (the base game's own "Arachnid" origin uses the same three), so Arachne just points at
them by ID. Zero new files needed for three of the eight original requirements.

**Six are this addon's own custom powers**, one JSON file each under `powers/arachne/`:

- `night_vision.json`, `max_health.json`, `armor.json` — each picks a generic Origins power *type*
  (`origins:night_vision`, `origins:attribute`) and fills in Arachne-specific numbers. Compare
  `max_health.json`'s `-8.0` (20 HP → 12 HP, i.e. 6 hearts) against the real Shulk origin's own
  armor power (`+8.0` armor) that this pattern was modeled on — same power *type*, different
  numbers, different power.
- `tracking_glow.json` — makes anything in the `monster_origins:enemies` tag (a curated list of hostile
  mobs, in `tags/entity_types/enemies.json`) glow once you can see it, using the built-in
  `origins:entity_glow` power type with a `can_see` condition.
- `scale.json` — the 50%-size requirement. There's no built-in Origins↔Pehkui integration, so
  this power runs Pehkui's own `/scale set pehkui:base 0.5 @s` command automatically whenever the
  power is gained (and resets it to `1.0` when lost), via the generic `origins:action_on_callback`
  power type. Still zero custom Java — just telling an existing power type to run a command. Two
  non-obvious things had to be right for this to actually work, both covered in `CLAUDE.md`:
  Pehkui's `<scale_type>` argument needs the `pehkui:` namespace explicitly (a bare `base` silently
  resolves to `minecraft:base`, which doesn't exist), and the callback needs both the
  "just chosen" and "world (re)joined" trigger fields, not just one.
- `on_hit_poison.json` — 10 seconds of poison on your attacks, skipped for undead targets, with a
  5-second cooldown. Uses `origins:target_action_on_hit` (applies an action to whatever you hit)
  with a `target_condition` excluding the `undead` entity group.

Every one of these fields was checked against the real Origins mod's own documentation and source
before being written — see `CLAUDE.md`'s closing note on how, if you're curious or adding your
own.

Last piece: `data/origins/origin_layers/origin.json`. This is what actually makes Arachne
*appear* in the origin-picker GUI when you join a world or use an Orb of Origin — without it, the
origin would exist but be unreachable. `TEMPLATE.md` §2 explains exactly why this file lives at
that specific path.

---

## Phase 3 — The one piece of custom code

Open `src/main/java/com/example/originmodstudy/mixin/ArthropodPassiveTargetMixin.java`.

A **mixin** is Fabric's way of reaching into someone else's compiled code (here, vanilla
Minecraft's own mob AI) and inserting your own logic at a specific point, without needing that
code's source. This mixin hooks into `TargetGoal#canAttack` — the one method every hostile mob's
"should I go after this thing" AI goal calls before accepting a target — and cancels it when: the
mob is a spider, cave spider, silverfish, or endermite; the potential target is a player wearing
the Arachne origin; and that specific mob hasn't been hit by that specific player recently.

That last check is the neat part: it reads vanilla's own `getLastHurtByMob()` field, which every
living entity already tracks automatically whenever something damages it. No custom "has this mob
been attacked" bookkeeping was needed — vanilla was already keeping that information, this mixin
just had to ask for it in the right place.

---

## Phase 4 — Building and testing

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build
```

Output lands in `build/libs/originsmodstudy-<version>.jar`. To actually see Arachne in game, copy
that jar into one of your existing PrismLauncher instances' `mods/` folders (either "SOLO origin"
or "1.20.1" — both already have Origins, Origins Minus, Pehkui, and Fabric API installed at
matching versions) and launch that instance. Alternatively, `./gradlew runClient` launches a dev
copy of Minecraft directly with this mod loaded — but it needs a display, so it has to be run on
your own machine, not from here.

Things worth checking in game: does Arachne appear as a choice when you join a fresh world or use
an Orb of Origin; do its stats/abilities match the description text shown for each power; do
spiders/cave spiders/silverfish/endermite ignore you until you hit one, then fight back.

---

## Phase 5 — Adding more origins

That's what `TEMPLATE.md` is for — it's the same pattern this tutorial just walked through,
written as a standalone reference with a decision checklist ("does an existing power already do
this? can I configure an existing power type? does it need a command integration? only then: does
it need real code?"), plus `data/monster_origins/origins/example_stub.json` as a literal copy-paste
starting point.
