package com.example.originmodstudy.mixin;

import com.example.originmodstudy.entity.ThrownJavelin;
import com.example.originmodstudy.item.HarpyJavelinItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code ThrownTrident.onHitEntity} (decompiled and read directly, not assumed from docs) deals
 * its own damage via {@code entity.hurt(damageSource, f)} directly — completely bypassing
 * {@code ItemStack.hurtEnemy}, which only fires for melee swings. This mixin is what makes a
 * thrown hit apply the same Harpy-origin-gated rules {@link HarpyJavelinItem#hurtEnemy} applies
 * on a melee hit. It targets {@code ThrownTrident} itself (not the dedicated {@code ThrownJavelin}
 * subclass) so the transformed method is inherited automatically — {@code ThrownJavelin} doesn't
 * override {@code onHitEntity} at all, it only exists for its entity type and renderer.
 *
 * <p>The airborne bonus checks {@link LivingEntity#isFallFlying()} at the moment of impact rather
 * than capturing whether the thrower was airborne at the moment of the throw — simpler, and for a
 * javelin's short flight time the two are practically the same "dove and threw it" case the user
 * asked for.
 *
 * <p><b>Storm Javelin (a separate, later addition):</b> unlike the airborne bonus above, this one
 * deliberately captures data at throw time, not impact time — {@link HarpyJavelinItem#releaseUsing}
 * stashes the thrower's Y level and {@code fallDistance} on the spawned {@link ThrownJavelin} the
 * moment it's thrown, and {@code harpyJavelin$stormJavelinLightning} below reads that back on
 * impact. These are two genuinely different moments and mechanics and are kept in separate fields/
 * methods rather than merged with the airborne-bonus check. The lightning spawn technique
 * (real {@link LightningBolt} entity via {@code EntityType.LIGHTNING_BOLT.create(level)},
 * positioned with {@code Vec3.atBottomCenterOf}, added via {@code level.addFreshEntity}) is lifted
 * directly from vanilla's own Channeling enchantment code path, decompiled straight out of
 * {@code ThrownTrident.onHitEntity} itself rather than assumed — the only intentional difference is
 * this ability doesn't gate on {@code isThundering()}/{@code canSeeSky()} the way real Channeling
 * does, since the throw-height/fall-distance condition is the whole point of this ability, not the
 * weather.
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
	@Shadow
	private ItemStack tridentItem;

	/** UUID-keyed last-trigger game time (ticks), gating Storm Javelin's lightning + AOE damage
	 * behind a 1200-tick (1 minute) per-player cooldown, separate from the Bleed/airborne-bonus
	 * logic above. A plain map is sufficient here (per the plan brief) since this whole effect is
	 * Java-side, not a JSON power — there's no Apoli cooldown power to piggyback on. */
	private static final Map<UUID, Long> STORM_JAVELIN_COOLDOWNS = new ConcurrentHashMap<>();
	private static final long STORM_JAVELIN_COOLDOWN_TICKS = 1200L;
	private static final double STORM_JAVELIN_THROW_HEIGHT_THRESHOLD = 120.0;
	private static final float STORM_JAVELIN_FALL_DISTANCE_THRESHOLD = 30.0F;
	private static final double STORM_JAVELIN_AOE_RADIUS = 10.0;
	private static final float STORM_JAVELIN_AOE_DAMAGE = 6.0F;

	/** {@code getOwner()} is public and declared on {@link Projectile}, not on ThrownTrident
	 * itself, so it's reached via a plain cast to that real, already-compiled superclass rather
	 * than an {@code @Shadow} method stub. */
	private Entity harpyJavelin$owner() {
		return ((Projectile) (Object) this).getOwner();
	}

	@ModifyArg(
		method = "onHitEntity",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
		),
		index = 1
	)
	private float harpyJavelin$airborneBonusDamage(float amount) {
		if (!(this.tridentItem.getItem() instanceof HarpyJavelinItem)) {
			return amount;
		}
		Entity owner = harpyJavelin$owner();
		if (owner instanceof LivingEntity livingOwner
			&& livingOwner.isFallFlying()
			&& HarpyJavelinItem.isHarpyOrigin(livingOwner)) {
			return amount + 3.0F;
		}
		return amount;
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void harpyJavelin$applyBleedOnThrow(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.tridentItem.getItem() instanceof HarpyJavelinItem)) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner)) {
			return;
		}
		if (entityHitResult.getEntity() instanceof LivingEntity target) {
			HarpyJavelinItem.applyBleed(target, livingOwner);
		}
	}

	/**
	 * Storm Javelin: if this thrown Harpy Javelin was released from above Y=120 or after a 30+
	 * block fall (captured at throw time, see the class doc), call down a real lightning bolt at
	 * the impact point and deal AOE damage in a 10-block radius, subject to a 1200-tick
	 * (1 minute) per-player cooldown. Deliberately its own TAIL injection alongside the Bleed one
	 * above rather than folded into it, since it has an entirely separate trigger condition
	 * (throw-time height/fall, not "did this hit land at all").
	 */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void harpyJavelin$stormJavelinLightning(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.tridentItem.getItem() instanceof HarpyJavelinItem)) {
			return;
		}
		Object self = this;
		if (!(self instanceof ThrownJavelin thrownJavelin)) {
			return;
		}
		boolean highThrow = thrownJavelin.stormThrowY > STORM_JAVELIN_THROW_HEIGHT_THRESHOLD;
		boolean bigFall = thrownJavelin.stormFallDistance >= STORM_JAVELIN_FALL_DISTANCE_THRESHOLD;
		if (!highThrow && !bigFall) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner)) {
			return;
		}

		Entity javelinEntity = (Entity) self;
		Level level = javelinEntity.level();
		if (!(level instanceof ServerLevel)) {
			return;
		}

		long now = level.getGameTime();
		UUID ownerId = livingOwner.getUUID();
		Long lastUse = STORM_JAVELIN_COOLDOWNS.get(ownerId);
		if (lastUse != null && now - lastUse < STORM_JAVELIN_COOLDOWN_TICKS) {
			return;
		}
		STORM_JAVELIN_COOLDOWNS.put(ownerId, now);

		BlockPos impactPos = entityHitResult.getEntity().blockPosition();

		LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(level);
		if (lightningBolt != null) {
			lightningBolt.moveTo(Vec3.atBottomCenterOf(impactPos));
			if (livingOwner instanceof ServerPlayer serverPlayer) {
				lightningBolt.setCause(serverPlayer);
			}
			level.addFreshEntity(lightningBolt);
		}

		Vec3 impactCenter = Vec3.atCenterOf(impactPos);
		DamageSource stormDamage = javelinEntity.damageSources().lightningBolt();
		AABB aoeBounds = new AABB(impactPos).inflate(STORM_JAVELIN_AOE_RADIUS);
		List<LivingEntity> aoeTargets = level.getEntitiesOfClass(LivingEntity.class, aoeBounds,
				candidate -> candidate != livingOwner
						&& candidate.distanceToSqr(impactCenter) <= STORM_JAVELIN_AOE_RADIUS * STORM_JAVELIN_AOE_RADIUS);
		for (LivingEntity aoeTarget : aoeTargets) {
			aoeTarget.hurt(stormDamage, STORM_JAVELIN_AOE_DAMAGE);
		}
	}
}
