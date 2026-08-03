package com.example.originmodstudy.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Balances Harpy's permanent flight (Wings) with a small, continuous stamina cost tied to actively
 * gaining speed while flying. Passive gliding/coasting at a steady speed never costs anything, no
 * matter how long it lasts.
 *
 * <p><b>Does not use {@code LivingEntity.zza}/{@code xxa} to detect "holding forward," unlike an
 * earlier version of this file (and unlike {@code HarpyFlightSpeedMixin}'s own boost logic).</b>
 * A real playtest proved that signal is permanently stuck at {@code 0.0f} here no matter what keys
 * are held — confirmed by decompiling {@code ServerPlayer.setPlayerInput}, the only place vanilla
 * ever writes to those fields from a real client's input packet:
 * <pre>
 * public void setPlayerInput(float f, float g, boolean bl, boolean bl2) {
 *     if (this.isPassenger()) { // &lt;-- only true while riding a vehicle (boat/horse/etc.)
 *         this.xxa = f;
 *         this.zza = g;
 *         ...
 *     }
 * }
 * </pre>
 * Vanilla only ever syncs raw WASD state from a real player to the server while that player is a
 * *passenger* (steering a boat/horse/pig) — for an ordinary player just walking or flying, the
 * server has no need to know raw key state at all, since the client reports resulting positions
 * directly. {@code zza}/{@code xxa} are simply never written to for that case; they sit at their
 * default 0 forever, which is exactly what the diagnostic log from a real Harpy flight session
 * showed ({@code holdingForward=false} every single sampled second despite the key genuinely being
 * held down the whole time) — this is a hard read-time limitation, not a threshold/tuning problem
 * three earlier attempts at this file mistakenly kept trying to fix by retuning around it. (Whether
 * {@code HarpyFlightSpeedMixin}'s own W-based speed boost has ever actually worked is a real open
 * question raised by this same finding, since it reads the identical always-zero field — untouched
 * here, since that is a different feature than the one being fixed.)
 *
 * <p>Sidesteps needing real key state entirely: this instead measures actual horizontal flight
 * speed once a second via {@code LivingEntity.getDeltaMovement()} (the same public vanilla method
 * {@code HarpyFlightSpeedMixin} already reads), and only charges a cost when this second's speed is
 * higher than last second's — i.e., she is verifiably, physically gaining speed right now,
 * regardless of what's causing it. This is a strictly server-side, per-tick-authoritative signal,
 * needs no client packet of any kind, and lines up with what was actually asked for ("you lose
 * [stamina] while actively going faster") arguably more directly than a raw keypress would.
 *
 * <p>Routed through {@code Player.causeFoodExhaustion(float)} (confirmed public via {@code javap}),
 * not a direct {@code FoodData.setSaturation} write like the original version of this file used.
 * Decompiling {@code FoodData.tick()} directly showed why that original version never had any
 * visible effect: vanilla only ever decrements the visible hunger bar ({@code foodLevel}) once
 * {@code saturationLevel} is already at its floor of 0, and directly forcing saturation toward that
 * same floor every tick is a no-op in that already-common state — saturation sits near 0 for most
 * normal play anyway. Feeding exhaustion into vanilla's own real pipeline instead means this drains
 * saturation first (while any remains) and then starts actually depleting the hunger bar once it's
 * gone, exactly the vanilla sprinting-when-hungry feel this was meant to have.
 */
public final class HarpyFlightStaminaHooks {
	private static final ResourceLocation HARPY_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "harpy");
	private static final int SCAN_INTERVAL_TICKS = 20;
	// A tiny margin above zero, not an exact ">", so ordinary floating-point noise between two
	// otherwise-equal samples (coasting at a constant speed) doesn't register as "gaining speed."
	private static final double SPEED_INCREASE_THRESHOLD = 0.01;
	// Vanilla's FoodData.tick() only spends a point of saturation/food once accumulated exhaustion
	// crosses 4.0 (confirmed via decompile), so 0.5/second costs a point roughly every 8 seconds of
	// sustained acceleration — real evidence (a logged playtest) showed speed-gaining ticks are
	// intermittent, not every single second, so full saturation (5 points, no HUD bar of its own)
	// drains in well under a minute of real flying, and the visible hunger bar starts moving soon
	// after — a rate low enough to read as "gaining speed costs stamina," not a hard punishment, but
	// fast enough to actually be seen within one normal test/play session.
	private static final float EXHAUSTION_PER_SECOND = 0.5F;

	private static final Map<UUID, Double> lastHorizontalSpeed = new HashMap<>();

	private HarpyFlightStaminaHooks() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HarpyFlightStaminaHooks::onServerTick);
	}

	private static void onServerTick(MinecraftServer server) {
		if (server.getTickCount() % SCAN_INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				UUID id = player.getUUID();
				if (!player.isFallFlying() || !OriginUtil.hasOrigin(player, HARPY_ORIGIN_ID)) {
					lastHorizontalSpeed.remove(id);
					continue;
				}
				Vec3 velocity = player.getDeltaMovement();
				double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
				double previousSpeed = lastHorizontalSpeed.getOrDefault(id, speed);
				boolean gainingSpeed = speed - previousSpeed > SPEED_INCREASE_THRESHOLD;
				lastHorizontalSpeed.put(id, speed);
				if (gainingSpeed) {
					player.causeFoodExhaustion(EXHAUSTION_PER_SECOND);
				}
			}
		}
	}
}
