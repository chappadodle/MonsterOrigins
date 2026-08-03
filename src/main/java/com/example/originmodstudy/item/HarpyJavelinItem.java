package com.example.originmodstudy.item;

import com.example.originmodstudy.effect.ModEffects;
import com.example.originmodstudy.entity.ThrownJavelin;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A light, fast throwing spear. Anyone can craft and swing/throw it, but the Bleed-on-hit (both
 * melee and thrown) and the airborne throw bonus only apply for the Harpy origin — same
 * hit-time-gating pattern as FangItem/PetrifyingTridentItem, see OriginUtil for why.
 *
 * <p>Lighter/faster than the vanilla trident stats {@link TridentItem} would otherwise give it
 * (8.0 damage / -2.9 speed) — its own {@link ItemAttributeModifiers} data component is built via
 * {@code createHarpyAttributes()} and passed into {@code Properties#attributes(...)} before
 * construction (1.21.1's per-slot {@code getDefaultAttributeModifiers(EquipmentSlot)} override
 * point no longer exists), matching real vanilla {@code TridentItem.createAttributes()}'s own
 * pattern rather than a field subclasses adjust after the fact. Named {@code createHarpyAttributes}
 * rather than reusing {@code createAttributes} verbatim since {@code TridentItem} itself already
 * declares a same-signature {@code public static createAttributes()} — a subclass can't legally
 * redeclare that name with weaker (package-private/private) access, confirmed by a real javac
 * error when first tried.
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
 *
 * <p>The Storm Javelin lightning ability has no throw-time requirement at all anymore — earlier
 * versions gated it on the thrower's fall distance and/or absolute Y level at the moment of the
 * throw, both dropped per the user's own successive simplifications. {@code ThrownTridentMixin}
 * now triggers it on any thrown hit, purely gated by its own 30-second per-player cooldown. This
 * is unrelated to the airborne throw bonus above (which reads {@code isFallFlying()} at impact
 * time) — the two are kept deliberately separate, not merged into one condition.
 */
public class HarpyJavelinItem extends TridentItem {
	private static final ResourceLocation HARPY_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "harpy");

	/**
	 * Built once at construction time via {@code Properties#attributes(...)}, since 1.21.1's
	 * {@code Item#getDefaultAttributeModifiers(EquipmentSlot)} per-slot override no longer exists
	 * (confirmed via javap — the real current API is a single no-arg
	 * {@code Item#getDefaultAttributeModifiers()} returning this immutable data component). Mirrors
	 * real vanilla {@code TridentItem.createAttributes()}'s own static-factory pattern (this one is
	 * named {@code createHarpyAttributes} to avoid clashing with that existing same-signature
	 * public static method inherited from {@code TridentItem} — see class doc above).
	 */
	private static ItemAttributeModifiers createHarpyAttributes() {
		return ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.build();
	}

	public HarpyJavelinItem(Properties properties) {
		super(properties.attributes(createHarpyAttributes()));
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
		int riptide = EnchantmentHelper.getItemEnchantmentLevel(
				level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.RIPTIDE), stack);
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
				level.playSound(null, thrownJavelin, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
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
			level.playSound(null, player, soundEvent.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		}
	}

	/** Shared with ThrownTridentMixin so the thrown-hit path applies the exact same rule. */
	public static void applyBleed(LivingEntity target, LivingEntity attacker) {
		if (!target.getType().is(EntityTypeTags.UNDEAD) && OriginUtil.hasOrigin(attacker, HARPY_ORIGIN_ID)) {
			target.addEffect(new MobEffectInstance(ModEffects.BLEED, 200, 0));
		}
	}

	public static boolean isHarpyOrigin(LivingEntity entity) {
		return OriginUtil.hasOrigin(entity, HARPY_ORIGIN_ID);
	}

	/** Fixing-doc round 2: "way more damage based on how fast you're going while flying," base 7
	 * up to 14 — shared between the melee mixin ({@code HarpyJavelinSpeedDamageMixin}) and the
	 * thrown one ({@code ThrownTridentMixin}'s own airborne-bonus handler), replacing what used to
	 * be a flat +3 whenever simply airborne at all. Scales linearly from 0 (not moving/not flying)
	 * up to {@link #MAX_AIRBORNE_BONUS_DAMAGE} at {@link #MAX_BOOSTED_HORIZONTAL_SPEED} (the same
	 * reference ceiling {@code HarpyFlightSpeedMixin} clamps flight speed to, so "full bonus" lines
	 * up with "flying about as fast as this mod ever lets you go").
	 *
	 * <p>Both constants live here, on a plain non-mixin class, rather than on
	 * {@code HarpyFlightSpeedMixin} itself: an earlier attempt declared {@code
	 * MAX_BOOSTED_HORIZONTAL_SPEED} as a {@code public static} field directly inside that mixin
	 * class so this class could reference it — Sponge Mixin's pre-processor rejects any
	 * <em>non-private</em> static field declared in a mixin (it would have to attach a public
	 * static field onto {@code LivingEntity} itself), which crashed the whole mixin transform of
	 * {@code LivingEntity} at game launch (real crash, caught by the user's own playtest, not
	 * something this headless environment could have caught by reasoning alone). Fixed by moving
	 * the constant to this ordinary class instead; the mixin now reads it from here as a plain
	 * {@code private static final} copy, which Mixin has no objection to. */
	public static final float MAX_AIRBORNE_BONUS_DAMAGE = 7.0F;
	public static final double MAX_BOOSTED_HORIZONTAL_SPEED = 3.0;

	public static float airborneBonusDamage(LivingEntity owner) {
		if (!owner.isFallFlying()) {
			return 0.0F;
		}
		Vec3 velocity = owner.getDeltaMovement();
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double fraction = Math.min(horizontalSpeed / MAX_BOOSTED_HORIZONTAL_SPEED, 1.0);
		return (float) (fraction * MAX_AIRBORNE_BONUS_DAMAGE);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		OriginUtil.addOriginGatedTooltip(tooltip, "Causes Bleed; the faster you're flying, the more bonus damage (up to +7)");
		OriginUtil.addOriginGatedTooltip(tooltip, "A thrown hit calls down lightning (30 second cooldown)");
		OriginUtil.addOriginGatedTooltip(tooltip, "Harpy only");
	}
}
