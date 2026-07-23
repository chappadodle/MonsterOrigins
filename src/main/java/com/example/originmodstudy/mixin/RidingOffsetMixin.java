package com.example.originmodstudy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Requirement (Latch On follow-up): the rider was appearing inside the ridden player's head,
 * blocking their view. Nothing in Origins/Apoli lets a data-driven power adjust ride/passenger
 * seat height — {@code origins:mount} just calls {@code startRiding}, and vanilla's own seat math
 * (a player's own passenger offset minus the passenger's {@code -0.35} "leg" constant) already
 * assumes the passenger is a normal-sized entity. Since Arachne's rider is scaled down to 50% via
 * Pehkui, the effective clearance shrinks with it.
 *
 * {@code getPassengersRidingOffset} is declared on {@link Entity} itself, not overridden by
 * {@link Player} — an earlier version of this mixin targeted {@code Player} directly and crashed
 * the whole game at launch (Mixin couldn't find the method there at all, which is a hard failure,
 * not a graceful skip). Mixing into {@link Entity} instead means this runs for every entity with
 * passengers (boats, horses, minecarts...), so the boost is explicitly gated to only apply when
 * the entity being ridden is a {@link Player} — only Arachne's own "Latch On" power ever causes
 * one player to ride another in the first place, so this stays scoped to that case in practice.
 */
@Mixin(Entity.class)
public abstract class RidingOffsetMixin {
	private static final double SEAT_HEIGHT_BOOST = 0.4;

	@Inject(method = "getPassengersRidingOffset", at = @At("RETURN"), cancellable = true)
	private void arachne$raiseSeatForClearance(CallbackInfoReturnable<Double> cir) {
		if ((Object) this instanceof Player) {
			cir.setReturnValue(cir.getReturnValue() + SEAT_HEIGHT_BOOST);
		}
	}
}
