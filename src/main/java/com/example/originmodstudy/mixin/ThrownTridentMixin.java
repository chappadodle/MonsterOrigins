package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.HarpyJavelinItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
	@Shadow
	private ItemStack tridentItem;

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
}
