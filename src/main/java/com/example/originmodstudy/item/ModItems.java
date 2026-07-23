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

	// Same purpose as ARACHNE_EYE above, for the Siren origin's icon.
	public static final Item SIREN_EYE = register("siren_eye", new Item(new Item.Properties()));

	// A light, fast dagger — built on iron's tier but with a lower damage modifier (2, vs. the
	// vanilla iron sword's 3) and a faster attack speed modifier (-1.8, vs. the vanilla sword
	// default -2.4), trading raw damage for swing speed to feel like a dagger rather than a sword.
	// First of three tiers — see FangItem's class doc for the full progression.
	public static final Item FANG = register("fang",
			new FangItem(Tiers.IRON, 2, -1.8F, false, false, new Item.Properties()));

	// Second tier: diamond-tier (modifier 3 + diamond's 3.0 bonus = 6 attack damage, same -1.8
	// speed as Fang), adds Bleed on top of Poison. Upgraded from Fang on a crafting table.
	public static final Item VENOMFANG = register("venomfang",
			new FangItem(Tiers.DIAMOND, 3, -1.8F, true, false, new Item.Properties()));

	// Third and final tier: netherite-tier (modifier 3 + netherite's 4.0 bonus = 7 attack damage,
	// same speed again), adds Wither on top of Poison and Bleed. Upgraded from Venomfang on a
	// smithing table (smithing_transform), matching vanilla's own Diamond->Netherite convention.
	public static final Item WIDOWFANG = register("widowfang",
			new FangItem(Tiers.NETHERITE, 3, -1.8F, true, true, new Item.Properties()));

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

	// Siren's exclusive crown: +2 hearts and continuous Regeneration while worn (see
	// SirenCrownItem). Diamond-tier defense values, since it's partly crafted from diamonds.
	public static final Item SIREN_CROWN = register("siren_crown",
			new SirenCrownItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new Item.Properties()));

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
			entries.accept(SIREN_CROWN);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
			entries.accept(SILK);
		});
	}
}
