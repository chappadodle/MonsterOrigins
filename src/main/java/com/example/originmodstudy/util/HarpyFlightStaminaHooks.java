package com.example.originmodstudy.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Balances Harpy's permanent flight (Wings) with a stamina cost: the first 60 seconds of any
 * continuous flight are free, matching how flight already felt before this change, but every
 * second past that drains saturation until she lands. Landing (or simply not flying, checked via
 * {@code isFallFlying()}, the same real signal {@code HarpyFlightSpeedMixin} already uses to
 * detect Harpy flight) resets the clock back to zero.
 *
 * <p>{@code Player.getFoodData()}/{@code FoodData.setSaturation(float)} are plain public vanilla
 * methods (confirmed via {@code javap}), so this needs no mixin, same periodic
 * {@code ServerTickEvents.END_SERVER_TICK} scan pattern already used by {@code MedusaFearHooks}.
 * Deliberately only touches saturation, never food level directly, matching what was asked for;
 * once saturation hits zero, sustained flight stops draining anything further here (vanilla's own
 * exhaustion system still eats into food level from ordinary movement/actions as normal).
 */
public final class HarpyFlightStaminaHooks {
	private static final ResourceLocation HARPY_ORIGIN_ID = new ResourceLocation("monster_origins", "harpy");
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final int FREE_FLIGHT_SECONDS = 60;
	private static final float SATURATION_DRAIN_PER_SECOND = 0.25F;

	private static final Map<UUID, Integer> continuousFlightSeconds = new HashMap<>();

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
				if (!OriginUtil.hasOrigin(player, HARPY_ORIGIN_ID) || !player.isFallFlying()) {
					continuousFlightSeconds.remove(id);
					continue;
				}
				int seconds = continuousFlightSeconds.merge(id, 1, Integer::sum);
				if (seconds > FREE_FLIGHT_SECONDS) {
					FoodData foodData = player.getFoodData();
					foodData.setSaturation(Math.max(0.0F, foodData.getSaturationLevel() - SATURATION_DRAIN_PER_SECOND));
				}
			}
		}
	}
}
