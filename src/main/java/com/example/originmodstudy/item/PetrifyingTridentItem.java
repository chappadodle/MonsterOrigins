package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.entity.ThrownPetrifyingTrident;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
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
 * A reskinned trident, Medusa's weapon (Task 15) — this class itself only applies petrify on a
 * <em>thrown</em> hit, via {@link ModEffects#PETRIFY} + Blindness + Darkness (the same upgraded
 * effect stack Stone Gaze Burst uses), giving the trident a real ranged identity distinct from
 * her innate melee Petrifying Bite power. In practice, a melee swing petrifies too — not through
 * this class, but because Petrifying Bite ({@code petrifying_bite.json}) triggers on *any* hit
 * (unarmed or with this trident specifically), applying its own Slowness/Mining Fatigue/Blindness/
 * Darkness stack independently. The tooltip describes the combined, player-visible result ("hit,
 * thrown or melee"), not just this class's own direct effect.
 *
 * <p><b>Fixing-doc fix:</b> now overrides {@code releaseUsing} (a faithful reproduction of the
 * real decompiled {@code TridentItem.releaseUsing}, same technique as {@code HarpyJavelinItem}/
 * {@code MermaidTridentItem}) to spawn a dedicated {@link ThrownPetrifyingTrident} instead of
 * vanilla's hardcoded {@code ThrownTrident} — fixes it always rendering as a plain vanilla trident
 * in flight/stuck (vanilla's real thrown-trident renderer hardcodes its texture regardless of the
 * carried item, see CLAUDE.md), independent of the thrown-hit petrify logic below, which already
 * worked correctly either way since {@code ThrownTridentMixin} reads the real carried
 * {@code tridentItem} field directly.
 */
public class PetrifyingTridentItem extends TridentItem {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = new ResourceLocation("monster_origins", "medusa");

	public PetrifyingTridentItem(Properties properties) {
		super(properties);
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
				ThrownPetrifyingTrident thrownTrident = new ThrownPetrifyingTrident(level, player, stack);
				thrownTrident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F + riptide * 0.5F, 1.0F);
				if (player.getAbilities().instabuild) {
					thrownTrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
				}
				level.addFreshEntity(thrownTrident);
				level.playSound(null, thrownTrident, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
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
	public static void applyPetrify(LivingEntity target) {
		target.addEffect(new MobEffectInstance(ModEffects.PETRIFY, 60, 0));
		target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
		target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
	}

	public static boolean isMedusaOrigin(LivingEntity entity) {
		return OriginUtil.hasOrigin(entity, MEDUSA_ORIGIN_ID);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Petrifies on a hit, thrown or melee");
		OriginUtil.addOriginGatedTooltip(tooltip, "Medusa only");
	}
}
