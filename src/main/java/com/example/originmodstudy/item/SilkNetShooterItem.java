package com.example.originmodstudy.item;

import com.example.originmodstudy.entity.ThrownSilkNet;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Arachne's Silk Net Shooter — fires a {@link ThrownSilkNet} that traps whatever it hits in a
 * temporary cobweb plus a short heavy Slowness. A plain {@code Item} with a custom {@link #use}
 * is sufficient here (no charge-and-release like {@code TridentItem}/{@code HarpyJavelinItem}
 * need): confirmed by decompiling vanilla's own {@code SnowballItem.use}, which this method is a
 * faithful adaptation of (same instant-fire-on-right-click shape, sound-then-spawn-then-consume
 * order), just spending durability instead of shrinking a stack — see the task report for why
 * durability (not stack count) is the right model for this weapon's "20 uses."
 *
 * <p>Anyone can craft and throw this like every other weapon in this mod, but the net's actual
 * trap effect only triggers for the Arachne origin — enforced in {@link ThrownSilkNet}'s hit
 * handling, not here (see its class doc). This tooltip exists purely so a player can tell whose
 * weapon this is, same pattern every other origin-gated weapon uses via {@link OriginUtil}.
 */
public class SilkNetShooterItem extends Item {
	public SilkNetShooterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.5F,
				0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide) {
			ThrownSilkNet net = new ThrownSilkNet(level, player);
			net.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
			level.addFreshEntity(net);
			stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Traps on hit in a web and slows completely");
		OriginUtil.addOriginGatedTooltip(tooltip, "Arachne only");
	}
}
