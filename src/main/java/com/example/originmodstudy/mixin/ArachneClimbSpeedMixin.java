package com.example.originmodstudy.mixin;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Arachne climbs faster than anything else alive. Vanilla offers no attribute or hook for climb
 * speed — every number involved is a bare literal inside {@link LivingEntity} — so this mixin
 * rewrites those literals for Arachne players only.
 *
 * <p>Verified against the real mapped 1.20.1 jar (decompiled with CFR, bytecode cross-checked with
 * {@code javap -p -c}) rather than assumed. Two separate methods hold the relevant constants, and
 * they do genuinely different jobs:
 *
 * <p><b>1. {@code LivingEntity#handleOnClimbable(Vec3)}</b> (private, declared on
 * {@code LivingEntity} — confirmed via {@code javap -p}) clamps movement while attached to a
 * climbable:
 * <pre>{@code
 * private Vec3 handleOnClimbable(Vec3 vec3) {
 *     if (this.onClimbable()) {
 *         this.resetFallDistance();
 *         float f = 0.15f;
 *         double d = Mth.clamp(vec3.x, -0.15f, 0.15f);
 *         double e = Mth.clamp(vec3.z, -0.15f, 0.15f);
 *         double g = Math.max(vec3.y, -0.15f);
 *         ...
 * }
 * }</pre>
 * Those are the lateral (x/z) speed cap and the downward-slide floor. In bytecode they are five
 * {@code ldc2_w} loads of {@code (double) 0.15f} / {@code (double) -0.15f} (the {@code float f}
 * local is dead code and is left alone), so a {@code @ModifyConstant} keyed on the exact double
 * value catches all of them in one handler.
 *
 * <p><b>2. {@code LivingEntity#handleRelativeFrictionAndCalculateMovement(Vec3, float)}</b> holds
 * the actual <i>vertical</i> climb speed, which is <i>not</i> in {@code handleOnClimbable} at all:
 * <pre>{@code
 * if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || ...powder snow...)) {
 *     vec32 = new Vec3(vec32.x, 0.2, vec32.z);
 * }
 * }</pre>
 * Pressing into a climbable surface forces y to a flat {@code 0.2} per tick. Without this second
 * hook, "climbs faster" would not actually speed up climbing — only strafing and sliding down — so
 * both are needed for the power to do what it says. There is exactly one {@code ldc2_w 0.2d} in
 * that method (checked in bytecode), so the constant is unambiguous.
 *
 * <p>{@code @ModifyConstant} is used rather than {@code @Redirect} on the {@code handleOnClimbable}
 * call site because the value that needs changing is the clamp <i>bound</i>, not the returned
 * vector: scaling the returned {@code Vec3} after the fact cannot undo a clamp that has already
 * discarded the excess speed, and it would also scale the y component in cases the clamp never
 * touched. Modifying the bounds themselves is both smaller and semantically exact.
 *
 * <p>The {@code 0.2} handler re-checks {@code onClimbable()} because that same branch is shared
 * with walking on powder snow — without the check, an Arachne player wearing leather boots would
 * also climb powder snow faster, which is not the intent. {@code onClimbable()} is public and
 * declared on {@code LivingEntity} itself, so it is reached by a plain cast rather than an
 * {@code @Shadow} stub, matching {@code ThrownTridentMixin}'s handling of {@code getOwner()}.
 */
@Mixin(LivingEntity.class)
public abstract class ArachneClimbSpeedMixin {
	private static final ResourceLocation ARACHNE_ORIGIN_ID =
		new ResourceLocation("monster_origins", "arachne");

	/** Arachne moves on climbable surfaces at 1.65x vanilla's fixed rate. */
	private static final double CLIMB_SPEED_MULTIPLIER = 1.65D;

	private boolean arachne$isArachneClimber() {
		return OriginUtil.hasOrigin((LivingEntity) (Object) this, ARACHNE_ORIGIN_ID);
	}

	/**
	 * Raises the lateral movement cap and the downward-slide floor while on a climbable. Only
	 * reached from inside {@code handleOnClimbable}'s own {@code if (this.onClimbable())} branch,
	 * so no extra climbing check is needed here.
	 */
	@ModifyConstant(method = "handleOnClimbable", constant = @Constant(doubleValue = (double) 0.15F))
	private double arachne$fasterClimbClampUpper(double constant) {
		return arachne$isArachneClimber() ? constant * CLIMB_SPEED_MULTIPLIER : constant;
	}

	@ModifyConstant(method = "handleOnClimbable", constant = @Constant(doubleValue = (double) -0.15F))
	private double arachne$fasterClimbClampLower(double constant) {
		return arachne$isArachneClimber() ? constant * CLIMB_SPEED_MULTIPLIER : constant;
	}

	/**
	 * Raises the flat upward climb speed applied when pressing into a climbable surface. This is
	 * the constant a player actually feels as "climbing speed".
	 */
	@ModifyConstant(
		method = "handleRelativeFrictionAndCalculateMovement",
		constant = @Constant(doubleValue = 0.2D)
	)
	private double arachne$fasterClimbAscent(double constant) {
		if (!((LivingEntity) (Object) this).onClimbable()) {
			// Shared branch: also runs for walking on powder snow, which must stay vanilla.
			return constant;
		}
		return arachne$isArachneClimber() ? constant * CLIMB_SPEED_MULTIPLIER : constant;
	}
}
