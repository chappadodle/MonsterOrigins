package com.example.originmodstudy.effect;

import com.example.originmodstudy.OriginModStudy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Registers every status effect this mod adds. Fields are {@code static final} so they are
 * created (and registered) the moment this class is first referenced.
 */
public class ModEffects {

	// Harpy's Talons power (see data/arachne/powers/harpy/talons.json) applies this on hit.
	// Color is a dried-blood red, distinct from vanilla Poison's green.
	public static final MobEffect BLEED = register("bleed",
			new BleedMobEffect(MobEffectCategory.HARMFUL, 0x8B1A1A));

	private static MobEffect register(String name, MobEffect effect) {
		return Registry.register(BuiltInRegistries.MOB_EFFECT, OriginModStudy.id(name), effect);
	}

	public static void registerModEffects() {
		// Registration happens via the static field initializers above; this method exists so
		// OriginModStudy can force this class to load at a predictable point during init, the
		// same convention ModItems/ModSounds use.
	}
}
