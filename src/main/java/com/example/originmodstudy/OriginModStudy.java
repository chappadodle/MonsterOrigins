package com.example.originmodstudy;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.entity.ModEntities;
import com.example.originmodstudy.item.ModItems;
import com.example.originmodstudy.power.ScreamConeAction;
import com.example.originmodstudy.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginModStudy implements ModInitializer {
	public static final String MOD_ID = "arachne";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Most of this mod's kit is entirely data-driven (data/arachne/origins, data/arachne/powers)
		// and registered automatically by Origins/Apoli's own data loaders. Real game content still
		// needs Java registration like vanilla would: items, the custom Bleed status effect (Harpy's
		// Talons), the bundled Scream sound event, and the one custom Apoli action type (Scream's
		// forward-cone knockback, which nothing data-driven can express).
		ModEntities.registerModEntities();
		ModItems.registerModItems();
		ModEffects.registerModEffects();
		ModSounds.registerModSounds();
		ScreamConeAction.register();

		LOGGER.info("Arachne origin loaded");
	}

	/** Helper to build a ResourceLocation namespaced to this mod, e.g. arachne:golden_spider_eye. */
	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
