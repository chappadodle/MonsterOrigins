package com.example.originmodstudy.mixin;

import com.example.originmodstudy.entity.ThrownJavelin;
import com.example.originmodstudy.item.HarpyJavelinItem;
import com.example.originmodstudy.item.MermaidTridentItem;
import com.example.originmodstudy.item.PetrifyingTridentItem;
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
 * <p><b>Storm Javelin (a separate, later addition):</b> now unconditional on any thrown hit,
 * subject only to its own per-player cooldown — no throw-time data capture needed anymore (an
 * earlier version keyed this off the thrower's Y level and fall distance, both since removed per
 * the user's own simplification). The lightning spawn technique (real {@link LightningBolt} entity
 * via {@code EntityType.LIGHTNING_BOLT.create(level)}, positioned with {@code Vec3
 * .atBottomCenterOf}, added via {@code level.addFreshEntity}) is lifted directly from vanilla's own
 * Channeling enchantment code path, decompiled straight out of {@code ThrownTrident.onHitEntity}
 * itself rather than assumed — the only intentional difference is this ability doesn't gate on
 * {@code isThundering()}/{@code canSeeSky()} the way real Channeling does.
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
	/**
	 * 1.21.1 update: {@code ThrownTrident}'s own private {@code tridentItem} field no longer
	 * exists (confirmed via {@code javap}) — the carried item moved up to {@code AbstractArrow}'s
	 * private {@code pickupItemStack}, exposed through the real public
	 * {@code ThrownTrident#getWeaponItem()} method (which itself just returns
	 * {@code AbstractArrow#getPickupItemStackOrigin()}). A {@code @Shadow} method stub for that
	 * real public method replaces the old {@code @Shadow} field — same "was not located in the
	 * target class" launch-time crash risk this project's CLAUDE.md already documents for any
	 * mixin whose shadowed member stops existing.
	 */
	@Shadow
	public abstract ItemStack getWeaponItem();

	/** UUID-keyed last-trigger game time (ticks), gating Storm Javelin's lightning + AOE damage
	 * behind a per-player cooldown, separate from the Bleed/airborne-bonus logic above. A plain
	 * map is sufficient here (per the plan brief) since this whole effect is Java-side, not a
	 * JSON power — there's no Apoli cooldown power to piggyback on. Reduced to a flat 600 ticks
	 * (30 seconds) this round, per the user's own simplification: no fall-distance/height
	 * requirement at all anymore, just "you threw the javelin and it hit something," balanced by
	 * this shorter but still real cooldown instead of a throw-time condition. */
	private static final Map<UUID, Long> STORM_JAVELIN_COOLDOWNS = new ConcurrentHashMap<>();
	private static final long STORM_JAVELIN_COOLDOWN_TICKS = 600L;
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
		if (!(this.getWeaponItem().getItem() instanceof HarpyJavelinItem)) {
			return amount;
		}
		Entity owner = harpyJavelin$owner();
		if (owner instanceof LivingEntity livingOwner && HarpyJavelinItem.isHarpyOrigin(livingOwner)) {
			return amount + HarpyJavelinItem.airborneBonusDamage(livingOwner);
		}
		return amount;
	}

	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void harpyJavelin$applyBleedOnThrow(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.getWeaponItem().getItem() instanceof HarpyJavelinItem)) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner)) {
			return;
		}
		if (entityHitResult.getEntity() instanceof LivingEntity target) {
			HarpyJavelinItem.applyBleed(target, livingOwner);
		}
	}

	/** Set at {@code onHitEntity}'s own HEAD (see {@code mermaidTrident$captureHitResult} below)
	 * so the Barbed Tip {@code @ModifyArg} handler can read the target's water state without
	 * declaring an invalid extra parameter — see that method's own doc for the crash this fixes. */
	private EntityHitResult mermaidTrident$currentHitResult;

	@Inject(method = "onHitEntity", at = @At("HEAD"))
	private void mermaidTrident$captureHitResult(EntityHitResult entityHitResult, CallbackInfo ci) {
		this.mermaidTrident$currentHitResult = entityHitResult;
	}

	/**
	 * Living Coral Trident's Barbed Tip (bonus damage vs. a swimming/floating target) for a
	 * <em>thrown</em> hit — same instruction Harpy's airborne bonus modifies above, chained via a
	 * second independent {@code @ModifyArg} handler on the identical injection point (Mixin
	 * applies multiple {@code @ModifyArg}s targeting the same call in definition order, each
	 * receiving the previous handler's result), rather than folded into Harpy's own method, so each
	 * origin's bonus stays a single-purpose handler gated on its own item type.
	 *
	 * <p><b>Crash fixed:</b> this originally declared {@code entityHitResult} as a second, appended
	 * parameter directly on the {@code @ModifyArg} handler, assuming that worked like an
	 * {@code @Inject} handler's enclosing-method parameter capture. It doesn't — {@code @ModifyArg}
	 * handler parameters are read as "which arguments of the invoked call to modify," so the extra
	 * parameter made Mixin expect {@code Entity.hurt} to have the signature
	 * {@code (float, EntityHitResult)}, which doesn't match its real one and crashed the whole
	 * mixin transform of {@code ThrownTrident} at game launch. Fixed by reading
	 * {@code mermaidTrident$currentHitResult} (stashed by the HEAD {@code @Inject} above) instead,
	 * keeping this handler at its original, valid single-parameter shape.
	 */
	@ModifyArg(
		method = "onHitEntity",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
		),
		index = 1
	)
	private float mermaidTrident$barbedTipBonusDamage(float amount) {
		if (!(this.getWeaponItem().getItem() instanceof MermaidTridentItem)) {
			return amount;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner) || !MermaidTridentItem.isMermaidOrigin(livingOwner)) {
			return amount;
		}
		EntityHitResult hitResult = this.mermaidTrident$currentHitResult;
		if (hitResult != null && hitResult.getEntity() instanceof LivingEntity target && target.isInWater()) {
			return amount + MermaidTridentItem.BARBED_TIP_BONUS_DAMAGE;
		}
		return amount;
	}

	/**
	 * Living Coral Trident's Symbiosis (hunger restore) and Bleeding Current (Bleed while the
	 * wielder is in water) for a thrown hit, plus the shared "Metal Hit" underwater sound — same
	 * TAIL injection shape as {@code harpyJavelin$applyBleedOnThrow} above, just gated on
	 * {@code MermaidTridentItem} instead.
	 */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void mermaidTrident$applyOnHitTraits(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.getWeaponItem().getItem() instanceof MermaidTridentItem)) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner) || !MermaidTridentItem.isMermaidOrigin(livingOwner)) {
			return;
		}
		if (entityHitResult.getEntity() instanceof LivingEntity target) {
			MermaidTridentItem.applySymbiosis(livingOwner);
			MermaidTridentItem.applyBleedingCurrent(target, livingOwner);
		}
	}

	/**
	 * Medusa's Petrifying Trident (Task 15): petrify moves to thrown-only, applied here since
	 * {@code onHitEntity} is the only place a thrown hit's damage/effects can be intercepted at
	 * all (see the class doc above). Melee damage from {@code PetrifyingTridentItem} is plain
	 * vanilla trident damage now — this mixin no longer needs a melee-side counterpart the way
	 * {@code hurtEnemy} used to provide before Task 15.
	 */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void medusaTrident$applyPetrifyOnThrow(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.getWeaponItem().getItem() instanceof PetrifyingTridentItem)) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner) || !PetrifyingTridentItem.isMedusaOrigin(livingOwner)) {
			return;
		}
		if (entityHitResult.getEntity() instanceof LivingEntity target) {
			PetrifyingTridentItem.applyPetrify(target);
		}
	}

	/**
	 * Storm Javelin: any thrown-hit lands a real lightning bolt at the impact point and deals AOE
	 * damage in a 10-block radius, subject to a 600-tick (30 second) per-player cooldown — no
	 * throw-time condition at all anymore (no fall distance, no height), per the user's own
	 * further simplification: "no requirement except the fact that you have to throw the trident
	 * to use it." Deliberately its own TAIL injection alongside the Bleed one above rather than
	 * folded into it, keeping the two abilities' cooldown/gating logic clearly separate even
	 * though this one no longer has a throw-time condition of its own.
	 */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void harpyJavelin$stormJavelinLightning(EntityHitResult entityHitResult, CallbackInfo ci) {
		if (!(this.getWeaponItem().getItem() instanceof HarpyJavelinItem)) {
			return;
		}
		if (!(harpyJavelin$owner() instanceof LivingEntity livingOwner)) {
			return;
		}

		Entity javelinEntity = (Entity) (Object) this;
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
