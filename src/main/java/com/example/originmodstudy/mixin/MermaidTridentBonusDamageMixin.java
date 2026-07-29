package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.MermaidTridentItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Living Coral Trident's Barbed Tip trait (bonus damage against a swimming/floating target) for a
 * <em>melee</em> hit — same {@code Player.attack}'s single {@code entity.hurt(...)} call,
 * pre-hit {@code @ModifyArg} technique {@code HarpyTalonBonusDamageMixin} already uses and
 * documents in full (a same-tick second {@code hurt()} call gets silently dropped by the target's
 * invulnerability window, so the bonus has to be folded into the one real damage number before the
 * hit lands, not added afterward from {@code hurtEnemy}).
 *
 * <p><b>Crash fixed:</b> an earlier version of this class tried to read the melee target by
 * declaring it as a second, appended parameter on the {@code @ModifyArg} handler itself
 * (assuming the same enclosing-method parameter capture {@code @Inject} handlers support
 * elsewhere in this mod). That's not how {@code @ModifyArg} works — its handler parameters are
 * read as "which arguments of the *invoked* call to modify," not a local capture, so the extra
 * parameter made Mixin expect {@code Entity.hurt} to have the signature {@code (float, Entity)},
 * which doesn't match its real one and crashed the whole mixin transform of {@code Player} at game
 * launch. Fixed the same way {@code ThrownTridentMixin} now does: a plain {@code @Inject} at
 * {@code @At("HEAD")} captures {@code attack}'s own real {@code target} parameter (a genuinely
 * supported capture, since it's the enclosing method's own declared parameter, not a computed
 * local) into a field, and the {@code @ModifyArg} handler goes back to its original, valid
 * single-parameter shape and just reads that field.
 */
@Mixin(Player.class)
public abstract class MermaidTridentBonusDamageMixin {
	private Entity mermaidTrident$attackTarget;

	@Inject(method = "attack", at = @At("HEAD"))
	private void mermaidTrident$captureTarget(Entity target, CallbackInfo ci) {
		this.mermaidTrident$attackTarget = target;
	}

	@ModifyArg(
		method = "attack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
			ordinal = 0
		),
		index = 1
	)
	private float mermaidTrident$barbedTipBonus(float amount) {
		Player player = (Player) (Object) this;
		ItemStack mainhand = player.getMainHandItem();
		Entity target = this.mermaidTrident$attackTarget;
		if (mainhand.getItem() instanceof MermaidTridentItem
				&& MermaidTridentItem.isMermaidOrigin(player)
				&& target != null && target.isInWater()) {
			return amount + MermaidTridentItem.BARBED_TIP_BONUS_DAMAGE;
		}
		return amount;
	}
}
