package com.example.originmodstudy.sound;

import com.example.originmodstudy.OriginModStudy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers every sound event this mod adds. The actual audio file and its resource-pack-side
 * name mapping live in {@code assets/arachne/sounds.json} and
 * {@code assets/arachne/sounds/harpy_scream.ogg} — see CREDITS.md for that file's license.
 */
public class ModSounds {

	public static final SoundEvent HARPY_SCREAM = register("harpy_scream");

	private static SoundEvent register(String name) {
		ResourceLocation id = OriginModStudy.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void registerModSounds() {
		// Registration happens via the static field initializer above; see ModEffects for why
		// this method exists.
	}
}
