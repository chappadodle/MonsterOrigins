package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.HarpyTalonGauntletItem;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * {@code Player.attack(Entity)} (decompiled directly to confirm, same verification habit already
 * used for {@code ThrownTridentMixin}) computes the melee damage float once, then calls
 * {@code entity.hurt(damageSource, f)} exactly one time for the primary hit (a later, separate
 * call inside the same method handles the sword sweep-attack mechanic against other nearby
 * entities, left untouched here via {@code ordinal = 0}). This mixin folds a Talon Gauntlet's
 * origin-gated bare-fist bonus into that single float before the hit lands, instead of trying to
 * add it afterward, since a same-tick second {@code hurt()} call gets silently ignored by
 * {@code LivingEntity}'s own invulnerability-window guard whenever the added amount is less than
 * the primary hit's damage (see {@link HarpyTalonGauntletItem}'s own class doc for the full
 * decompiled reasoning). Same pre-hit-injection shape {@code ThrownTridentMixin} already uses for
 * the Harpy Javelin's airborne throw bonus, just targeting the melee path instead of the thrown one.
 */
@Mixin(Player.class)
public abstract class HarpyTalonBonusDamageMixin {

	@ModifyArg(
		method = "attack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			ordinal = 0
		),
		index = 1
	)
	private float harpyTalon$applyBareFistBonus(float amount) {
		Player player = (Player) (Object) this;
		ItemStack mainhand = player.getMainHandItem();
		if (mainhand.getItem() instanceof HarpyTalonGauntletItem
				&& OriginUtil.hasOrigin(player, HarpyTalonGauntletItem.HARPY_ORIGIN_ID)) {
			return amount + HarpyTalonGauntletItem.BARE_FIST_BONUS_DAMAGE;
		}
		return amount;
	}
}
