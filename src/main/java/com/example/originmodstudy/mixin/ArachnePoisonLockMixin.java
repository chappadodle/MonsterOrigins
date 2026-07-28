package com.example.originmodstudy.mixin;

import com.example.originmodstudy.util.ArachnePoisonLock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Task 6 correction, half heart lock: a target currently marked by {@link ArachnePoisonLock} (has
 * been hit by Arachne's own tiered Poison at least once) that drops to half a heart (1.0 HP) or
 * below gets Slowness and Blindness, continuously, until it either heals back above that threshold
 * or dies. There is no data-driven Origins/Apoli power for "apply an effect based on a health
 * threshold with an auto-removal condition tied to healing back above it," so this is real code,
 * the same kind of genuine data-driven gap {@code ArthropodPassiveTargetMixin} fills for mob AI
 * targeting.
 *
 * <p>{@code LivingEntity#tick()} (confirmed present via {@code javap} against this project's own
 * mapped Minecraft jar before writing this mixin) already runs for every living entity every
 * tick — this just adds one cheap {@code hasAttached} check to that existing per entity work,
 * rather than a separate {@code ServerTickEvents} callback scanning the whole entity list itself.
 * Nothing further happens unless that check passes, i.e. this specific entity has actually been
 * poisoned by Arachne's Fang line at some point.
 *
 * <p>The lock effects are deliberately reapplied with a short duration every tick they're needed,
 * rather than tracked with a separate "currently locked" flag plus an explicit
 * {@code removeEffect} call on the healed/died transition. A short lived, constantly refreshed
 * effect naturally expires within well under a second of the condition no longer holding, which is
 * simpler and safer than an explicit removal here: Arachne's own Silk Net Shooter also applies
 * Slowness, and an explicit {@code removeEffect(MobEffects.MOVEMENT_SLOWDOWN)} on every
 * heal-above-threshold tick could have clobbered that unrelated Slowness application if both
 * happened to be active on the same target at once. Vanilla Poison already caps its own damage so
 * it can never actually reduce a target below 1 HP (documented in this repo's own CLAUDE.md), so
 * in practice a poisoned target sits at exactly 1 HP indefinitely once it reaches that point,
 * making this lock a real "finish me off" window rather than a rare edge case.
 */
@Mixin(LivingEntity.class)
public abstract class ArachnePoisonLockMixin {
	/** Half a heart, per the design doc's own framing. */
	private static final float LOCK_HEALTH_THRESHOLD = 1.0F;

	// Reapplied every tick the lock is active, so effective duration reads as continuous; short
	// enough that it lapses within well under a second of the lock condition no longer holding.
	private static final int LOCK_EFFECT_DURATION_TICKS = 20;

	// Noticeably impairing without being a full stop (Slowness VI+ effectively halts movement
	// entirely) — Slowness II reads as "you're in real trouble" without removing player agency.
	private static final int LOCK_SLOWNESS_AMPLIFIER = 1;

	@Inject(method = "tick", at = @At("TAIL"))
	private void arachne$poisonHalfHeartLock(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			return;
		}
		if (!ArachnePoisonLock.isCandidate(self)) {
			return;
		}
		if (!self.isAlive() || self.getHealth() > LOCK_HEALTH_THRESHOLD) {
			return;
		}
		self.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LOCK_EFFECT_DURATION_TICKS,
				LOCK_SLOWNESS_AMPLIFIER));
		self.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LOCK_EFFECT_DURATION_TICKS, 0));
	}
}
