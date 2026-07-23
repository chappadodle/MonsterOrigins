package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.util.OriginUtil;
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
 * top of Poison: {@code arachne:fang} (base, Poison only), {@code arachne:venomfang} (+ Bleed,
 * crafted from Fang + diamond + a golden spider eye on a crafting table), and
 * {@code arachne:widowfang} (+ Wither too, upgraded from Venomfang via a smithing-table
 * {@code smithing_transform} recipe — netherite ingot + another golden spider eye — matching
 * vanilla's own Diamond-to-Netherite upgrade convention rather than a plain crafting recipe).
 */
public class FangItem extends SwordItem {
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("monster_origins", "arachne");

	private final boolean bleedOnHit;
	private final boolean witherOnHit;

	public FangItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, boolean bleedOnHit,
			boolean witherOnHit, Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
		this.bleedOnHit = bleedOnHit;
		this.witherOnHit = witherOnHit;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (OriginUtil.hasOrigin(attacker, ARACHNE_ORIGIN_ID)) {
			if (target.getMobType() != MobType.UNDEAD) {
				target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
				if (bleedOnHit) {
					target.addEffect(new MobEffectInstance(ModEffects.BLEED, 100, 0));
				}
			}
			if (witherOnHit) {
				// Unlike Poison, Wither was never vanilla-blocked on the undead — confirmed via
				// LivingEntity#canBeAffected, which only special-cases Poison/Regeneration — so
				// this applies to undead too. WitherBoss itself is separately immune to Wither
				// via its own canBeAffected override, enforced automatically by addEffect, no
				// extra check needed here.
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
