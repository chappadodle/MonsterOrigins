package com.example.originmodstudy.item;

import com.example.originmodstudy.OriginModStudy;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

/**
 * Registers every item this mod adds. Fields are {@code static final} so they are
 * created (and registered) the moment this class is first referenced.
 */
public class ModItems {

	// Arachne's carnivore diet (origins:carnivore) blocks vanilla golden apples, since apples
	// aren't meat. This is the meat-diet equivalent: same crafting cost and a matching buff
	// (Regeneration II for 5s, Absorption I for 2 minutes), tagged into origins:meat so the
	// carnivore power actually permits eating it.
	public static final Item GOLDEN_SPIDER_EYE = register("golden_spider_eye",
			new Item(new Item.Properties().food(
					new FoodProperties.Builder()
							.nutrition(4)
							.saturationModifier(1.2F)
							.effect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1.0F)
							.effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1.0F)
							.build())));

	// Exists purely to carry a texture for the origin's icon in the origin-picker GUI (Origins'
	// "icon" field only accepts a registered item ID, not a raw texture path). Not craftable, not
	// added to any creative tab — see CLAUDE.md.
	public static final Item ARACHNE_EYE = register("arachne_eye", new Item(new Item.Properties()));

	// Same purpose as ARACHNE_EYE above, for the Medusa origin's icon.
	public static final Item MEDUSA_EYE = register("medusa_eye", new Item(new Item.Properties()));

	// Same purpose as ARACHNE_EYE above, for the Harpy origin's icon.
	public static final Item HARPY_EYE = register("harpy_eye", new Item(new Item.Properties()));

	// Same purpose as ARACHNE_EYE above, for the Mermaid origin's icon.
	public static final Item MERMAID_EYE = register("mermaid_eye", new Item(new Item.Properties()));

	// A light, fast dagger — built on iron's tier but with a lower damage modifier (2, vs. the
	// vanilla iron sword's 3) and a faster attack speed modifier (-1.8, vs. the vanilla sword
	// default -2.4), trading raw damage for swing speed to feel like a dagger rather than a sword.
	// First of three tiers — see FangItem's class doc for the full progression. Starts at Poison
	// Tier 1 (basePoisonTier 1) and escalates all the way to Tier 3 across three hits, same as the
	// other two tiers, just starting lower; durability is an explicit override (500), not Iron's
	// raw 250, same technique HARPY_JAVELIN/PETRIFYING_TRIDENT use for their own explicit overrides.
	public static final Item FANG = register("fang",
			new FangItem(Tiers.IRON, 2, -1.8F, 1, false, false, new Item.Properties().durability(500)));

	// Second tier: diamond-tier (modifier 3 + diamond's 3.0 bonus = 6 attack damage, same -1.8
	// speed as Fang), applies Poison Tier 2 on the very first hit and escalates to Tier 3 on a
	// second qualifying hit, adds Bleed on top. Upgraded from Fang on a crafting table. Durability
	// overridden to 1500 (not Diamond's raw 1561).
	public static final Item VENOMFANG = register("venomfang",
			new FangItem(Tiers.DIAMOND, 3, -1.8F, 2, true, false, new Item.Properties().durability(1500)));

	// Third and final tier: netherite-tier (modifier 3 + netherite's 4.0 bonus = 7 attack damage,
	// same speed again), applies Poison Tier 3 instantly on the very first hit, every time, no
	// escalation needed, adds Wither (flat, untiered) on top of Poison and Bleed. Upgraded from
	// Venomfang on a smithing table (smithing_transform), matching vanilla's own Diamond->Netherite
	// convention. Durability overridden to 3500 (not Netherite's raw 2031).
	public static final Item WIDOWFANG = register("widowfang",
			new FangItem(Tiers.NETHERITE, 3, -1.8F, 3, true, true, new Item.Properties().durability(3500)));

	// Vanilla trident stats/behavior (throwable, riptide, etc.), reskinned, petrify on a thrown
	// hit only (Task 15). Durability bumped from 250 to 1000, matching this mod's explicit-override
	// convention for every other weapon/armor piece.
	public static final Item PETRIFYING_TRIDENT = register("petrifying_trident",
			new PetrifyingTridentItem(new Item.Properties().durability(1000)));

	// A lighter, faster throwing spear than the vanilla trident (6.0 damage / -2.4 speed instead
	// of 8.0 / -2.9) — Harpy's weapon. Bleed on hit and a bonus while thrown mid-flight only
	// trigger for the Harpy origin; see HarpyJavelinItem and ThrownTridentMixin. Renamed to "Storm
	// Trident" (matching the Storm Javelin lightning ability's own naming) once it switched to
	// real vanilla Trident geometry and stopped looking like a javelin — item id and display name
	// only, the Java class/entity/renderer names are unchanged internal implementation detail.
	public static final Item STORM_TRIDENT = register("storm_trident",
			new HarpyJavelinItem(new Item.Properties().durability(200)));

	// Harpy sheds these every 10 minutes (feather_shedding.json), same mechanic/pattern as
	// Arachne's Silk Glands and Medusa's Scale Shedding. A plain crafting material, no
	// functionality of its own — used in the Harpy Javelin's own recipe.
	public static final Item HARPY_FEATHER = register("harpy_feather", new Item(new Item.Properties()));

	// A plain crafting material, no functionality yet — deliberately reserved for future recipes.
	// Craftable by anyone (same recipe-can't-see-the-player limitation as every other weapon in
	// this mod, see OriginUtil), just thematically Arachne's own material.
	public static final Item SILK = register("silk", new Item(new Item.Properties()));

	// Arachne's Silk Net Shooter: fires a ThrownSilkNet that traps its target in a temporary
	// cobweb plus a short heavy Slowness. 20 uses modeled as durability (not a consumable stack),
	// matching every other weapon's explicit-override convention in this mod — see SilkNetShooterItem
	// and the task report for why durability was chosen over stack count.
	public static final Item SILK_NET_SHOOTER = register("silk_net_shooter",
			new SilkNetShooterItem(new Item.Properties().durability(100)));

	// The Living Coral Trident (Task 13): plain vanilla trident stats, Mermaid's own late-game
	// weapon. 3500 durability, matching this mod's explicit-override convention for every
	// weapon/armor piece rather than a tier's raw default. See MermaidTridentItem for its four
	// on-hit traits (Symbiosis, Barbed Tip, Bleeding Current, +1 reach).
	public static final Item MERMAID_TRIDENT = register("mermaid_trident",
			new MermaidTridentItem(new Item.Properties().durability(3500)));

	// Medusa sheds these every 20 minutes (Task 19, scale_shedding.json), same mechanic/interval as
	// Arachne's own Silk Glands. A plain crafting material, no functionality of its own.
	public static final Item MEDUSA_SCALE = register("medusa_scale", new Item(new Item.Properties()));

	// Medusa's off-hand Serpent Aegis: a real vanilla ShieldItem subclass, 3000 durability
	// (explicit override, same convention as every other weapon/armor piece in this mod, not
	// Shield's own raw default). Blocking a melee attack slows the attacker and reflects some of
	// the blocked damage back, gated on the Medusa origin — see SerpentAegisBlockMixin. Immune to
	// Ghast fireballs specifically (no durability loss) — see SerpentAegisDurabilityMixin.
	public static final Item SERPENT_AEGIS = register("serpent_aegis",
			new SerpentAegisItem(new Item.Properties().durability(3000)));

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

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, OriginModStudy.id(name), item);
	}

	public static void registerModItems() {
		// Registration now happens entirely via the static final field initializers above
		// (item fields and the MONSTER_ORIGINS_TAB creative tab field alike). This method is
		// kept solely to force this class to load at a predictable point, called from
		// OriginModStudy.onInitialize() — same convention as ModEffects.registerModEffects().
	}
}
