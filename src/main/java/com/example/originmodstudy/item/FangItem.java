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
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("arachne", "arachne");

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
		if (target.getMobType() != MobType.UNDEAD && OriginUtil.hasOrigin(attacker, ARACHNE_ORIGIN_ID)) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
			if (bleedOnHit) {
				target.addEffect(new MobEffectInstance(ModEffects.BLEED, 100, 0));
			}
			if (witherOnHit) {
				target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
			}
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		StringBuilder text = new StringBuilder("Poisons on hit (undead immune)");
		if (bleedOnHit) {
			text.append(", also causes Bleed");
		}
		if (witherOnHit) {
			text.append(" and Wither");
		}
		text.append(" — Arachne only.");
		OriginUtil.addOriginGatedTooltip(tooltip, text.toString());
	}
}
