package com.example.originmodstudy.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Iron golems see through Medusa entirely and villagers are scared of her — unconditional now
 * that Innocent Form (a disguise toggle these hooks were originally gated behind, since removed
 * per the user's own call — "quite useless and was a bad idea") no longer exists. Both driven
 * from a plain periodic server-tick scan (Fabric API's {@code ServerTickEvents.END_SERVER_TICK})
 * over players with the Medusa origin, rather than a per-mob AI-goal mixin — iron golems' own
 * built-in targeting goals only ever consider {@code Mob} targets (confirmed by decompiling
 * {@code IronGolem.registerGoals()} directly: both its {@code NearestAttackableTargetGoal}s are
 * typed to {@code Mob.class}), and {@code Player} isn't a {@code Mob} subclass at all, so golems
 * have no existing goal this mod could gate/redirect in the first place — the real "golems attack
 * hostile players" behavior in vanilla is driven by the separate village-reputation/anger system,
 * not a target goal. Directly calling {@code Mob#setTarget} on a nearby golem is the simpler,
 * equally valid way to make one actually attack a specific player.
 *
 * <p>Villagers get a lighter touch: a scared knockback + vanilla's own "I don't want to do that"
 * sound ({@code SoundEvents.VILLAGER_NO}) whenever Medusa gets close, on top of {@code
 * VillagerNoTradeMixin} refusing the trade screen outright. A full Brain/memory-based fear
 * reaction (making a villager path away and stay away) would need reverse-engineering 1.20.1's
 * villager Brain/Sensor system — real complexity disproportionate to a "study" project's own
 * established scope precedent (see {@code util/OriginUtil.java}'s own reasoning for a similar
 * proportionate-scope call) — so this settles for an immediate, repeatable "flinch away" instead
 * of persistent flee behavior.
 */
public final class MedusaFearHooks {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "medusa");
	private static final int SCAN_INTERVAL_TICKS = 20;
	private static final double GOLEM_AGGRO_RADIUS = 16.0;
	private static final double VILLAGER_FEAR_RADIUS = 8.0;
	private static final double VILLAGER_KNOCKBACK_STRENGTH = 0.6;

	private MedusaFearHooks() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(MedusaFearHooks::onServerTick);
	}

	private static void onServerTick(MinecraftServer server) {
		if (server.getTickCount() % SCAN_INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				if (!OriginUtil.hasOrigin(player, MEDUSA_ORIGIN_ID)) {
					continue;
				}
				aggroNearbyGolems(level, player);
				scareNearbyVillagers(level, player);
			}
		}
	}

	private static void aggroNearbyGolems(ServerLevel level, ServerPlayer player) {
		AABB area = player.getBoundingBox().inflate(GOLEM_AGGRO_RADIUS);
		List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class, area, IronGolem::isAlive);
		for (IronGolem golem : golems) {
			if (golem.getTarget() == null) {
				golem.setTarget(player);
			}
		}
	}

	private static void scareNearbyVillagers(ServerLevel level, ServerPlayer player) {
		AABB area = player.getBoundingBox().inflate(VILLAGER_FEAR_RADIUS);
		List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area, Villager::isAlive);
		for (Villager villager : villagers) {
			Vec3 away = villager.position().subtract(player.position());
			double distance = away.length();
			if (distance < 1.0e-4) {
				continue;
			}
			Vec3 push = away.scale(VILLAGER_KNOCKBACK_STRENGTH / distance);
			villager.setDeltaMovement(villager.getDeltaMovement().add(push.x, 0.1, push.z));
			villager.hurtMarked = true;
			level.playSound(null, villager, SoundEvents.VILLAGER_NO, villager.getSoundSource(), 1.0F, 1.0F);
		}
	}
}
