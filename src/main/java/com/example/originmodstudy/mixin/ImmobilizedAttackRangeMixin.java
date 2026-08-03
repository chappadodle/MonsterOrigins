package com.example.originmodstudy.mixin;

import com.example.originmodstudy.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Playtest fix: {@code ModEffects.IMMOBILIZED}'s {@code BLOCK_INTERACTION_RANGE}/
 * {@code ENTITY_INTERACTION_RANGE} attribute penalty (see {@code ModEffects.java}) only ever
 * affects a <em>player's own</em> reach when attacking — confirmed via decompile that a mob's own
 * melee attack range ({@code MeleeAttackGoal#canPerformAttack} → {@code Mob
 * #isWithinMeleeAttackRange(LivingEntity)} → {@code Mob#getAttackBoundingBox()}) never reads that
 * attribute at all; it's a flat bounding-box inflate by a hardcoded constant
 * ({@code DEFAULT_ATTACK_REACH}, real vanilla mob melee reach, independent of any attribute). So a
 * trapped hostile mob's own attacks were never actually affected by the earlier fix — exactly the
 * reported bug: sneak behind an immobilized mob and it still lands hits.
 *
 * <p>This mixin overrides {@code isWithinMeleeAttackRange} directly for an immobilized attacker,
 * with two real restrictions per the user's own request: (1) genuine zero reach — no bounding-box
 * inflation at all, only two literally-overlapping bounding boxes can register a hit (the real
 * {@code getHitbox()} vanilla itself compares against is {@code protected}, unreachable on an
 * arbitrary target instance from a mixin class; the public {@code getBoundingBox()} is a close
 * enough equivalent for this restriction, not a byte-for-byte replication); and
 * (2) no hit at all if the target is behind the mob's frozen facing direction (dot product of the
 * mob's look vector against the direction to the target), so an immobilized mob genuinely can't
 * turn and strike someone who snuck around behind it — matching
 * {@code ImmobilizedRotationLockMixin}'s own frozen-facing behavior, which this composes with
 * rather than duplicates.
 */
@Mixin(Mob.class)
public abstract class ImmobilizedAttackRangeMixin {

	@Inject(method = "isWithinMeleeAttackRange", at = @At("HEAD"), cancellable = true)
	private void immobilized$restrictAttackRange(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		Mob self = (Mob) (Object) this;
		if (!self.hasEffect(ModEffects.IMMOBILIZED)) {
			return;
		}

		if (!self.getBoundingBox().intersects(target.getBoundingBox())) {
			cir.setReturnValue(false);
			return;
		}

		Vec3 look = self.getLookAngle();
		Vec3 towardsTarget = target.position().subtract(self.position());
		if (towardsTarget.lengthSqr() > 1.0E-6 && look.dot(towardsTarget.normalize()) < 0.0) {
			cir.setReturnValue(false);
			return;
		}

		cir.setReturnValue(true);
	}
}
