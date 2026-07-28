package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.util.OriginUtil;
import com.example.originmodstudy.util.TieredHitTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A worn claw weapon, the upgrade path for Harpy's own bare-fist Talons power (see
 * {@code powers/harpy/talons.json}). Three tiers share this one class, same convention as
 * {@code FangItem}: {@code monster_origins:iron_talon_gauntlet} (Bleed always I),
 * {@code monster_origins:diamond_talon_gauntlet} (Bleed I on first hit, upgrades to Bleed II on a
 * second qualifying hit), and {@code monster_origins:netherite_talon_gauntlet} (Bleed escalates
 * I to II to III across three qualifying hits). Anyone can craft and swing any tier, but the Bleed
 * and the bare fist damage bonus below only trigger for the Harpy origin, same hit time gating
 * every other origin weapon in this mod uses, see {@link OriginUtil}.
 *
 * <p>Bleed tiering uses {@link TieredHitTracker}, matching the reset window Task 6 landed for
 * Arachne's own tiered poison (160 ticks, 8 seconds) for consistency between the two tiered on hit
 * systems in this mod.
 *
 * <p><b>Stacking with the bare fist bonus (resolved decision #6):</b> {@code talons.json}'s own
 * {@code +4.0} flat damage bonus (re-read fresh from that file, not assumed, since it could have
 * drifted since it was written) is gated on an empty mainhand, so it never fires while a Gauntlet
 * is equipped. That exact {@code +4.0} is re-applied for a Harpy wielder by
 * {@code HarpyTalonBonusDamageMixin}, an {@code @ModifyArg} on {@code Player.attack}'s own single
 * {@code entity.hurt(...)} call -- the same pre-hit-injection technique {@code ThrownTridentMixin}
 * already uses for the Harpy Javelin's airborne throw bonus -- so it is folded into the one real
 * damage number before the hit lands, rather than trying to re-inflict it as a second
 * {@code target.hurt(...)} call from {@link #hurtEnemy} afterward. A second call was the first
 * approach tried, and it does not work: {@code LivingEntity#hurt} (decompiled directly to check,
 * the same verification habit this project already uses for every other on-hit assumption) sets
 * {@code invulnerableTime = 20} and {@code lastHurt = <primary hit's damage>} the moment the
 * primary swing lands, and {@code hurtEnemy} runs synchronously right after that, same tick, same
 * call stack. A same-tick repeat {@code hurt()} call is silently dropped whenever its amount is
 * less than or equal to {@code lastHurt} (no vanilla damage type used here carries the
 * {@code bypasses_cooldown} tag that would skip this check) -- and {@code 4.0} is always less than
 * any of this weapon's own primary hits, so the "obvious" implementation would have compiled,
 * looked correct, and then silently never applied in game. Deliberately, this class does
 * <em>not</em> also override {@code getDefaultAttributeModifiers} to bake the {@code +4.0} into
 * the item's own static {@code ATTACK_DAMAGE} modifier: that field has no wielder context, so it
 * would apply the bonus to literally anyone holding the Gauntlet, breaking the origin gate this
 * bonus is supposed to have, on top of double counting it for an actual Harpy wielder once the
 * mixin below also adds it. The item's own base damage is left as plain, unconditional
 * {@code SwordItem} tier math (2/3/4 hearts via {@code attackDamageModifier} + the tier's own
 * bonus, same as every other tier in this weapon family and matching {@code FangItem}'s own
 * pattern) -- only the mixin's contribution is origin gated.
 */
public class HarpyTalonGauntletItem extends SwordItem {
	/** Public (unlike FangItem's private ARACHNE_ORIGIN_ID) so {@code HarpyTalonBonusDamageMixin},
	 * in the sibling {@code mixin} package, can gate the melee damage bonus on the same origin. */
	public static final ResourceLocation HARPY_ORIGIN_ID = new ResourceLocation("monster_origins", "harpy");

	/** {@code powers/harpy/talons.json}'s own current bare fist bonus damage value, re-applied
	 * here for a Gauntlet wielder since that power's own condition (empty mainhand) is false while
	 * one is equipped. Public so {@code HarpyTalonBonusDamageMixin} can reference it. */
	public static final float BARE_FIST_BONUS_DAMAGE = 4.0F;

	/** Shared "repeat hit" tracker key for all three Gauntlet tiers, matching FangItem's own
	 * per-family shared key convention. */
	private static final String BLEED_TRACKER_KEY = "harpy_talon_bleed";

	// 8 seconds, matching Task 6's Arachne poison reset window exactly (see FangItem's own
	// POISON_RESET_TICKS) for consistency between this mod's two tiered on-hit effect systems.
	private static final int BLEED_RESET_TICKS = 160;

	// talons.json's own rake sub-power applies Bleed for 200 ticks (10 seconds); reused here so a
	// Gauntlet's Bleed lasts exactly as long as the bare-fist version does.
	private static final int BLEED_DURATION_TICKS = 200;

	private final int maxBleedTier;

	public HarpyTalonGauntletItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, int maxBleedTier,
			Properties properties) {
		super(tier, attackDamageModifier, attackSpeedModifier, properties);
		this.maxBleedTier = maxBleedTier;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		boolean result = super.hurtEnemy(stack, target, attacker);
		if (target.getMobType() != MobType.UNDEAD && OriginUtil.hasOrigin(attacker, HARPY_ORIGIN_ID)) {
			int tier = TieredHitTracker.nextTier(target, attacker, BLEED_TRACKER_KEY, maxBleedTier, BLEED_RESET_TICKS);
			target.addEffect(new MobEffectInstance(ModEffects.BLEED, BLEED_DURATION_TICKS, tier - 1));
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Adds the bare fist bonus damage and causes Bleed");
		OriginUtil.addOriginGatedTooltip(tooltip, "Harpy only");
	}
}
