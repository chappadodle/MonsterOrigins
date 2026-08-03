package com.example.originmodstudy.item;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Medusa's Serpent Aegis (Task 19): an off-hand defensive item extending vanilla
 * {@link ShieldItem} directly, inheriting its real raise/block/disable mechanics unchanged
 * (confirmed via {@code javap} that {@code ShieldItem} exposes nothing that needs overriding for
 * this — no custom block-detection path was worth building from scratch). Anyone can craft and
 * raise it, but blocking a melee attack only inflicts Slowness on the attacker for a Medusa
 * wielder — see {@code SerpentAegisBlockMixin} for why that has to live in a mixin on
 * {@code LivingEntity.blockedByShield} rather than here (this class has no on-block hook of its
 * own to override; blocking is resolved entirely inside vanilla's damage-taking pipeline).
 *
 * <p>Blocks a projectile no differently than any other shield (full vanilla behavior, matching
 * the source doc's "no effect vs projectiles" — that's simply what a plain shield already does,
 * no extra code needed to make it *not* do something extra there).
 */
public class SerpentAegisItem extends ShieldItem {
	public SerpentAegisItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Blocking a melee attack slows the attacker and reflects half the damage");
		OriginUtil.addOriginGatedTooltip(tooltip, "Immune to Ghast fireballs");
		OriginUtil.addOriginGatedTooltip(tooltip, "Nearby allies take half damage while you block");
		OriginUtil.addOriginGatedTooltip(tooltip, "Medusa only");
	}
}
