package com.example.originmodstudy.util;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Marks a target as "has been hit by Arachne's own tiered Poison" (landed by
 * {@code FangItem}'s Fang/Venomfang/Widowfang, not the separate empty hand Venomous Bite power,
 * which stays out of scope for this marker) so {@code ArachnePoisonLockMixin} knows which
 * entities are worth checking, every tick, for the half heart Slowness/Blindness lock described
 * in the design doc. Uses the same Fabric Data Attachment API {@link TieredHitTracker} already
 * established the pattern for in this project (see that class's own doc for why this is the
 * Fabric native replacement for Forge's persistent-data NBT) — a plain boolean flag is all this
 * needs, so there is no NBT sub-structure to namespace the way {@link TieredHitTracker} does.
 *
 * <p>The flag is never cleared once set. The mixin's own per tick health check is what actually
 * decides whether the lock is active right now, so leaving a stale marker on a fully healed target
 * costs nothing beyond one cheap {@code hasAttached} lookup on its future ticks, no worse than the
 * cost of re-deciding "was this entity ever poisoned by us" some other way.
 */
public final class ArachnePoisonLock {
	private static final AttachmentType<Boolean> POISON_LOCK_CANDIDATE = AttachmentRegistry.createPersistent(
			new ResourceLocation("monster_origins", "arachne_poison_lock_candidate"),
			Codec.BOOL
	);

	private ArachnePoisonLock() {
	}

	/** Called by {@code FangItem} whenever it actually lands a tiered Poison application. */
	public static void markCandidate(LivingEntity target) {
		target.setAttached(POISON_LOCK_CANDIDATE, Boolean.TRUE);
	}

	/** Called by {@code ArachnePoisonLockMixin} every tick to decide whether this entity is worth
	 * a health check at all. */
	public static boolean isCandidate(LivingEntity target) {
		return target.hasAttached(POISON_LOCK_CANDIDATE);
	}
}
