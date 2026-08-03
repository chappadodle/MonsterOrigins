package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.util.ArachnePoisonLock;
import com.example.originmodstudy.util.OriginUtil;
import com.example.originmodstudy.util.TieredHitTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A light, fast dagger — anyone can craft and swing it, but the on-hit effects only trigger for
 * the Arachne origin (see OriginUtil). Three tiers share this one class rather than three
 * near-duplicate classes, since the only real difference between them is which effects layer on
 * top of Poison: {@code arachne:fang} (base, starts at Poison I and escalates to III,
 * {@code basePoisonTier} 1), {@code arachne:venomfang} (starts at Poison II and escalates to III,
 * {@code basePoisonTier} 2, + Bleed, crafted from Fang + diamond + a golden spider eye on a
 * crafting table), and {@code arachne:widowfang} (Poison III instantly, no escalation needed,
 * {@code basePoisonTier} 3, + Wither too, upgraded from Venomfang via a smithing-table
 * {@code smithing_transform} recipe — netherite ingot + another golden spider eye — matching
 * vanilla's own Diamond-to-Netherite upgrade convention rather than a plain crafting recipe).
 *
 * <p><b>Tier mapping (Task 6 correction):</b> all three tiers share one {@link TieredHitTracker}
 * sequence under {@link #POISON_TRACKER_KEY} — a target's raw hit count climbs 1, 2, 3 regardless
 * of which Fang tier actually lands each hit, exactly as before. What changed is how that raw
 * count turns into the Poison tier actually applied: each weapon has its own floor
 * ({@code basePoisonTier}), and the effective tier is {@code min(basePoisonTier + rawTier - 1,
 * MAX_POISON_TIER)}. That one formula produces every weapon's documented behavior with no
 * per-weapon special casing: Fang (floor 1) reads straight off the raw tier (1, 2, 3 across three
 * hits); Venomfang (floor 2) starts at II on the first hit and reaches III on the second
 * ({@code min(2 + 0, 3)=2}, {@code min(2 + 1, 3)=3}); Widowfang (floor 3) is already at the cap on
 * the very first hit and the formula can never push it past {@code MAX_POISON_TIER}, so it reads
 * as "instant Tier 3, no escalation" for free. {@link TieredHitTracker#nextTier} is always called
 * with {@code MAX_POISON_TIER} (3), not each weapon's own floor, specifically so the raw shared
 * sequence stays capable of reaching 3 no matter which tier lands each hit — including Widowfang,
 * whose raw tier is thrown away in favor of the formula's already-capped result, but is still
 * requested every hit so the shared sequence keeps advancing correctly if the player swaps
 * weapons mid-fight.
 *
 * <p>Poison duration now scales with the effective tier actually applied (200/240/280 ticks for
 * I/II/III), rather than the flat 60 ticks every tier used to get.
 *
 * <p>Undead gating is now tier-dependent rather than blanket: Tier 1 Poison keeps vanilla Poison's
 * own real undead immunity, but Tier 2 and 3 now pierce it (an explicit, deliberate deviation from
 * vanilla for this addon's own tiers, confirmed with the maintainer). The raw hit sequence still
 * advances even on a hit whose resulting tier gets blocked by that immunity, so a first hit on an
 * undead target that computes (and discards) Tier 1 still counts toward a second hit correctly
 * computing Tier 2. Bleed keeps its own, separate, unconditional undead exclusion (matching
 * {@code talons.json}'s own pattern) — independent of the Poison undead-piercing change above, so
 * restructuring this method must not let the two share a single undead check. Wither stays flat
 * (untiered) and unaffected by any of this, same as before.
 *
 * <p>Whenever a hit actually lands Poison here, the target is marked via
 * {@link ArachnePoisonLock} so {@code ArachnePoisonLockMixin} can watch for it dropping to half a
 * heart and lock it down with Slowness and Blindness until it heals back up or dies.
 */
public class FangItem extends SwordItem {
	private static final ResourceLocation ARACHNE_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "arachne");

	/** Shared "repeat hit" tracker key for all three Fang tiers — a target's poison tier climbs
	 * the same sequence regardless of which tier actually lands the follow-up hit. */
	private static final String POISON_TRACKER_KEY = "arachne_poison";

	// 8 seconds. This is the project's chosen reset-window convention for tiered on-hit effects —
	// Task 9's Harpy Talon Gauntlet bleed tiers are meant to match this same value for consistency
	// (see TieredHitTracker's own class doc).
	private static final int POISON_RESET_TICKS = 160;

	/** The highest Poison tier any Fang can ever apply, and the cap passed to every
	 * {@link TieredHitTracker#nextTier} call regardless of which weapon tier is swinging (see the
	 * class doc's tier mapping section for why). */
	private static final int MAX_POISON_TIER = 3;

	// Index 0 = Tier 1 (10s), index 1 = Tier 2 (12s), index 2 = Tier 3 (14s) — each tier lasts 2
	// seconds longer than the one before it, per the design doc.
	private static final int[] POISON_DURATION_TICKS = {200, 240, 280};

	private final int basePoisonTier;
	private final boolean bleedOnHit;
	private final boolean witherOnHit;

	public FangItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, int basePoisonTier,
			boolean bleedOnHit, boolean witherOnHit, Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
		this.basePoisonTier = basePoisonTier;
		this.bleedOnHit = bleedOnHit;
		this.witherOnHit = witherOnHit;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (OriginUtil.hasOrigin(attacker, ARACHNE_ORIGIN_ID)) {
			boolean targetIsUndead = target.getMobType() == MobType.UNDEAD;

			// Always advance the shared sequence, even for a hit that ends up not applying Poison
			// below (blocked by undead immunity) — otherwise that hit wouldn't count and a real
			// follow up hit would compute the wrong tier. See the class doc's tier mapping section.
			int rawTier = TieredHitTracker.nextTier(target, attacker, POISON_TRACKER_KEY, MAX_POISON_TIER,
					POISON_RESET_TICKS);
			int effectiveTier = Math.min(basePoisonTier + rawTier - 1, MAX_POISON_TIER);

			// Tier 1 keeps vanilla Poison's own real undead immunity; Tier 2 and 3 pierce it.
			if (effectiveTier >= 2 || !targetIsUndead) {
				int duration = POISON_DURATION_TICKS[effectiveTier - 1];
				target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, effectiveTier - 1));
				ArachnePoisonLock.markCandidate(target);
			}

			// Bleed keeps its own, separate, unconditional undead exclusion — not folded into the
			// Poison undead check above, which now varies by tier.
			if (bleedOnHit && !targetIsUndead) {
				target.addEffect(new MobEffectInstance(ModEffects.BLEED, 100, 0));
			}

			if (witherOnHit) {
				// Unlike Poison, Wither was never vanilla-blocked on the undead — confirmed via
				// LivingEntity#canBeAffected, which only special-cases Poison/Regeneration — so
				// this applies to undead too. WitherBoss itself is separately immune to Wither
				// via its own canBeAffected override, enforced automatically by addEffect, no
				// extra check needed here. Wither stays flat/untiered, unaffected by the poison
				// tiering above.
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
			}
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		if (basePoisonTier >= 2) {
			OriginUtil.addOriginGatedTooltip(tooltip, "Poisons on hit, starting at Tier " + basePoisonTier);
		} else {
			OriginUtil.addOriginGatedTooltip(tooltip, "Poisons on hit");
		}
		if (bleedOnHit) {
			OriginUtil.addOriginGatedTooltip(tooltip, "+ Bleed");
		}
		if (witherOnHit) {
			OriginUtil.addOriginGatedTooltip(tooltip, "+ Wither");
		}
		OriginUtil.addOriginGatedTooltip(tooltip, "Arachne only");
	}
}
