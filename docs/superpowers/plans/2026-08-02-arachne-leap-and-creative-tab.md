# Arachne Leap + Dedicated Creative Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Arachne's secondary-key "Scuttle" speed burst with a short forward-and-up leap, and give this mod's items their own dedicated creative-inventory tab instead of being scattered across vanilla tabs.

**Architecture:** Part 1 is a pure data change (one new Origins/Apoli power JSON replacing an old one, one origin-file reference update) — no Java. Part 2 is a small Java change to `ModItems.java`: register one new `CreativeModeTab` via Fabric API's `FabricItemGroup.builder()`, move every existing item registration into its `displayItems`, and delete the three vanilla-tab hooks.

**Tech Stack:** Fabric 1.20.1 / Mojang mappings, Origins/Apoli data-driven powers, Fabric API `fabric-item-group-api-v1`.

## Global Constraints

- Mappings are Mojang official, not Yarn — Java identifiers below use Mojmap names (`CreativeModeTab.Builder`, `BuiltInRegistries`, etc.), already confirmed present via `javap` against this project's own mapped jar.
- JSON-lint every new/edited resource file before building: `python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]"`.
- Build with `JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew build` (Gradle needs JDK 21+ to run even though the mod compiles for Java 17 — see this project's own CLAUDE.md).
- After a successful build, copy the built jar (`build/libs/monster_origins-<version>.jar`) into every PrismLauncher instance under `~/.local/share/PrismLauncher/instances/*/minecraft/mods/` that already has a `monster_origins-*.jar` installed, replacing the old file — this project's standing convention (a prior session in this same conversation already did this for the last change; the instance list may need re-checking with `find ~/.local/share/PrismLauncher/instances -iname "monster_origins-*.jar"` since it can drift).
- `bar_index` values across all of this mod's `hud_render` blocks must stay unique and within 0–8 (Apoli's shared `resource_bar.png` atlas only has real art for those indices — see CLAUDE.md).
- This is a headless environment with no display — nothing here can be playtested directly. Flag any behavior that needs real in-game verification in a code comment, and say so explicitly when reporting completion; do not claim gameplay feel is confirmed.

---

### Task 1: Replace Arachne's Scuttle power with Leap

**Files:**
- Delete: `src/main/resources/data/monster_origins/powers/arachne/scuttle.json`
- Create: `src/main/resources/data/monster_origins/powers/arachne/leap.json`
- Modify: `src/main/resources/data/monster_origins/origins/arachne.json`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: the power ID `monster_origins:arachne/leap`, referenced by Task 1's own change to `arachne.json`. No other task depends on this.

**Context:** Arachne's origin file currently lists `"origins_minus:rappel"` immediately before `"monster_origins:arachne/poison_immunity"` in its powers array, with `"monster_origins:arachne/scuttle"` appearing later in the list (read the file directly to find its exact current position before editing — do not assume order, confirm by reading). Scuttle currently applies a Speed IV burst for 4 seconds on the secondary key, with a companion `hungrier` sub-power that triples food exhaustion drain while the speed effect is active. The user wants this fully replaced with a leap: a one-shot forward-and-upward velocity burst, no residual speed effect, no exhaustion cost beyond the existing cooldown.

`origins:add_velocity` (the entity action used below) was verified directly against Apoli's own decompiled source (`io.github.apace100.apoli.power.factory.action.EntityActions`, the `"add_velocity"` registration): it takes plain float `x`/`y`/`z` fields, a `space` field (`"local"` converts the vector via the entity's own yaw before applying — confirmed in this project's CLAUDE.md that positive local `z` is forward, the opposite of vanilla's usual -Z-forward world-space convention, verified there via real playtest on Mermaid's Riptide dash), and a `set` boolean (`true` overwrites velocity outright rather than adding to it, same as Riptide dash uses). This project's existing `src/main/resources/data/monster_origins/powers/mermaid/riptide_dash.json` already uses this exact same power type (`origins:active_self`) and entity action type successfully — read it for the proven working shape before writing `leap.json` if anything below is unclear.

The velocity values (`z: 0.65`, `y: 0.7`) were derived by simulating vanilla's own real per-tick air physics — horizontal drag 0.91, gravity 0.08, vertical drag 0.98 per tick, all three already documented as vanilla's real constants in this project's CLAUDE.md from prior decompiles (`LivingEntity.travel`) — solving for the initial velocity that lands the entity roughly 5 blocks forward with roughly a 2.5-block peak height. The simulation (recurrence `vy = (vy - 0.08) * 0.98; y += vy` for vertical, `vh = vh * 0.91; x += vh` for horizontal, starting `vh=0.65, vy=0.7`) landed at ~5.1 blocks horizontal, ~2.4 blocks peak height, over ~15 ticks. This is a best-effort tuning only — this headless environment cannot playtest it. Say so in the code comment and in your final report; do not claim the feel is confirmed.

- [ ] **Step 1: Read the current files to confirm exact structure before editing**

Read `src/main/resources/data/monster_origins/origins/arachne.json` in full and note the exact line containing `"monster_origins:arachne/scuttle"` (its position in the powers array). Read `src/main/resources/data/monster_origins/powers/mermaid/riptide_dash.json` to confirm the `origins:active_self` / `origins:add_velocity` shape matches what's described above (it should — if it doesn't, stop and reconcile before proceeding, since this plan's JSON below assumes that shape is correct).

- [ ] **Step 2: Delete the old Scuttle power file**

```bash
rm "src/main/resources/data/monster_origins/powers/arachne/scuttle.json"
```

- [ ] **Step 3: Create the new Leap power file**

Create `src/main/resources/data/monster_origins/powers/arachne/leap.json`:

```json
{
	"type": "origins:active_self",
	"key": {
		"key": "key.origins.secondary_active",
		"continuous": false
	},
	"cooldown": 300,
	"entity_action": {
		"type": "origins:add_velocity",
		"space": "local",
		"set": true,
		"z": 0.65,
		"y": 0.7
	},
	"hud_render": {
		"should_render": true,
		"bar_index": 7
	},
	"name": "Leap",
	"description": "Press your secondary key (unbound by default, set it in Controls) for a short pounce forward and up, about 5 blocks. 15 second cooldown. (Distance is a best-effort estimate from vanilla's own drag/gravity physics, not confirmed by a real playtest yet.)"
}
```

`bar_index: 7` reuses Scuttle's old HUD slot — safe once Scuttle's own power file no longer exists, since nothing else in this mod uses index 7 (confirm this by grepping, next step).

- [ ] **Step 4: Confirm no bar_index collision**

```bash
grep -rn '"bar_index"' src/main/resources/data/ | sort -t: -k3
```

Expected: every distinct `bar_index` value across all power files is unique and in 0–8, with exactly one file (the new `leap.json`) using `7` (Scuttle's old file is gone).

- [ ] **Step 5: Update arachne.json's power list**

In `src/main/resources/data/monster_origins/origins/arachne.json`, change the array entry `"monster_origins:arachne/scuttle"` to `"monster_origins:arachne/leap"`, in the same position it currently occupies (do not reorder the rest of the list).

- [ ] **Step 6: JSON-lint the changed files**

```bash
python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]" && echo OK
```

Expected: `OK`, no exceptions.

- [ ] **Step 7: Grep to confirm no stray references to the old power remain**

```bash
grep -rn "arachne/scuttle\|Scuttle" src/main/resources/ src/main/java/
```

Expected: no output (or only unrelated coincidental matches — inspect anything that appears).

- [ ] **Step 8: Commit**

```bash
git add "src/main/resources/data/monster_origins/powers/arachne/leap.json" \
        "src/main/resources/data/monster_origins/origins/arachne.json"
git rm "src/main/resources/data/monster_origins/powers/arachne/scuttle.json"
git commit -m "Replace Arachne's Scuttle speed burst with a forward leap"
```

---

### Task 2: Add a dedicated creative-mode tab and move all items into it

**Files:**
- Modify: `src/main/java/com/example/originmodstudy/item/ModItems.java`
- Modify: `src/main/resources/assets/monster_origins/lang/en_us.json`

**Interfaces:**
- Consumes: nothing from Task 1 (independent change).
- Produces: nothing consumed by later tasks — this is the last task in this plan.

**Context:** `ModItems.java` currently declares every item as a `public static final Item` field (e.g. `GOLDEN_SPIDER_EYE`, `FANG`, `VENOMFANG`, `WIDOWFANG`, `PETRIFYING_TRIDENT`, `STORM_TRIDENT`, `HARPY_FEATHER`, `SILK`, `SILK_NET_SHOOTER`, `MERMAID_TRIDENT`, `MEDUSA_SCALE`, `SERPENT_AEGIS`, plus the four icon-only `ARACHNE_EYE`/`MEDUSA_EYE`/`HARPY_EYE`/`MERMAID_EYE`), and its `registerModItems()` method (called once from `OriginModStudy.onInitialize()`) currently adds a subset of those items into three separate *vanilla* creative tabs via `ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS / .COMBAT / .INGREDIENTS)`. Read the current file in full before editing — the exact current item lists in each of those three blocks are the full set to move (do not add the four `*_EYE` items; they stay excluded from any creative tab, per this file's own existing comments explaining why).

Register one new `CreativeModeTab` field via Fabric API's `FabricItemGroup.builder()` (`net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup`, already available since this project already depends on Fabric API). This returns vanilla's own `CreativeModeTab.Builder` (confirmed via `javap` against this project's mapped jar: has `.title(Component)`, `.icon(Supplier<ItemStack>)`, `.displayItems(CreativeModeTab.DisplayItemsGenerator)`, `.build()`). `DisplayItemsGenerator.accept` has signature `(CreativeModeTab.ItemDisplayParameters, CreativeModeTab.Output)` (also confirmed via `javap`); `CreativeModeTab.Output` has a default `accept(ItemLike)` method, so `output.accept(ModItems.FANG)` works directly since `Item` implements `ItemLike` — no need to construct `ItemStack`s by hand for the display list.

Register the tab the same way this file already registers items — `Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, OriginModStudy.id("monster_origins"), ...)` (mirrors the existing `private static Item register(String name, Item item)` helper's own `Registry.register(BuiltInRegistries.ITEM, OriginModStudy.id(name), item)` call, just against the `CREATIVE_MODE_TAB` registry instead of `ITEM`).

- [ ] **Step 1: Read the current file in full**

Read `src/main/java/com/example/originmodstudy/item/ModItems.java` completely and copy its exact current imports and the exact contents of the three `ItemGroupEvents.modifyEntriesEvent` blocks inside `registerModItems()` — these define the full list of items to move into the new tab.

- [ ] **Step 2: Add the new imports**

Add these imports to `ModItems.java` (alongside the existing `import` block):

```java
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
```

(`Registries`/`ResourceKey` are only needed if `Registry.register` requires a `ResourceKey<CreativeModeTab>` rather than a bare `ResourceLocation` for this registry — confirm which overload actually compiles in Step 4; if the plain `ResourceLocation` overload works, as it does for every other `register` call already in this file, drop the unused `Registries`/`ResourceKey` imports again before committing.)

- [ ] **Step 3: Declare the new creative tab field**

Add this field to `ModItems.java`, positioned after all the existing item field declarations (so every `ModItems.XXX` reference inside it resolves against an already-initialized field) and before the private `register` helper method:

```java
// Playtest follow-up (2026-08-02): a dedicated tab for this mod's own content, replacing the
// old approach of scattering items into vanilla Combat/Food/Ingredients tabs. The four origin
// "eye" icon items (ARACHNE_EYE/MEDUSA_EYE/HARPY_EYE/MERMAID_EYE) are deliberately excluded here
// too, same as before — they exist purely to give the origin picker GUI an icon and were never
// meant to appear in any creative tab.
public static final CreativeModeTab MONSTER_ORIGINS_TAB = Registry.register(
		BuiltInRegistries.CREATIVE_MODE_TAB,
		OriginModStudy.id("monster_origins"),
		FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.monster_origins.monster_origins"))
				.icon(() -> new ItemStack(WIDOWFANG))
				.displayItems((parameters, output) -> {
					output.accept(GOLDEN_SPIDER_EYE);
					output.accept(FANG);
					output.accept(VENOMFANG);
					output.accept(WIDOWFANG);
					output.accept(PETRIFYING_TRIDENT);
					output.accept(STORM_TRIDENT);
					output.accept(SILK_NET_SHOOTER);
					output.accept(MERMAID_TRIDENT);
					output.accept(SERPENT_AEGIS);
					output.accept(SILK);
					output.accept(MEDUSA_SCALE);
					output.accept(HARPY_FEATHER);
				})
				.build());
```

This needs `import net.minecraft.world.item.ItemStack;` too if `ModItems.java` doesn't already import it — check the existing import list first (it likely doesn't, since no field currently constructs an `ItemStack` directly).

- [ ] **Step 4: Remove the three vanilla-tab registration blocks**

In `registerModItems()`, delete the three `ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS/.COMBAT/.INGREDIENTS)` blocks entirely (every item they added is now covered by `MONSTER_ORIGINS_TAB`'s `displayItems` above). If `registerModItems()`'s body becomes empty as a result, keep the method itself (it's still called from `OriginModStudy.onInitialize()` and still serves its documented purpose of forcing this class to load at a predictable point — see the existing class doc comment, same convention as `ModEffects.registerModEffects()`), just with an empty body plus a short comment noting registration now happens entirely via the `static final` field initializers above (item fields and the new tab field alike).

If `ItemGroupEvents` and/or `CreativeModeTabs` are no longer referenced anywhere else in the file after this deletion, remove their now-unused imports too.

- [ ] **Step 5: Add the tab's lang key**

In `src/main/resources/assets/monster_origins/lang/en_us.json`, add a new entry (matching the file's existing flat key-value JSON convention — insert it near the other top-level/non-item keys, e.g. alongside the `effect.monster_origins.*` block already there):

```json
	"itemGroup.monster_origins.monster_origins": "Monster Origins",
```

Make sure the preceding line gets a trailing comma if it didn't already have one, and that this new line does NOT have a trailing comma if it's now the last entry in the object (check the file's actual current last line before deciding).

- [ ] **Step 6: JSON-lint the lang file**

```bash
python3 -c "import json,glob;[json.load(open(f)) for f in glob.glob('src/main/resources/**/*.json', recursive=True)]" && echo OK
```

Expected: `OK`, no exceptions.

- [ ] **Step 7: Build to confirm the Java compiles and the mixin/registration pass succeeds**

```bash
JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew build
```

Expected: `BUILD SUCCESSFUL`, with no new warnings beyond the two pre-existing benign ones already documented in this project's CLAUDE.md (the `remapSourcesJar`/`ArthropodPassiveTargetMixin` warning and the `Env.CLIENT` annotation-processor warning). If `Registry.register` against `BuiltInRegistries.CREATIVE_MODE_TAB` doesn't accept a plain `ResourceLocation` as its key argument, the compiler error will name the expected type directly (likely `ResourceKey<CreativeModeTab>`) — if so, wrap it: `ResourceKey.create(Registries.CREATIVE_MODE_TAB, OriginModStudy.id("monster_origins"))` in place of the bare `OriginModStudy.id("monster_origins")` call, keeping the `Registries`/`ResourceKey` imports from Step 2 in that case (drop them only if the plain form compiled).

- [ ] **Step 8: Grep to confirm no leftover references to the removed vanilla-tab blocks**

```bash
grep -n "CreativeModeTabs\." src/main/java/com/example/originmodstudy/item/ModItems.java
```

Expected: no output (all three vanilla-tab references removed) — or, if `CreativeModeTabs` genuinely isn't imported/used at all anymore, this returns nothing by construction.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/example/originmodstudy/item/ModItems.java \
        src/main/resources/assets/monster_origins/lang/en_us.json
git commit -m "Add a dedicated Monster Origins creative tab, replacing scattered vanilla tabs"
```

---

### Task 3: Full build verification and deploy to test instances

**Files:** none (verification-only task).

**Interfaces:**
- Consumes: the completed state of Tasks 1 and 2.
- Produces: a built jar deployed to PrismLauncher test instances, ready for the user's own in-game playtest.

- [ ] **Step 1: Clean build**

```bash
JAVA_HOME=~/.local/jdks/temurin-21 ./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`, same two pre-existing benign warnings as always, no others.

- [ ] **Step 2: Confirm the jar version/filename**

```bash
ls -la build/libs/
```

Note the exact `monster_origins-<version>.jar` filename (no `-sources` suffix) for the next step.

- [ ] **Step 3: Deploy to every PrismLauncher instance that already tracks this mod**

```bash
find ~/.local/share/PrismLauncher/instances -iname "monster_origins-*.jar" -not -iname "*-sources.jar"
```

For each match found, remove the old jar from that instance's `mods/` folder and copy in the newly built one (same filename pattern as the one just removed, i.e. replace, don't accumulate both versions side by side).

- [ ] **Step 4: Report to the user what needs real playtest verification**

State explicitly (this is not optional, per the Global Constraints section above): the leap's landing distance/height (`z: 0.65`/`y: 0.7`) is a physics-simulation estimate, not confirmed in-game — the user should try it and report back if it needs retuning (e.g. multiply both values by a constant factor to scale distance up/down while roughly preserving the forward/height ratio). Also confirm the new creative tab actually shows up and displays the expected items with the expected icon, since this environment cannot open the creative inventory to check.
