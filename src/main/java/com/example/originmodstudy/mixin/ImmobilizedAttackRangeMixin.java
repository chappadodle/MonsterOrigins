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
 * Playtest fix: a mob's own melee attack range ({@code MeleeAttackGoal} → {@code Mob
 * #isWithinMeleeAttackRange(LivingEntity)}) is a hardcoded distance-squared comparison against the
 * mob's own {@code getMeleeAttackRangeSqr}/{@code getPerceivedTargetDistanceSquareForMeleeAttack} —
 * entirely independent of any attribute this mod could otherwise grant/reduce (1.20.1 has no
 * per-mob "reach" attribute at all, unlike 1.21+). So a webbed/immobilized hostile mob's own
 * attacks were never affected by anything this mod did — matches the reported bug exactly: sneak
 * behind an immobilized mob, it still lands hits.
 *
 * <p>This mixin overrides {@code isWithinMeleeAttackRange} directly for an immobilized attacker,
 * with two real restrictions: (1) genuine zero reach — only two literally-overlapping bounding
 * boxes can register a hit, no extra range at all; and (2) no hit at all if the target is behind
 * the mob's frozen facing direction (dot product of the mob's look vector against the direction to
 * the target), so an immobilized mob genuinely can't turn and strike someone who snuck around
 * behind it — matching {@code ImmobilizedRotationLockMixin}'s own frozen-facing behavior, which
 * this composes with rather than duplicates.
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
