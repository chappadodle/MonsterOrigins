package com.example.originmodstudy.effect;

import net.minecraft.resources.ResourceLocation;
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
 * <id>, ADD_MULTIPLIED_TOTAL)}. {@code ADD_MULTIPLIED_TOTAL} applies its value as the signed
 * fractional change on the already-fully-computed attribute (final = base * (1 + value)) — not
 * the "added fraction before a final +1" behavior {@code ADD_MULTIPLIED_BASE} has (see this
 * project's own gotcha about that operation, hit while planning Mermaid's swim speed — written
 * under this same operation's pre-1.21.1 name, {@code MULTIPLY_BASE}). A value of -0.9/-0.8 here is
 * therefore a real, literal -90%/-80% reduction, strong enough to read as "turned to stone" rather
 * than a reskinned Slowness III/Mining Fatigue III.
 *
 * <p><b>1.21.1 port:</b> {@code addAttributeModifier}'s identity key changed from a {@code String}
 * UUID to a real {@link net.minecraft.resources.ResourceLocation}, and {@code
 * AttributeModifier.Operation}'s own constants were renamed ({@code ADDITION}→{@code ADD_VALUE},
 * {@code MULTIPLY_BASE}→{@code ADD_MULTIPLIED_BASE}, {@code MULTIPLY_TOTAL}→
 * {@code ADD_MULTIPLIED_TOTAL} — confirmed via {@code javap} on
 * {@code MobEffect.class} — same {@code Holder<T>}/{@code ResourceLocation}-identity migration hit
 * throughout the rest of this port, see CLAUDE.md/the task-4 fixing doc). The two modifier ids
 * below are freshly minted {@code monster_origins} namespace ids (not reused from any vanilla or
 * other mod modifier) — {@code addAttributeModifier} keys its internal map by {@link
 * net.minecraft.world.entity.ai.attributes.Attribute}, but a real, unique id per modifier is still
 * the documented/expected shape for this API (every vanilla example does the same), so one was
 * minted per attribute rather than reused.
 *
 * <p><b>Playtest fix (2026-08-02):</b> movement speed changed from -90% to a true -100% — the
 * report asked for petrified targets to be "completely locked in place," and -90% still let a
 * target creep, however slowly. {@code MULTIPLY_TOTAL}'s formula above confirms -1.0 clamps
 * cleanly to exactly 0, not a negative/backward value. (Rotation is handled separately, by
 * {@code ImmobilizedRotationLockMixin} reacting to the companion {@code ModEffects.IMMOBILIZED}
 * marker applied alongside this effect at each of its two application sites —
 * {@code gaze_petrify.json} and {@code PetrifyingTridentItem.applyPetrify} — rather than from
 * inside this class, since {@code MobEffect} has no "effect just started" hook in this Minecraft
 * version to centralize that from, confirmed via {@code javap} before assuming one existed.)
 */
public class PetrifyMobEffect extends MobEffect {
	private static final ResourceLocation MOVEMENT_SPEED_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "petrify_movement_speed");
	private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath("monster_origins", "petrify_attack_speed");

	public PetrifyMobEffect(MobEffectCategory category, int color) {
		super(category, color);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED_MODIFIER_ID,
				-1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_ID,
				-0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}
