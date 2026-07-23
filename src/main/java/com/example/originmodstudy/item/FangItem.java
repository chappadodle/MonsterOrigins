package com.example.originmodstudy.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * A light, fast dagger. Poisons on every hit (undead excluded, matching Arachne's own
 * Venomous Bite power) regardless of whether the wielder has the Arachne origin — the poison
 * is a property of the weapon itself, not origin-gated.
 */
public class FangItem extends SwordItem {
	public FangItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (target.getMobType() != MobType.UNDEAD) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
		}
		return result;
	}
}
