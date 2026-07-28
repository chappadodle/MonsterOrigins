package com.example.originmodstudy.entity;

import com.example.originmodstudy.item.ModItems;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;

/**
 * The Silk Net Shooter's projectile. Extends {@link ThrowableItemProjectile} directly (the same
 * base class vanilla's {@code Snowball}/{@code Egg} use) rather than {@code ThrownTrident} — there
 * is no melee/riptide/loyalty behavior to inherit here, only "fly in a line, do something on
 * entity hit."
 *
 * <p>Unlike {@code ThrownJavelin} (which had to work around {@code ThrownTrident}'s private,
 * never-synced {@code tridentItem} field — see CLAUDE.md), {@code ThrowableItemProjectile} already
 * has a real synced {@code EntityDataAccessor<ItemStack>} and a working {@code getItem()} that
 * falls back to {@code new ItemStack(getDefaultItem())} whenever the synced stack is empty
 * (confirmed by decompiling the class directly). Since this projectile only ever renders as one
 * specific item, overriding {@link #getDefaultItem()} is sufficient on its own — {@code setItem()}
 * is never called, so the synced field simply stays at its empty default and {@code getItem()}'s
 * own fallback does the rest. This reproduces none of the Harpy Javelin's original bug, without
 * needing to hand-roll a second copy of the same synced-data mechanism the superclass already
 * provides.
 *
 * <p>On hitting an entity, places a temporary cobweb at the target's feet and applies a short,
 * heavy Slowness as redundancy in case the cobweb placement is denied (e.g. the space isn't
 * replaceable). See {@code SilkNetShooterItem} for the crafting/use side and the task report for
 * the verification that {@code origins:temporary_cobweb}'s auto-revert is a property of the block
 * itself, not something that needs to be reproduced here.
 *
 * <p>Anyone can craft and throw the Silk Net Shooter, but same as every other origin-gated
 * weapon in this mod (Fang tiers, Petrifying Trident, Harpy Javelin — see {@code OriginUtil}), the
 * actual on-hit effect only triggers if the thrower has the Arachne origin. This is checked here
 * against the projectile's own owner (the thrower), not in the item's {@code use()} — same
 * hit-time-gating placement every other origin-gated weapon in this project uses. A non-Arachne
 * thrower's net still flies and hits normally, it just does nothing on impact.
 */
public class ThrownSilkNet extends ThrowableItemProjectile {
	/** 3 seconds, matching the temporary cobweb block's own real scheduled-tick revert delay. */
	private static final int EFFECT_DURATION_TICKS = 60;

	/**
	 * Amplifier 6 (displayed as Slowness VII) is the lowest amplifier that actually reaches 0
	 * movement speed: Slowness's real attribute modifier is -0.15 MULTIPLY_TOTAL per level
	 * (confirmed by decompiling {@code MobEffects}/{@code MobEffect#getAttributeModifierValue},
	 * which scales the base -0.15 by {@code amplifier + 1}), and {@code generic.movement_speed}'s
	 * real range floor is 0.0 (confirmed via {@code Attributes.MOVEMENT_SPEED}'s
	 * {@code RangedAttribute} bounds), so any amplifier whose combined multiplier would go negative
	 * is simply clamped to a full stop rather than a display oddity like moving backward. At
	 * amplifier 6, the multiplier is {@code 1 + (-0.15 * 7) = -0.05}, already past zero, so this is
	 * the first amplifier that fully immobilizes.
	 */
	private static final int SLOWNESS_AMPLIFIER = 6;

	private static final ResourceLocation TEMPORARY_COBWEB_ID = new ResourceLocation("origins", "temporary_cobweb");
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("monster_origins", "arachne");

	public ThrownSilkNet(EntityType<? extends ThrownSilkNet> entityType, Level level) {
		super(entityType, level);
	}

	public ThrownSilkNet(Level level, LivingEntity owner) {
		super(ModEntities.THROWN_SILK_NET, owner, level);
	}

	/**
	 * In-flight render item only ({@code ThrownItemRenderer} draws {@code new
	 * ItemStack(getDefaultItem())}, see {@code ThrownSilkNetRenderer} and the class doc above) —
	 * deliberately vanilla {@code Items.COBWEB}, not {@link ModItems#SILK_NET_SHOOTER}. Returning
	 * the shooter item here made the net's flight visual look like the launcher itself was flying
	 * at the target; a flying cobweb reads correctly as a net/web in the air and also matches this
	 * weapon's own on-impact effect (a temporary cobweb at the target's feet), so the projectile
	 * and its payload are visually consistent. No new texture/model needed since it's a real
	 * vanilla item with its own existing icon.
	 */
	@Override
	protected Item getDefaultItem() {
		return Items.COBWEB;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level().isClientSide) {
			return;
		}
		Entity owner = this.getOwner();
		if (!(owner instanceof LivingEntity livingOwner) || !OriginUtil.hasOrigin(livingOwner, ARACHNE_ORIGIN_ID)) {
			// Anyone can throw the net, but the cobweb/Slowness payload is Arachne's own weapon
			// effect, same as every other origin-gated weapon in this mod. A non-Arachne thrower's
			// net still flies and connects, it just does nothing further on impact.
			return;
		}
		Entity target = result.getEntity();
		BlockPos pos = target.blockPosition();
		BlockState currentState = this.level().getBlockState(pos);
		// Mirrors master_of_webs.json's own origins:replacable check before origins:set_block, so
		// this doesn't overwrite solid terrain the target happens to be standing inside.
		if (currentState.canBeReplaced()) {
			Block temporaryCobweb = BuiltInRegistries.BLOCK.get(TEMPORARY_COBWEB_ID);
			// A plain Level#setBlock call here gets the same auto-revert the origins:set_block
			// action gets: TemporaryCobwebBlock.onPlace schedules its own 60-tick self-removal tick,
			// so no custom revert-scheduling logic is needed on this end (verified by decompiling
			// TemporaryCobwebBlock directly, see the task report).
			this.level().setBlock(pos, temporaryCobweb.defaultBlockState(), 3);
		}
		if (target instanceof LivingEntity livingTarget) {
			// Belt-and-suspenders redundancy alongside the cobweb block, in case placement above was
			// skipped (e.g. currentState wasn't replaceable).
			livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION_TICKS, SLOWNESS_AMPLIFIER));
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!this.level().isClientSide) {
			this.discard();
		}
	}
}
