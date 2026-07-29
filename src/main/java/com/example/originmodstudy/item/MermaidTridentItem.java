package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.entity.ThrownMermaidTrident;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
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
 * The Living Coral Trident (Task 13): a late-game Mermaid weapon, plain vanilla trident stats
 * (no {@code getDefaultAttributeModifiers} override, unlike {@code HarpyJavelinItem} — the source
 * doc never asked for different damage/speed here), 3500 durability. Anyone can craft/throw it,
 * but its three on-hit traits below only trigger for the Mermaid origin, same hit-time gating
 * every other origin weapon in this mod uses (see {@link OriginUtil}):
 *
 * <ul>
 *   <li><b>Symbiosis</b> — restores a little of the wielder's hunger on every hit that lands.</li>
 *   <li><b>Bleeding Current</b> — applies Bleed if the wielder is in water at the moment of the
 *       hit (melee or thrown).</li>
 * </ul>
 *
 * <p>The fourth trait, <b>Barbed Tip</b> (bonus damage vs. a swimming/floating target), is a melee
 * damage number and can't be applied from {@code hurtEnemy} for the same reason documented on
 * {@code HarpyTalonGauntletItem}: a same-tick second {@code hurt()} call is silently dropped by the
 * target's own invulnerability window. It lives in {@code MermaidTridentBonusDamageMixin} (melee)
 * and {@code ThrownTridentMixin} (thrown) instead, both pre-hit {@code @ModifyArg} injections,
 * mirroring {@code HarpyTalonBonusDamageMixin}'s already-proven technique exactly.
 *
 * <p>{@code releaseUsing} is overridden the same way {@code HarpyJavelinItem} overrides it — a
 * faithful reproduction of the real decompiled {@code TridentItem.releaseUsing}, with only the
 * thrown-entity construction swapped to {@link ThrownMermaidTrident} so it renders with its own
 * item model and leaves a bubble trail through water (see that class) instead of vanilla's plain
 * {@code ThrownTrident}.
 *
 * <p>The requested "+1 block reach" lives entirely in {@code MermaidTridentReachMixin}, a
 * client-only mixin on {@code MultiPlayerGameMode.getPickRange()} — see that class doc for why a
 * mixin was used instead of the third-party Reach Entity Attributes dependency Origins' own
 * {@code extra_reach.json} power relies on.
 */
public class MermaidTridentItem extends TridentItem {
	private static final ResourceLocation MERMAID_ORIGIN_ID = new ResourceLocation("monster_origins", "mermaid");

	public MermaidTridentItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (OriginUtil.hasOrigin(attacker, MERMAID_ORIGIN_ID)) {
			applySymbiosis(attacker);
			applyBleedingCurrent(target, attacker);
		}
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
				ThrownMermaidTrident thrownTrident = new ThrownMermaidTrident(level, player, stack);
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

	/** Shared with ThrownTridentMixin so the thrown-hit path applies the exact same rules. */
	public static void applySymbiosis(LivingEntity attacker) {
		if (attacker instanceof Player player) {
			player.getFoodData().eat(1, 0.2F);
		}
	}

	public static void applyBleedingCurrent(LivingEntity target, LivingEntity attacker) {
		if (attacker.isInWater()) {
			target.addEffect(new MobEffectInstance(ModEffects.BLEED, 100, 0));
		}
	}

	public static boolean isMermaidOrigin(LivingEntity entity) {
		return OriginUtil.hasOrigin(entity, MERMAID_ORIGIN_ID);
	}

	/** Barbed Tip's bonus damage value, shared between the melee mixin and the thrown-hit mixin
	 * so the two paths stay in sync. Public for the same reason
	 * {@code HarpyTalonGauntletItem.BARE_FIST_BONUS_DAMAGE} is public. */
	public static final float BARBED_TIP_BONUS_DAMAGE = 3.0F;

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Feeds on the target, restoring your hunger");
		OriginUtil.addOriginGatedTooltip(tooltip, "Bonus damage against swimming or floating targets");
		OriginUtil.addOriginGatedTooltip(tooltip, "Causes Bleed while you're in water");
		OriginUtil.addOriginGatedTooltip(tooltip, "+1 block reach. Mermaid only");
	}
}
