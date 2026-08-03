package com.example.originmodstudy.mixin;

import com.example.originmodstudy.effect.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Playtest fix, corrected a second time: the previous version mixed into whichever real reach
 * check a mob's own {@code MeleeAttackGoal} (or subclass) used — but vanilla has a whole family of
 * these ({@code SpiderAttackGoal}, {@code VindicatorMeleeAttackGoal}, {@code
 * RavagerMeleeAttackGoal}, {@code DrownedAttackGoal}, ... — confirmed via the real 1.20.1 jar's own
 * class list), several of which override the reach calculation *themselves* rather than inheriting
 * the base goal's. A mixin on just the base class's method never fires for a subclass's own
 * override (plain Java virtual dispatch — the subclass's override replaces it entirely unless it
 * calls {@code super}), so the fix only ever worked for mobs using the unmodified base goal
 * (zombies/husks), not spiders, vindicators, and everything else with its own reach logic —
 * exactly the residual "still hits from behind" report, and reproducing this correctly for every
 * vanilla mob would mean chasing down and mixing into each one of these subclasses individually.
 *
 * <p>Far more robust: every one of these goals, however they each compute "am I in range," all
 * funnel into the exact same place to actually deal the hit — {@code Mob
 * #doHurtTarget(Entity)} (confirmed identical signature on both this project's 1.20.1 and 1.21.1
 * branches). Cancelling the hit itself at the one real choke point every melee mob shares, instead
 * of trying to intercept every different way a mob might decide it's in range, means this doesn't
 * need to know or care which {@code AttackGoal} subclass got the mob there. Same two real
 * restrictions as before, now applied uniformly to any mob regardless of its specific attack goal:
 * the hit is cancelled unless the attacker and target's bounding boxes are actually touching AND
 * the target is in front of the attacker's frozen facing (matching {@code
 * ImmobilizedRotationLockMixin}'s own frozen-facing behavior).
 */
@Mixin(Mob.class)
public abstract class ImmobilizedAttackRangeMixin {

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void immobilized$restrictAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
		Mob self = (Mob) (Object) this;
		if (!self.hasEffect(ModEffects.IMMOBILIZED) || !(target instanceof LivingEntity)) {
			return;
		}

		boolean touching = self.getBoundingBox().intersects(target.getBoundingBox());

		Vec3 look = self.getLookAngle();
		Vec3 towardsTarget = target.position().subtract(self.position());
		boolean inFront = towardsTarget.lengthSqr() <= 1.0E-6 || look.dot(towardsTarget.normalize()) >= 0.0;

		if (!touching || !inFront) {
			cir.setReturnValue(false);
		}
	}
}
