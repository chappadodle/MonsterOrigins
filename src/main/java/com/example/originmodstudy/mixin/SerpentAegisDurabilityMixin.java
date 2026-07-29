package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.SerpentAegisItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Serpent Aegis never lost durability blocking a hit, reported as "indestructible" — traced
 * to vanilla's own real {@code Player.hurtCurrentlyUsedShield(float)} (decompiled to confirm):
 * {@code if (!this.useItem.is(Items.SHIELD)) return;} is, again, an exact identity check against
 * the single vanilla item, not {@code instanceof ShieldItem} — the same anti-pattern already found
 * twice this session (the 3D shield/trident rendering dispatch). {@code LivingEntity}'s own base
 * version of this method is a complete no-op ({@code protected void hurtCurrentlyUsedShield(float
 * f) {}}); all the real logic — the `f >= 3.0f` damage threshold, `hurtAndBreak`, the item-used
 * stat, and breaking the shield outright when its durability hits zero — lives only in
 * {@code Player}'s override, gated behind that one identity check. A HEAD {@code @Inject}
 * reproducing that same logic for {@code SerpentAegisItem} specifically composes safely with the
 * original method: vanilla's own check still runs afterward and no-ops for our item either way,
 * so nothing needs cancelling.
 *
 * <p><b>Ghast fireball immunity</b> needs the blocked hit's {@code DamageSource}, which {@code
 * hurtCurrentlyUsedShield} is never given (only the float amount) — captured instead via a HEAD
 * {@code @Inject} on {@code Player.hurt(DamageSource, float)} itself (confirmed via {@code javap}
 * that {@code Player} really does declare its own {@code hurt} override, not just inherit
 * {@code LivingEntity}'s), the same enclosing-method-capture-into-a-field technique
 * {@code SerpentAegisBlockMixin} uses for damage reflection. When the captured source is a real
 * Ghast fireball ({@code DamageTypes.FIREBALL}), the shield takes no durability loss at all — no
 * stat, no {@code hurtAndBreak}, nothing.
 */
@Mixin(Player.class)
public abstract class SerpentAegisDurabilityMixin {
	private DamageSource serpentAegis$lastDamageSource;

	// hurt(DamageSource, float) returns boolean, not void — see SerpentAegisBlockMixin's own
	// identical note; CallbackInfoReturnable is required here for the same reason.
	@Inject(method = "hurt", at = @At("HEAD"))
	private void serpentAegis$captureDamageSource(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
		this.serpentAegis$lastDamageSource = damageSource;
	}

	@Inject(method = "hurtCurrentlyUsedShield", at = @At("HEAD"))
	private void serpentAegis$hurtShield(float amount, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		ItemStack useItem = player.getUseItem();
		if (!(useItem.getItem() instanceof SerpentAegisItem)) {
			return;
		}
		DamageSource source = this.serpentAegis$lastDamageSource;
		if (source != null && source.is(DamageTypes.FIREBALL)) {
			return;
		}
		if (!player.level().isClientSide) {
			player.awardStat(Stats.ITEM_USED.get(useItem.getItem()));
		}
		if (amount >= 3.0F) {
			int durabilityLoss = 1 + Mth.floor(amount);
			InteractionHand hand = player.getUsedItemHand();
			useItem.hurtAndBreak(durabilityLoss, player, p -> p.broadcastBreakEvent(hand));
			if (useItem.isEmpty()) {
				if (hand == InteractionHand.MAIN_HAND) {
					player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				} else {
					player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
				}
				player.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + player.level().random.nextFloat() * 0.4F);
			}
		}
	}
}
