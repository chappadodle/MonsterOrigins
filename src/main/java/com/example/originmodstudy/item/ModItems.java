package com.example.originmodstudy.item;

import com.example.originmodstudy.OriginModStudy;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
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
							.saturationMod(1.2F)
							.meat()
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

	// Vanilla trident stats/behavior (throwable, riptide, etc.), reskinned, with petrify-on-hit.
	public static final Item PETRIFYING_TRIDENT = register("petrifying_trident",
			new PetrifyingTridentItem(new Item.Properties().durability(250)));

	// A lighter, faster throwing spear than the vanilla trident (6.0 damage / -2.4 speed instead
	// of 8.0 / -2.9) — Harpy's weapon. Bleed on hit and a bonus while thrown mid-flight only
	// trigger for the Harpy origin; see HarpyJavelinItem and ThrownTridentMixin.
	public static final Item HARPY_JAVELIN = register("harpy_javelin",
			new HarpyJavelinItem(new Item.Properties().durability(200)));

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

	// Mermaid's exclusive crown: +2 hearts and Regeneration while worn and in water or rain (see
	// MermaidCrownItem). Diamond-tier defense values, since it's partly crafted from diamonds.
	// 1000 durability, same explicit-override convention every other weapon/armor piece uses.
	public static final Item MERMAID_CROWN = register("mermaid_crown",
			new MermaidCrownItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new Item.Properties().durability(1000)));

	// Talon Gauntlets: the upgrade path for Harpy's own bare-fist Talons power, a worn claw weapon
	// rather than a gripped one. Three tiers share HarpyTalonGauntletItem, same one-class-three-tiers
	// convention as FangItem. attackDamageModifier is chosen so each tier's own SwordItem-tier
	// ATTACK_DAMAGE math (modifier + tier bonus) lands on exactly 2/3/4 hearts (4.0/6.0/8.0) per the
	// source doc's "Iron Talon adds 2 hearts, Diamond 3, Netherite 4" spec, before the bare fist
	// bonus is folded in on top (see HarpyTalonGauntletItem's own class doc). Durability is an
	// explicit override (1000/2000/3500), matching the doc's own requested values, not each tier's
	// raw durability. Attack speed (-1.0) is faster than Fang's -1.8, since claws should feel
	// quicker than a dagger.
	public static final Item IRON_TALON_GAUNTLET = register("iron_talon_gauntlet",
			new HarpyTalonGauntletItem(Tiers.IRON, 2, -1.0F, 1, new Item.Properties().durability(1000)));
	public static final Item DIAMOND_TALON_GAUNTLET = register("diamond_talon_gauntlet",
			new HarpyTalonGauntletItem(Tiers.DIAMOND, 3, -1.0F, 2, new Item.Properties().durability(2000)));
	public static final Item NETHERITE_TALON_GAUNTLET = register("netherite_talon_gauntlet",
			new HarpyTalonGauntletItem(Tiers.NETHERITE, 4, -1.0F, 3, new Item.Properties().durability(3500)));

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, OriginModStudy.id(name), item);
	}

	public static void registerModItems() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(GOLDEN_SPIDER_EYE);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
			entries.accept(FANG);
			entries.accept(VENOMFANG);
			entries.accept(WIDOWFANG);
			entries.accept(PETRIFYING_TRIDENT);
			entries.accept(HARPY_JAVELIN);
			entries.accept(SILK_NET_SHOOTER);
			entries.accept(MERMAID_CROWN);
			entries.accept(IRON_TALON_GAUNTLET);
			entries.accept(DIAMOND_TALON_GAUNTLET);
			entries.accept(NETHERITE_TALON_GAUNTLET);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(SILK);
		});
	}
}
