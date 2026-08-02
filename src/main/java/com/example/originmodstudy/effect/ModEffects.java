package com.example.originmodstudy.effect;

import com.example.originmodstudy.OriginModStudy;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Registers every status effect this mod adds. Fields are {@code static final} so they are
 * created (and registered) the moment this class is first referenced.
 */
public class ModEffects {

	// Harpy's Talons power (see data/monster_origins/powers/harpy/talons.json) applies this on hit.
	// Color is a dried-blood red, distinct from vanilla Poison's green.
	public static final MobEffect BLEED = register("bleed",
			new BleedMobEffect(MobEffectCategory.HARMFUL, 0x8B1A1A));

	// Mermaid's Call (see data/monster_origins/powers/mermaid/mermaids_call.json) applies this to
	// hostiles caught in the song. A pure marker — no tick behavior of its own, CharmedPassiveTargetMixin
	// only ever checks for its presence. MobEffect's constructor is protected, so an anonymous
	// subclass (rather than a whole new file, unlike BleedMobEffect) is enough for a no-op marker.
	public static final MobEffect CHARMED = register("charmed",
			new MobEffect(MobEffectCategory.NEUTRAL, 0x8ED1E0) {});

	// Medusa's Petrifying Bite / Stone Gaze Burst powers (see data/monster_origins/powers/medusa/)
	// apply this for the movement/attack-speed "turned to stone" component; Blindness + Darkness
	// stay as separate vanilla effects stacked alongside it in each power's own effect list. Color
	// is a warm stone gray, distinct from vanilla Slowness/Mining Fatigue's cooler blue-gray tones.
	public static final MobEffect PETRIFY = register("petrify",
			new PetrifyMobEffect(MobEffectCategory.HARMFUL, 0x8C8470));

	// Stone Gaze Burst's one-shot-cooldown gate (Task 16, not this task): applied alongside
	// PETRIFY with a much longer duration than Petrify's own. A pure marker, same as CHARMED above
	// — no tick behavior of its own, checked only via an inverted origins:status_effect condition
	// on the power itself to block re-triggering while still present.
	public static final MobEffect STONE_GAZE_COOLDOWN = register("stone_gaze_cooldown",
			new MobEffect(MobEffectCategory.NEUTRAL, 0x9696A0) {});

	// Playtest fix (2026-08-02): a shared marker applied alongside Arachne's web bite, the Silk
	// Net Shooter, Medusa's petrify gaze, Medusa's petrifying bite, and the Petrifying Trident.
	// ImmobilizedRotationLockMixin is the only thing that ever checks for its presence — a pure
	// marker, same anonymous-subclass pattern as CHARMED/STONE_GAZE_COOLDOWN above, no attribute
	// modifiers or tick behavior of its own. The actual movement lock stays with whatever effect
	// each source already applies (Petrify's attributes, vanilla Slowness); this only adds the
	// rotation freeze on top.
	public static final MobEffect IMMOBILIZED = register("immobilized",
			new MobEffect(MobEffectCategory.HARMFUL, 0x707070) {});

	private static MobEffect register(String name, MobEffect effect) {
		return Registry.register(BuiltInRegistries.MOB_EFFECT, OriginModStudy.id(name), effect);
	}

	public static void registerModEffects() {
		// Registration happens via the static field initializers above; this method exists so
		// OriginModStudy can force this class to load at a predictable point during init, the
		// same convention ModItems/ModSounds use.
	}
}
