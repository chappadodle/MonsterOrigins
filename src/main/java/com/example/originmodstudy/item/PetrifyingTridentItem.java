package com.example.originmodstudy.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;

/**
 * A reskinned trident that briefly petrifies whatever it hits — a lighter dose (shorter
 * duration, lower amplifiers) than Medusa's own Petrifying Bite origin power, since any
 * wielder gets this on-hit effect, origin or not.
 */
public class PetrifyingTridentItem extends TridentItem {
	public PetrifyingTridentItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0));
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
		return result;
	}
}
