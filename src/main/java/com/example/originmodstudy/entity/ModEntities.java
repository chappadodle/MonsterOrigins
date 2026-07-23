package com.example.originmodstudy.entity;

import com.example.originmodstudy.OriginModStudy;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registers every entity type this mod adds. {@code THROWN_HARPY_JAVELIN}'s size/tracking-range/
 * update-rate mirror {@code EntityType.TRIDENT}'s own real registration parameters exactly (read
 * from its decompiled static initializer), since a thrown javelin behaves the same way.
 */
public class ModEntities {
	public static final EntityType<ThrownJavelin> THROWN_HARPY_JAVELIN = register("thrown_harpy_javelin",
			FabricEntityTypeBuilder.<ThrownJavelin>create(MobCategory.MISC, ThrownJavelin::new)
					.dimensions(EntityDimensions.scalable(0.5F, 0.5F))
					.trackRangeChunks(4)
					.trackedUpdateRate(20)
					.build());

	private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, OriginModStudy.id(name), type);
	}

	public static void registerModEntities() {
		// Registration happens via the static field initializer above; this method exists so
		// OriginModStudy can force this class to load at a predictable point during init, the
		// same convention ModItems/ModEffects/ModSounds use.
	}
}
