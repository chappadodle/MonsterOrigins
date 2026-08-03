package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.HarpyJavelinItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Playtest fix: bare-hand Talons never got the speed-scaled bonus damage {@code talons.json}'s own
 * description promises ("the faster she's flying... the more bonus damage") — only the Storm
 * Trident's melee/thrown hits did, via {@code HarpyJavelinSpeedDamageMixin}/{@code
 * ThrownTridentMixin}, both explicitly gated on holding that item. This is the bare-hand
 * counterpart: same {@code Player.attack}'s single {@code entity.hurt(...)} call, same
 * pre-hit {@code @ModifyArg} technique, same shared {@code HarpyJavelinItem.airborneBonusDamage
 * (LivingEntity)} formula — just gated on an empty mainhand instead of the Javelin item, so bare
 * fists and the weapon scale identically instead of only one of them actually doing so.
 */
@Mixin(Player.class)
public abstract class HarpyTalonSpeedDamageMixin {

	@ModifyArg(
		method = "attack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			ordinal = 0
		),
		index = 1
	)
	private float harpyTalon$speedDamageBonus(float amount) {
		Player player = (Player) (Object) this;
		if (player.getMainHandItem().isEmpty() && HarpyJavelinItem.isHarpyOrigin(player)) {
			return amount + HarpyJavelinItem.airborneBonusDamage(player);
		}
		return amount;
	}
}
