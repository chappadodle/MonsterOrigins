package com.example.originmodstudy.mixin;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The real reason bumping {@code swim_speed.json}'s multiplier never felt like anything —
 * decompiled {@code LivingEntity.travel(Vec3)}'s water branch again (same method {@code
 * MermaidWaterTurningMixin} already targets) and found a line that mixin's own doc comment
 * quotes but doesn't fully account for: {@code float f = this.isSprinting() ? 0.9f :
 * this.getWaterSlowDown();}. Sprint-swimming — the normal way anyone actually swims fast in
 * vanilla — takes the {@code isSprinting()} branch, which hardcodes retention to a flat
 * {@code 0.9f} and never calls {@code getWaterSlowDown()} at all. {@code
 * MermaidWaterTurningMixin}'s override (0.98, near-zero drag) only ever applied while *not*
 * sprinting — meaning the actual retention fix, and by extension most of the felt benefit of
 * every swim-speed multiplier bump since, was silently inactive for anyone swimming the normal
 * way.
 *
 * <p><b>First attempt at this fix was a real regression, caught by the user's own playtest:</b>
 * forcing {@code Entity.isSprinting()} itself to report {@code false} while a Mermaid is in
 * water (a global override, affecting every caller) also broke the actual swimming pose —
 * decompiling {@code Entity.updateSwimming()} shows {@code this.setSwimming(this.isSprinting()
 * && this.isUnderWater() && ...)}, the exact flag that puts the entity into the horizontal
 * swimming animation/hitbox at all. Disabling {@code isSprinting()} while in water meant the
 * Mermaid moved through water while stuck in the upright walking pose — reported back as "can't
 * swim, just walks... more similar to gliding." Fixed properly this time with {@code
 * @ModifyConstant} (standard Sponge Mixin, not a new dependency — the same annotation
 * Additional Entity Attributes' own mixin already uses elsewhere) targeting the literal
 * {@code 0.9f} constant directly, confirmed via decompile to appear exactly once in the entire
 * {@code travel()} method body — no ordinal ambiguity, unlike the local-variable techniques this
 * project has otherwise avoided in this same method. This changes only the *value* substituted
 * into the ternary's true branch, without touching {@code isSprinting()} itself or anything that
 * calls it for pose/hunger/FOV purposes.
 */
@Mixin(LivingEntity.class)
public abstract class MermaidSprintSwimMixin {
	private static final ResourceLocation MERMAID_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "mermaid");
	private static final float MERMAID_SPRINT_SWIM_RETENTION = 0.98F;

	@ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.9F))
	private float mermaidSprintSwim$fixSprintDrag(float original) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof Player player && OriginUtil.hasOrigin(player, MERMAID_ORIGIN_ID)) {
			return MERMAID_SPRINT_SWIM_RETENTION;
		}
		return original;
	}
}
