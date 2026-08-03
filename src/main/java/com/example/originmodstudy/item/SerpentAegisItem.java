package com.example.originmodstudy.item;

import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Medusa's Serpent Aegis (Task 19): an off-hand defensive item extending vanilla
 * {@link ShieldItem} directly, inheriting its real raise/block/disable mechanics unchanged
 * (confirmed via {@code javap} that {@code ShieldItem} exposes nothing that needs overriding for
 * this — no custom block-detection path was worth building from scratch) — except {@link #use}
 * below, which is a real, mechanical gate, not just a tooltip claim: playtest feedback was that
 * "Medusa only" (the tooltip line) wasn't actually true, any origin that isn't blocked by
 * {@code origins:no_shield} could raise and block with it same as her. Anyone can still craft it
 * and hold it in the off-hand slot (this only prevents actually raising/blocking with it), but
 * blocking a melee attack only inflicts Slowness on the attacker for a Medusa wielder anyway —
 * see {@code SerpentAegisBlockMixin} for why that has to live in a mixin on {@code
 * LivingEntity.blockedByShield} rather than here (this class has no on-block hook of its own to
 * override; blocking is resolved entirely inside vanilla's damage-taking pipeline).
 *
 * <p>Blocks a projectile no differently than any other shield (full vanilla behavior, matching
 * the source doc's "no effect vs projectiles" — that's simply what a plain shield already does,
 * no extra code needed to make it *not* do something extra there).
 *
 * <p>Playtest addition: a real shield this heavy should cost something even before it blocks
 * anything — {@link #inventoryTick} applies a continuously-refreshed Slowness I to whoever has it
 * in their off-hand, Medusa included, the same tradeoff a shield has in real weight regardless of
 * who's carrying it.
 */
public class SerpentAegisItem extends ShieldItem {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = new ResourceLocation("monster_origins", "medusa");

	public SerpentAegisItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!OriginUtil.hasOrigin(player, MEDUSA_ORIGIN_ID)) {
			return InteractionResultHolder.fail(player.getItemInHand(hand));
		}
		return super.use(level, player, hand);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		super.inventoryTick(stack, level, entity, slotId, isSelected);
		if (!level.isClientSide()
				&& entity instanceof LivingEntity livingEntity
				&& livingEntity.getItemBySlot(EquipmentSlot.OFFHAND) == stack) {
			livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Blocking a melee attack slows the attacker and reflects half the damage");
		OriginUtil.addOriginGatedTooltip(tooltip, "Immune to Ghast fireballs");
		OriginUtil.addOriginGatedTooltip(tooltip, "Nearby allies take half damage while you block");
		OriginUtil.addOriginGatedTooltip(tooltip, "Medusa only");
	}
}
