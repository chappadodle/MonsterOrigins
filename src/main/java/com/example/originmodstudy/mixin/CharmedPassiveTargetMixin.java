package com.example.originmodstudy.mixin;

import com.example.originmodstudy.effect.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Siren's Call requirement: hostiles caught in the song stop attacking entirely, not just
 * whoever cast it — {@link ModEffects#CHARMED} is applied to every hostile in range (see
 * {@code sirens_call.json}), and this mixin suppresses target acquisition for any mob carrying
 * it, against any potential target, for as long as the effect lasts. Same
 * {@code TargetGoal#canAttack} choke point as {@link ArthropodPassiveTargetMixin} and
 * {@link SeaCreaturePassiveTargetMixin}, but keyed off a status effect instead of a static tag —
 * no retaliation exception, since a charmed mob is meant to stop fighting outright while it lasts.
 */
@Mixin(TargetGoal.class)
public abstract class CharmedPassiveTargetMixin {
	@Shadow
	@Final
	protected Mob mob;

	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void siren$suppressCharmedTargeting(
		LivingEntity potentialTarget, TargetingConditions targetConditions, CallbackInfoReturnable<Boolean> cir
	) {
		if (this.mob.hasEffect(ModEffects.CHARMED)) {
			cir.setReturnValue(false);
		}
	}
}
