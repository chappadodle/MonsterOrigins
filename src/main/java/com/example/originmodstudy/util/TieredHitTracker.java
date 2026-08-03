package com.example.originmodstudy.util;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared "repeat hit count by attacker+target, with a reset window" helper backing both Harpy's
 * Talon Gauntlet bleed tiers (Task 9) and Arachne's poison tiers (Task 6) — no consumer wires this
 * up yet, both of those land in later tasks.
 *
 * <p>State is stored as NBT — {@code {tier: int, lastHitGameTime: long}} per attacker — attached
 * to the <em>target</em>. Note for future readers: vanilla/Fabric has no {@code Entity#getPersistentData()}
 * (that's a Forge-only method; confirmed absent here via {@code javap} against this project's own
 * mapped Minecraft jar, {@code .gradle/loom-cache/.../minecraft-merged-*.jar}, before writing this
 * class). The Fabric-native equivalent used instead is Fabric API's Data Attachment API
 * ({@code net.fabricmc.fabric.api.attachment.v1}) — confirmed present on {@code Entity} in the
 * exact fabric-api version this project already depends on (0.92.11+1.20.1, jar-in-jar bundled
 * submodule {@code fabric-data-attachment-api-v1}) by the same javap check, and confirmed visible
 * to javac with zero build.gradle changes: unlike third-party jar-in-jar bundles (Cardinal
 * Components, Calio, Apoli — see this project's build.gradle comments for why those each need
 * their own explicit {@code modCompileOnly} line), Fabric Loom auto-exposes fabric-api's own
 * bundled submodules to the compile classpath whenever the umbrella {@code fabric-api} artifact is
 * on {@code modImplementation}, which this project's build.gradle already has.
 *
 * <p>{@link AttachmentType#createPersistent} is used (not the plain, non-persistent
 * {@code create}) so tier state actually survives a save/reload — matching "NBT-backed" from the
 * brief in more than name only, unlike a plain in-memory map.
 */
public final class TieredHitTracker {

	/** One attachment holds every tracker's data for a given target, keyed by
	 * {@code trackerKey:attackerUuid} sub-tags, so Arachne's poison tracker and Harpy's bleed
	 * tracker (or any future one) never collide even when attached to the same target entity. */
	private static final AttachmentType<CompoundTag> HIT_TRACKER_DATA = AttachmentRegistry.createPersistent(
			ResourceLocation.fromNamespaceAndPath("monster_origins", "tiered_hit_tracker"),
			CompoundTag.CODEC
	);

	private static final String TIER_KEY = "tier";
	private static final String LAST_HIT_GAME_TIME_KEY = "lastHitGameTime";

	private TieredHitTracker() {
	}

	/**
	 * Returns the tier to apply for this hit by {@code attacker} on {@code target}, under the
	 * given {@code trackerKey}'s own independent tier sequence.
	 *
	 * <p>Starts at 1 on an attacker's first qualifying hit on a target (or once the reset window
	 * has fully elapsed since their last one), increments by 1 per qualifying repeat hit landing
	 * within {@code resetTicks} of the previous hit, and caps at {@code maxTier}.
	 *
	 * @param target     the entity being hit — the tracker's NBT state is attached here
	 * @param attacker   the entity landing the hit — tiers are tracked independently per attacker
	 * @param trackerKey namespaces this call's tier sequence from any other tracker sharing the
	 *                   same target (e.g. {@code "arachne_poison"} vs {@code "harpy_bleed"})
	 * @param maxTier    the highest tier this call can ever return (expected >= 1)
	 * @param resetTicks how many ticks may pass with no qualifying hit on this target by this
	 *                   attacker, under this trackerKey, before the sequence resets to tier 1 — the
	 *                   caller supplies this (a generous 5-10s window is the intended use), it is
	 *                   never hardcoded in this class
	 * @return the tier (1..maxTier) to apply for this hit
	 */
	public static int nextTier(LivingEntity target, LivingEntity attacker, String trackerKey, int maxTier, int resetTicks) {
		CompoundTag root = target.getAttachedOrCreate(HIT_TRACKER_DATA, CompoundTag::new);
		String entryKey = trackerKey + ":" + attacker.getUUID();
		long now = target.level().getGameTime();

		int tier = 1;
		if (root.contains(entryKey)) {
			CompoundTag previous = root.getCompound(entryKey);
			long lastHitGameTime = previous.getLong(LAST_HIT_GAME_TIME_KEY);
			if (now - lastHitGameTime <= resetTicks) {
				tier = Math.min(previous.getInt(TIER_KEY) + 1, maxTier);
			}
		}

		CompoundTag entry = new CompoundTag();
		entry.putInt(TIER_KEY, tier);
		entry.putLong(LAST_HIT_GAME_TIME_KEY, now);
		root.put(entryKey, entry);
		// Entities (unlike block entities/chunks) don't need an explicit dirty-flag notification to
		// have their attachment saved, but setAttached is called anyway for clarity and because it's
		// the documented, always-correct way to commit a mutated attachment value.
		target.setAttached(HIT_TRACKER_DATA, root);

		return tier;
	}
}
