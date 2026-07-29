package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.HarpyJavelinItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fixing-doc round 2: the Harpy Javelin's speed-scaled bonus damage while flying, for a
 * <em>melee</em> hit — same {@code Player.attack}'s single {@code entity.hurt(...)} call,
 * pre-hit {@code @ModifyArg} technique {@code MermaidTridentBonusDamageMixin} already uses and
 * documents in full. Unlike that mixin, this one needs no enclosing-method target capture (the
 * bonus only depends on the attacker's own flight speed, not anything about the target), so it's
 * a plain single-parameter {@code @ModifyArg} handler with no HEAD {@code @Inject} needed.
 *
 * <p>Previously there was no melee-while-flying bonus for the Javelin at all — only the thrown
 * case ({@code ThrownTridentMixin}) had a flat +3. This is the melee counterpart, sharing
 * {@code HarpyJavelinItem.airborneBonusDamage(LivingEntity)} with the thrown path so both apply
 * the exact same speed-scaled formula.
 */
@Mixin(Player.class)
public abstract class HarpyJavelinSpeedDamageMixin {

	@ModifyArg(
		method = "attack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			ordinal = 0
		),
		index = 1
	)
	private float harpyJavelin$speedDamageBonus(float amount) {
		Player player = (Player) (Object) this;
		ItemStack mainhand = player.getMainHandItem();
		if (mainhand.getItem() instanceof HarpyJavelinItem && HarpyJavelinItem.isHarpyOrigin(player)) {
			return amount + HarpyJavelinItem.airborneBonusDamage(player);
		}
		return amount;
	}
}
