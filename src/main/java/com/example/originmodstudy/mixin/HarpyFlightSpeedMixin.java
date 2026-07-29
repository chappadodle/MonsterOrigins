package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.HarpyJavelinItem;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Harpy's flight-speed boost — see git history/CLAUDE.md for the full run of tuning passes this
 * has been through. This round: raised the cap to a clean "300% of a baseline elytra cruising
 * speed" (baseline assumed ~1.0 blocks/tick, a reasonable round-number approximation for level
 * fall-flying — there's no single stored vanilla constant for this, it emerges from the physics),
 * and replaced the flat per-tick acceleration with an exponential approach to the cap.
 *
 * <p><b>Why exponential, not linear, this time:</b> the user explicitly asked for the W-key
 * buildup to behave "similar" to the S-key braking they already liked — a lot of change quickly
 * at first, tapering off as the limit approaches, not a constant drip. The braking rule
 * ({@code speed *= BACKWARD_DECELERATION}) already has exactly that shape for free: multiplying a
 * large number by a fixed fraction removes a large *absolute* amount, multiplying a small number
 * removes a small absolute amount, so it naturally decays fast at first and gently near zero.
 * Mirroring that shape for acceleration means shrinking the *gap* to the cap by a fixed fraction
 * each tick (rather than adding a fixed amount): {@code speed += (cap - speed) * rate}. Far from
 * the cap, the gap is large, so the tick-to-tick gain is large ("a lot of momentum quickly");
 * close to the cap, the gap is small, so gains taper to nearly nothing (the same "slowly grows to
 * its limit" feel the old flat-addition model was reaching for, but sharing the exact same
 * mathematical shape as the braking curve instead of a visibly different one).
 */
@Mixin(LivingEntity.class)
public abstract class HarpyFlightSpeedMixin {
	private static final ResourceLocation HARPY_ORIGIN_ID = new ResourceLocation("monster_origins", "harpy");
	/** Lives on {@link HarpyJavelinItem}, not here: Mixin rejects a non-private static field
	 * declared directly inside a mixin class (it would need to attach a public static field onto
	 * {@code LivingEntity} itself, which Sponge Mixin's pre-processor disallows outright — this
	 * crashed the whole mixin apply pass at launch, caught by the user's own real playtest, not by
	 * this environment). Kept as a plain constant on a regular class instead, referenced from both
	 * here and {@code HarpyJavelinSpeedDamageMixin}/{@code ThrownTridentMixin}. Raised again this
	 * round to 3.0 — "300% base elytra speed," per the user's own explicit target. */
	private static final double MAX_BOOSTED_HORIZONTAL_SPEED = HarpyJavelinItem.MAX_BOOSTED_HORIZONTAL_SPEED;

	// Fraction of the remaining gap to the cap closed every tick while holding forward (W) — see
	// the class doc above for why this is a fraction-of-the-gap rule instead of a flat addition.
	// 0.05 mirrors BACKWARD_DECELERATION's own magnitude below (both close ~5% of their respective
	// gap per tick), giving W and S a matching, symmetric feel as requested — noticeably fast to
	// start, tapering smoothly rather than crawling the whole way.
	private static final double ACCELERATION_RATE = 0.05;

	// Braking: holding backward (S) actively bleeds speed off, faster than the passive decay below
	// — the user's own diagnosis for why flight needed to be slower in the first place: real
	// vanilla elytra physics have no drag mechanism strong enough to ever slow back down once this
	// mod's own boost has built up speed. Explicitly the model ACCELERATION_RATE above was written
	// to mirror.
	private static final double BACKWARD_DECELERATION = 0.95;

	// Passive decay: holding neither key no longer holds speed constant — it drifts back down on
	// its own, gently. Corrected an earlier version where *any* non-backward input (including no
	// input at all) accelerated toward the cap, which the user flagged directly: "the standard
	// should be that when you fly you lose momentum slowly unless you press w." Deliberately much
	// gentler than BACKWARD_DECELERATION — coasting shouldn't feel like braking.
	private static final double PASSIVE_DECELERATION = 0.998;

	// LivingEntity's own real forward-movement input impulse (positive = holding forward, negative
	// = holding backward/S, zero = neither) — a plain public vanilla field, not something this mod
	// added. Already synced server-side for a real player every tick (the server needs it to run
	// its own authoritative movement simulation), so reading it here needs no custom networking.
	@Shadow
	public float zza;

	@Inject(method = "travel", at = @At("TAIL"))
	private void harpyFlightSpeed$boostFallFlying(Vec3 travelVector, CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (!entity.isFallFlying() || !(entity instanceof Player player)) {
			return;
		}
		if (!OriginUtil.hasOrigin(player, HARPY_ORIGIN_ID)) {
			return;
		}
		Vec3 velocity = entity.getDeltaMovement();
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		if (horizontalSpeed < 1.0e-4) {
			return;
		}
		double newSpeed;
		if (this.zza > 0.0F) {
			// Holding forward (W): closes part of the remaining gap to the cap every tick — fast
			// initial gain, tapering as the cap approaches.
			newSpeed = horizontalSpeed + (MAX_BOOSTED_HORIZONTAL_SPEED - horizontalSpeed) * ACCELERATION_RATE;
		} else if (this.zza < 0.0F) {
			// Holding backward (S): brakes, faster than passive decay.
			newSpeed = horizontalSpeed * BACKWARD_DECELERATION;
		} else {
			// Neither held: speed drifts back down on its own instead of holding steady.
			newSpeed = horizontalSpeed * PASSIVE_DECELERATION;
		}
		double scale = newSpeed / horizontalSpeed;
		entity.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
	}
}
