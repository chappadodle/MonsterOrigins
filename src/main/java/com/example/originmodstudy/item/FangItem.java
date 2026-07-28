package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
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
 * top of Poison: {@code arachne:fang} (base, Poison I only, {@code maxPoisonTier} 1),
 * {@code arachne:venomfang} (Poison escalating to II across repeat hits, {@code maxPoisonTier} 2,
 * + Bleed, crafted from Fang + diamond + a golden spider eye on a crafting table), and
 * {@code arachne:widowfang} (Poison escalating to III, {@code maxPoisonTier} 3, + Wither too,
 * upgraded from Venomfang via a smithing-table {@code smithing_transform} recipe — netherite
 * ingot + another golden spider eye — matching vanilla's own Diamond-to-Netherite upgrade
 * convention rather than a plain crafting recipe).
 *
 * <p>Poison tiering uses {@link TieredHitTracker}: repeat qualifying hits by the same attacker on
 * the same target within {@link #POISON_RESET_TICKS} escalate the amplifier by one, up to
 * {@code maxPoisonTier - 1}, then reset to Poison I once that window lapses. Wither stays flat
 * (untiered) on Widowfang, unaffected by this.
 */
public class FangItem extends SwordItem {
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("monster_origins", "arachne");

	/** Shared "repeat hit" tracker key for all three Fang tiers — a target's poison tier climbs
	 * the same sequence regardless of which tier actually lands the follow-up hit. */
	private static final String POISON_TRACKER_KEY = "arachne_poison";

	// 8 seconds. This is the project's chosen reset-window convention for tiered on-hit effects —
	// Task 9's Harpy Talon Gauntlet bleed tiers are meant to match this same value for consistency
	// (see TieredHitTracker's own class doc).
	private static final int POISON_RESET_TICKS = 160;

	private final int maxPoisonTier;
	private final boolean bleedOnHit;
	private final boolean witherOnHit;

	public FangItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, int maxPoisonTier,
			boolean bleedOnHit, boolean witherOnHit, Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
		this.maxPoisonTier = maxPoisonTier;
		this.bleedOnHit = bleedOnHit;
		this.witherOnHit = witherOnHit;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (OriginUtil.hasOrigin(attacker, ARACHNE_ORIGIN_ID)) {
			if (target.getMobType() != MobType.UNDEAD) {
				int tier = TieredHitTracker.nextTier(target, attacker, POISON_TRACKER_KEY, maxPoisonTier,
						POISON_RESET_TICKS);
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, tier - 1));
				if (bleedOnHit) {
					target.addEffect(new MobEffectInstance(ModEffects.BLEED, 100, 0));
				}
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
		OriginUtil.addOriginGatedTooltip(tooltip, "Poisons on hit");
		if (bleedOnHit) {
			OriginUtil.addOriginGatedTooltip(tooltip, "+ Bleed");
		}
		if (witherOnHit) {
			OriginUtil.addOriginGatedTooltip(tooltip, "+ Wither");
		}
		OriginUtil.addOriginGatedTooltip(tooltip, "Arachne only");
	}
}
