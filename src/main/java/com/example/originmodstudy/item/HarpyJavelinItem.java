package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.entity.ThrownJavelin;
import com.example.originmodstudy.util.OriginUtil;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A light, fast throwing spear. Anyone can craft and swing/throw it, but the Bleed-on-hit (both
 * melee and thrown) and the airborne throw bonus only apply for the Harpy origin — same
 * hit-time-gating pattern as FangItem/PetrifyingTridentItem, see OriginUtil for why.
 *
 * <p>Lighter/faster than the vanilla trident stats {@link TridentItem} would otherwise give it
 * (8.0 damage / -2.9 speed) — {@code getDefaultAttributeModifiers} is overridden with its own
 * Multimap since TridentItem builds its modifiers in its constructor from hardcoded constants,
 * not a field subclasses can adjust.
 *
 * <p>The airborne-throw bonus and thrown-hit Bleed live in {@code ThrownTridentMixin}, not here:
 * vanilla's {@code ThrownTrident.onHitEntity} deals its own damage directly (confirmed by
 * decompiling the real class), entirely bypassing {@code hurtEnemy} — that method is melee-only.
 *
 * <p>{@code releaseUsing} is overridden to spawn a {@link ThrownJavelin} (so it renders using its
 * own item model in flight) instead of vanilla's hardcoded {@code new ThrownTrident(...)} — the
 * body below is a faithful line-for-line reproduction of the real decompiled
 * {@code TridentItem.releaseUsing}, with only that one construction swapped out. This duplicates
 * vanilla logic rather than calling {@code super.releaseUsing}, since that method has no
 * extension point for a different thrown-entity type; if vanilla's trident-throwing logic ever
 * changes, this will silently drift out of sync with it, same known risk already documented for
 * the {@code origins:master_of_webs} override in CLAUDE.md.
 */
public class HarpyJavelinItem extends TridentItem {
	private static final ResourceLocation HARPY_ORIGIN_ID = new ResourceLocation("arachne", "harpy");
	private final Multimap<Attribute, AttributeModifier> defaultModifiers;

	public HarpyJavelinItem(Properties properties) {
		super(properties);
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
				BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 6.0, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
				BASE_ATTACK_SPEED_UUID, "Tool modifier", -2.4, AttributeModifier.Operation.ADDITION));
		this.defaultModifiers = builder.build();
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		if (slot == EquipmentSlot.MAINHAND) {
			return this.defaultModifiers;
		}
		return super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		applyBleed(target, attacker);
		return result;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int chargeTicksRemaining) {
		if (!(livingEntity instanceof Player player)) {
			return;
		}
		int chargeTicks = this.getUseDuration(stack) - chargeTicksRemaining;
		if (chargeTicks < 10) {
			return;
		}
		int riptide = EnchantmentHelper.getRiptide(stack);
		if (riptide > 0 && !player.isInWaterOrRain()) {
			return;
		}
		if (!level.isClientSide) {
			stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(livingEntity.getUsedItemHand()));
			if (riptide == 0) {
				ThrownJavelin thrownJavelin = new ThrownJavelin(level, player, stack);
				thrownJavelin.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F + riptide * 0.5F, 1.0F);
				if (player.getAbilities().instabuild) {
					thrownJavelin.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
				}
				level.addFreshEntity(thrownJavelin);
				level.playSound(null, thrownJavelin, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
				if (!player.getAbilities().instabuild) {
					player.getInventory().removeItem(stack);
				}
			}
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		if (riptide > 0) {
			float yRot = player.getYRot();
			float xRot = player.getXRot();
			float h = -Mth.sin(yRot * ((float) Math.PI / 180)) * Mth.cos(xRot * ((float) Math.PI / 180));
			float l = -Mth.sin(xRot * ((float) Math.PI / 180));
			float m = Mth.cos(yRot * ((float) Math.PI / 180)) * Mth.cos(xRot * ((float) Math.PI / 180));
			float n = Mth.sqrt(h * h + l * l + m * m);
			float o = 3.0F * ((1.0F + riptide) / 4.0F);
			h *= o / n;
			l *= o / n;
			m *= o / n;
			player.push(h, l, m);
			player.startAutoSpinAttack(20);
			if (player.onGround()) {
				player.move(MoverType.SELF, new Vec3(0.0, 1.1999999284744263, 0.0));
			}
			var soundEvent = riptide >= 3 ? SoundEvents.TRIDENT_RIPTIDE_3 : (riptide == 2 ? SoundEvents.TRIDENT_RIPTIDE_2 : SoundEvents.TRIDENT_RIPTIDE_1);
			level.playSound(null, player, soundEvent, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	/** Shared with ThrownTridentMixin so the thrown-hit path applies the exact same rule. */
	public static void applyBleed(LivingEntity target, LivingEntity attacker) {
		if (target.getMobType() != MobType.UNDEAD && OriginUtil.hasOrigin(attacker, HARPY_ORIGIN_ID)) {
			target.addEffect(new MobEffectInstance(ModEffects.BLEED, 200, 0));
		}
	}

	public static boolean isHarpyOrigin(LivingEntity entity) {
		return OriginUtil.hasOrigin(entity, HARPY_ORIGIN_ID);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Causes Bleed, bonus damage thrown while flying");
		OriginUtil.addOriginGatedTooltip(tooltip, "Harpy only");
	}
}
