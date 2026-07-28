package com.example.originmodstudy.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Medusa's Petrify effect: the "turned to stone" movement/attack-speed component of
 * {@code petrifying_bite.json}/{@code stone_gaze_burst.json} (Blindness and Darkness stay as
 * separate vanilla effects stacked alongside this one in each power's own effect list — this
 * class only encodes the speed part).
 *
 * <p>Unlike {@link BleedMobEffect} (which reproduces Poison's real per-tick damage since vanilla's
 * tick-damage effects are hardcoded by reference-identity inside {@code MobEffect} itself and
 * grant nothing to a subclass for free — see this project's CLAUDE.md), Petrify needs no tick
 * override at all: attribute modifiers are a genuine, documented extension point on
 * {@code MobEffect} itself, applied/removed automatically by vanilla whenever the effect is
 * added/expires.
 *
 * <p>Construction mirrors vanilla's own real attribute-modifier effects exactly — confirmed via a
 * CFR decompile of {@code MobEffects.class} out of the mapped Minecraft jar (Loom's cache had no
 * sources jar, same gap this project's other gotchas about {@code TridentItem}/{@code
 * ThrownTrident} hit, filled the same way): e.g. {@code MobEffects.MOVEMENT_SLOWDOWN} is built as
 * {@code new MobEffect(HARMFUL, color).addAttributeModifier(Attributes.MOVEMENT_SPEED,
 * "<uuid>", -0.15, MULTIPLY_TOTAL)}. {@code MULTIPLY_TOTAL} applies its value as the signed
 * fractional change on the already-fully-computed attribute (final = base * (1 + value)) — not
 * the "added fraction before a final +1" behavior {@code MULTIPLY_BASE} has (see this project's
 * own gotcha about that operation, hit while planning Mermaid's swim speed). A value of -0.9/-0.8
 * here is therefore a real, literal -90%/-80% reduction, strong enough to read as "turned to
 * stone" rather than a reskinned Slowness III/Mining Fatigue III.
 *
 * <p>The two UUID strings are freshly generated (not reused from any vanilla or other mod
 * modifier) — {@code addAttributeModifier} keys its internal map by {@link
 * net.minecraft.world.entity.ai.attributes.Attribute}, but a real, unique UUID per modifier is
 * still the documented/expected shape for this API (every vanilla example does the same), so one
 * was minted per attribute rather than reused.
 */
public class PetrifyMobEffect extends MobEffect {
	private static final String MOVEMENT_SPEED_MODIFIER_UUID = "7ce5f6da-6cab-4e19-a6c4-1f471e04bb8b";
	private static final String ATTACK_SPEED_MODIFIER_UUID = "3698061e-4048-40fc-87a5-cb2ca0c05f0a";

	public PetrifyMobEffect(MobEffectCategory category, int color) {
		super(category, color);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER_UUID,
				-0.9, AttributeModifier.Operation.MULTIPLY_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID,
				-0.8, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}
}
