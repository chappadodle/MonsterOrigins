package com.example.originmodstudy.mixin;

import com.example.originmodstudy.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Playtest fix, corrected: the first version of this mixin targeted {@code Mob
 * #isWithinMeleeAttackRange(LivingEntity)} — that method genuinely exists on 1.20.1's {@code Mob}
 * class, but decompiling {@code MeleeAttackGoal} directly shows it's never actually called here;
 * vanilla 1.20.1's real reach check is {@code MeleeAttackGoal#getAttackReachSqr(LivingEntity)} —
 * a {@code protected} method declared directly on the GOAL class itself, not on {@code Mob} —
 * compared against a separately precomputed distance in {@code tick()}/{@code
 * checkAndPerformAttack}. Minecraft only moved this onto {@code Mob} as {@code
 * isWithinMeleeAttackRange} in a later version (confirmed already working correctly on this
 * project's 1.21.1 port, which really does call that method). Targeting the wrong class's method
 * doesn't crash — Mixin dutifully finds and no-ops nothing since the injection site is just never
 * reached — it just silently never restricts anything, exactly matching the reported bug (identical
 * mixin logic worked on 1.21.1, did nothing at all on 1.20.1).
 *
 * <p>Real fix: {@code @Mixin(MeleeAttackGoal.class)}, injecting into the actual
 * {@code getAttackReachSqr} call every hostile mob's melee attack goes through. The goal's own
 * {@code protected final PathfinderMob mob} field (shadowed here) is the real attacker. Same two
 * restrictions as before: forces a guaranteed-fail return (`0.0`, always less than any real
 * distance-squared) unless the attacker and target's bounding boxes are actually touching AND the
 * target is in front of the attacker's frozen facing (matching {@code
 * ImmobilizedRotationLockMixin}'s own frozen-facing behavior) — otherwise forces a
 * guaranteed-pass return ({@code Double.MAX_VALUE}) so a valid point-blank frontal hit isn't
 * accidentally denied by whatever real reach value vanilla would have computed.
 */
@Mixin(MeleeAttackGoal.class)
public abstract class ImmobilizedAttackRangeMixin {

	@Shadow
	@Final
	protected PathfinderMob mob;

	@Inject(method = "getAttackReachSqr", at = @At("HEAD"), cancellable = true)
	private void immobilized$restrictAttackReach(LivingEntity target, CallbackInfoReturnable<Double> cir) {
		if (!this.mob.hasEffect(ModEffects.IMMOBILIZED)) {
			return;
		}

		boolean touching = this.mob.getBoundingBox().intersects(target.getBoundingBox());

		Vec3 look = this.mob.getLookAngle();
		Vec3 towardsTarget = target.position().subtract(this.mob.position());
		boolean inFront = towardsTarget.lengthSqr() <= 1.0E-6 || look.dot(towardsTarget.normalize()) >= 0.0;

		cir.setReturnValue(touching && inFront ? Double.MAX_VALUE : 0.0);
	}
}
