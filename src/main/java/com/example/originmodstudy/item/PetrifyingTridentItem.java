package com.example.originmodstudy.item;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A reskinned trident that briefly petrifies whatever it hits — a lighter dose (shorter
 * duration, lower amplifiers) than Medusa's own Petrifying Bite origin power — but only if the
 * wielder has the Medusa origin; it's her weapon. See FangItem for why this gating happens at
 * hit-time rather than on the crafting recipe.
 */
public class PetrifyingTridentItem extends TridentItem {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = new ResourceLocation("arachne", "medusa");

	public PetrifyingTridentItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (OriginUtil.hasOrigin(attacker, MEDUSA_ORIGIN_ID)) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
			target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0));
			target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Petrifies on hit — Medusa only.");
	}
}
