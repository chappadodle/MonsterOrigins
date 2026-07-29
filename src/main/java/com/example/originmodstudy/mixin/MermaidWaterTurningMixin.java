package com.example.originmodstudy.mixin;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixing-doc pass: fixes the real cause this project already identified but never built a fix
 * for (see CLAUDE.md's own earlier gotcha) — swimming "feels slow/sliding, hard to turn" no
 * matter how high {@code swim_speed.json}'s own multiplier goes, because that data value only
 * ever affects the separate {@code additionalentityattributes:water_speed} attribute, not the
 * actual per-tick momentum retention vanilla applies to every entity in water.
 *
 * <p>Decompiled (via a locally fetched CFR jar) {@code LivingEntity.travel(Vec3)}'s real water
 * branch directly to confirm: {@code float f = this.isSprinting() ? 0.9f :
 * this.getWaterSlowDown(); ... this.setDeltaMovement(vec32.multiply(f, 0.8f, f));} —
 * {@code getWaterSlowDown()} (a plain, protected, non-final method returning a flat {@code 0.8f})
 * is the real horizontal momentum-retention constant applied every tick to anyone in water. A
 * flat 0.8 retention (losing 20% of horizontal velocity every tick, tempered only slightly by
 * pending direction changes) is exactly what produces the "sliding on ice, sluggish to turn"
 * feeling, independent of how fast the entity's *target* swim speed is set — a higher target speed
 * just means a bigger number to slide around at.
 *
 * <p>This overrides {@code getWaterSlowDown()}'s return value to {@code 0.98f} (minimal retention
 * loss, snappy and responsive) for a Mermaid-origin player specifically, leaving every other
 * entity (including non-Mermaid players) at vanilla's real 0.8. Deliberately doesn't touch the
 * *vertical* retention (a separate, literal {@code 0.8f} hardcoded directly in the same
 * {@code multiply(...)} call, not read from this method) — fixing that would need one of the
 * fragile ordinal-based local-variable techniques this project has otherwise avoided (see
 * {@code HarpyFlightSpeedMixin}'s own doc for the same reasoning), and the reported complaint is
 * specifically about turning/steering, a horizontal-plane concern.
 */
@Mixin(LivingEntity.class)
public abstract class MermaidWaterTurningMixin {
	private static final ResourceLocation MERMAID_ORIGIN_ID = new ResourceLocation("monster_origins", "mermaid");
	private static final float MERMAID_WATER_RETENTION = 0.98F;

	@Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true)
	private void mermaidWaterTurning$reduceDrag(CallbackInfoReturnable<Float> cir) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof Player player && OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			cir.setReturnValue(MERMAID_WATER_RETENTION);
		}
	}
}
