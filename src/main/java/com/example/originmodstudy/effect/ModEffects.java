package com.example.originmodstudy.effect;

import com.example.originmodstudy.OriginModStudy;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Registers every status effect this mod adds. Fields are {@code static final} so they are
 * created (and registered) the moment this class is first referenced.
 *
 * <p>Fields are {@code Holder<MobEffect>}, not a plain {@code MobEffect}, as of the 1.21.1 port —
 * confirmed via decompiling vanilla's own {@code MobEffects} class: every vanilla constant
 * (e.g. {@code MobEffects.POISON}) is itself a {@code Holder<MobEffect>}, obtained through
 * {@code Registry.registerForHolder(...)} rather than the plain {@code Registry.register(...)}
 * this class used pre-port. Every real API that consumes an effect reference —
 * {@code MobEffectInstance}'s constructors, {@code LivingEntity#hasEffect/removeEffect} — takes
 * {@code Holder<MobEffect>} now, not {@code MobEffect} directly, so registering as a bare
 * {@code MobEffect} here would force every call site to re-wrap it.
 */
public class ModEffects {

	// Harpy's Talons power (see data/monster_origins/powers/harpy/talons.json) applies this on hit.
	// Color is a dried-blood red, distinct from vanilla Poison's green.
	public static final Holder<MobEffect> BLEED = register("bleed",
			new BleedMobEffect(MobEffectCategory.HARMFUL, 0x8B1A1A));

	// Mermaid's Call (see data/monster_origins/powers/mermaid/mermaids_call.json) applies this to
	// hostiles caught in the song. A pure marker — no tick behavior of its own, CharmedPassiveTargetMixin
	// only ever checks for its presence. MobEffect's constructor is protected, so an anonymous
	// subclass (rather than a whole new file, unlike BleedMobEffect) is enough for a no-op marker.
	public static final Holder<MobEffect> CHARMED = register("charmed",
			new MobEffect(MobEffectCategory.NEUTRAL, 0x8ED1E0) {});

	// Medusa's Petrifying Bite / Stone Gaze Burst powers (see data/monster_origins/powers/medusa/)
	// apply this for the movement/attack-speed "turned to stone" component; Blindness + Darkness
	// stay as separate vanilla effects stacked alongside it in each power's own effect list. Color
	// is a warm stone gray, distinct from vanilla Slowness/Mining Fatigue's cooler blue-gray tones.
	public static final Holder<MobEffect> PETRIFY = register("petrify",
			new PetrifyMobEffect(MobEffectCategory.HARMFUL, 0x8C8470));

	// Stone Gaze Burst's one-shot-cooldown gate (Task 16, not this task): applied alongside
	// PETRIFY with a much longer duration than Petrify's own. A pure marker, same as CHARMED above
	// — no tick behavior of its own, checked only via an inverted origins:status_effect condition
	// on the power itself to block re-triggering while still present.
	public static final Holder<MobEffect> STONE_GAZE_COOLDOWN = register("stone_gaze_cooldown",
			new MobEffect(MobEffectCategory.NEUTRAL, 0x9696A0) {});

	// Playtest fix (2026-08-02): a shared marker applied alongside Arachne's web bite, the Silk
	// Net Shooter, Medusa's petrify gaze, Medusa's petrifying bite, and the Petrifying Trident.
	// ImmobilizedRotationLockMixin is the only thing that ever checks for its presence — a pure
	// marker, same anonymous-subclass pattern as CHARMED/STONE_GAZE_COOLDOWN above, no attribute
	// modifiers or tick behavior of its own. The actual movement lock stays with whatever effect
	// each source already applies (Petrify's attributes, vanilla Slowness); this only adds the
	// rotation freeze on top.
	// Playtest fix: full rotation-lock (see ImmobilizedRotationLockMixin) left a trapped target
	// completely unable to fight back at all, even at point-blank range. Rather than remove the
	// lock, this gives back a small amount of agency: -1 to both real 1.21 reach attributes (the
	// same two ModItems.MERMAID_TRIDENT already grants a +1 bonus to), so a trapped target can
	// still swing at whoever's standing right next to them, just not at vanilla's normal range.
	public static final Holder<MobEffect> IMMOBILIZED = register("immobilized",
			new MobEffect(MobEffectCategory.HARMFUL, 0x707070) {}
					.addAttributeModifier(Attributes.BLOCK_INTERACTION_RANGE,
							ResourceLocation.fromNamespaceAndPath("monster_origins", "immobilized_block_reach"),
							-1.0, AttributeModifier.Operation.ADD_VALUE)
					.addAttributeModifier(Attributes.ENTITY_INTERACTION_RANGE,
							ResourceLocation.fromNamespaceAndPath("monster_origins", "immobilized_entity_reach"),
							-1.0, AttributeModifier.Operation.ADD_VALUE));

	private static Holder<MobEffect> register(String name, MobEffect effect) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, OriginModStudy.id(name), effect);
	}

	public static void registerModEffects() {
		// Registration happens via the static field initializers above; this method exists so
		// OriginModStudy can force this class to load at a predictable point during init, the
		// same convention ModItems/ModSounds use.
	}
}
