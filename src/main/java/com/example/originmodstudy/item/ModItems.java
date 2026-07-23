package com.example.originmodstudy.item;

import com.example.originmodstudy.OriginModStudy;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

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

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, OriginModStudy.id(name), item);
	}

	public static void registerModItems() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
			entries.accept(GOLDEN_SPIDER_EYE);
		});
	}
}
