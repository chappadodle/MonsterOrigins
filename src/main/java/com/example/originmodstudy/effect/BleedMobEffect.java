package com.example.originmodstudy.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * A from-scratch clone of vanilla Poison's real tick behavior. Verified via {@code javap} on the
 * actual game classes that Poison/Wither/Regeneration aren't implemented via subclass overrides
 * at all — their damage logic is hardcoded by identity-check
 * ({@code if (this == MobEffects.POISON) {...}}) directly inside the shared base
 * {@code MobEffect.applyEffectTick()} method. A brand new effect gets none of that behavior for
 * free, so this class reproduces it exactly: damage every {@code 25 >> amplifier} ticks (Poison
 * I's real interval), 1 damage via a magic damage source, never dropping health at or below 1.
 *
 * <p>Also confirmed vanilla's undead-immunity check ({@code LivingEntity#canBeAffected}) only
 * special-cases the literal vanilla Poison/Regeneration objects, so this effect does <b>not</b>
 * inherit that exemption automatically — callers applying Bleed are responsible for excluding
 * undead themselves (see {@code talons.json}'s {@code target_condition}), the same pattern
 * already used for Arachne's own on-hit poison.
 */
public class BleedMobEffect extends MobEffect {
	public BleedMobEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		int interval = 25 >> amplifier;
		if (interval > 0) {
			return duration % interval == 0;
		}
		return true;
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity.getHealth() > 1.0F) {
			entity.hurt(entity.damageSources().magic(), 1.0F);
		}
	}
}
