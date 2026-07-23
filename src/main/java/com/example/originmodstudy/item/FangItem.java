package com.example.originmodstudy.item;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * A light, fast dagger. Anyone can craft and swing it, but it only poisons on hit (undead
 * excluded, matching Arachne's own Venomous Bite power) if the wielder has the Arachne origin —
 * it's her weapon. Gating happens here rather than on the recipe: vanilla's crafting-recipe
 * matching has no access to which player is crafting, so restricting the recipe itself would need
 * a real custom recipe implementation; checking the attacker's origin at hit-time is both simpler
 * and the actually-correct place to enforce "whose weapon this is" for a melee weapon anyway.
 */
public class FangItem extends SwordItem {
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("arachne", "arachne");

	public FangItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (target.getMobType() != MobType.UNDEAD && OriginUtil.hasOrigin(attacker, ARACHNE_ORIGIN_ID)) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
		}
		return result;
	}
}
