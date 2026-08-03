package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.SerpentAegisItem;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Medusa's Serpent Aegis (Task 19, extended this round): blocking a melee attack with it inflicts
 * Slowness IV on the attacker and reflects half the blocked damage back, thorns-enchantment style.
 *
 * <p><b>Corrected this round: {@code blockedByShield}'s {@code this}/parameter roles are the
 * opposite of what this mixin originally assumed.</b> Fresh decompile of the real call chain
 * (not trusted from memory this time, since the reflection feature's total failure demanded
 * re-checking): {@code LivingEntity.hurt(...)} calls {@code this.blockUsingShield(attacker)}
 * (defender calling it, attacker as the argument); {@code blockUsingShield(LivingEntity
 * livingEntity)} — {@code this} = defender here — calls {@code livingEntity.blockedByShield(
 * this)}, i.e. {@code attacker.blockedByShield(defender)}. So inside {@code blockedByShield},
 * {@code this} is the ATTACKER and the method parameter is the DEFENDER — confirmed against
 * vanilla's own default body, {@code livingEntity.knockback(0.5, livingEntity.getX() -
 * this.getX(), ...)}, which knocks back the *parameter* using a vector pointing away from
 * {@code this} — matching the well-known real vanilla behavior that blocking a heavy hit pushes
 * the *blocker* backward, not the attacker. The original version of this mixin read {@code this}
 * as the defender and the parameter as the attacker — exactly backwards — so it only ever
 * applied Slowness/reflection when the roles happened to coincidentally align (e.g. two
 * Aegis-wielding Medusa players fighting each other), never in the intended "Medusa blocks,
 * attacker gets punished" case. Fixed by swapping which variable checks for the Aegis/Medusa
 * origin and which one receives the Slowness/reflected damage.
 *
 * <p>Only ever fires for a <em>melee</em> hit — {@code blockedByShield} isn't part of the
 * projectile-blocking path at all (that's handled separately, entirely inside vanilla's own
 * shield logic, untouched here), matching the source doc's "no effect vs projectiles" without
 * needing a separate check.
 *
 * <p><b>Damage reflection needs the raw blocked damage amount, which {@code blockedByShield}
 * itself is never given</b> — captured instead via a HEAD {@code @Inject} on {@code LivingEntity
 * .hurt(DamageSource, float)}, the real vanilla method that goes on to call {@code
 * blockUsingShield}/{@code blockedByShield} in the first place. Since {@code hurt()} runs on the
 * DEFENDER (confirmed: it's the entity taking damage), and {@code blockedByShield} runs on the
 * ATTACKER, the captured field has to be read off the *parameter* (the defender) inside {@code
 * blockedByShield}, not off {@code this} — a second consequence of the same this/parameter swap
 * this whole fix is about.
 */
@Mixin(LivingEntity.class)
public abstract class SerpentAegisBlockMixin {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "medusa");
	// Half the blocked damage reflected back — a real, meaningful bite (not just a token amount)
	// without literally negating the shield's own point (100% reflection would mean nothing was
	// truly "blocked" at all, damage-wise, just redirected).
	private static final float REFLECTED_DAMAGE_FRACTION = 0.5F;

	private float serpentAegis$lastIncomingDamage;

	// hurt(DamageSource, float) returns boolean, not void — a HEAD @Inject into a non-void method
	// needs CallbackInfoReturnable, not plain CallbackInfo, even when the handler never touches
	// it. Missing this crashed the whole mixin apply pass at launch ("CallbackInfoReturnable is
	// required!"), caught by the user's own real playtest, not by anything checked here first.
	@Inject(method = "hurt", at = @At("HEAD"))
	private void serpentAegis$captureIncomingDamage(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
		this.serpentAegis$lastIncomingDamage = amount;
	}

	@Inject(method = "blockedByShield", at = @At("TAIL"))
	private void serpentAegis$slowAndReflect(LivingEntity defender, CallbackInfo ci) {
		// "this" is the ATTACKER here, not the defender — see the class doc above.
		LivingEntity attacker = (LivingEntity) (Object) this;
		ItemStack blockingItem = defender.getUseItem();
		if (!(blockingItem.getItem() instanceof SerpentAegisItem)) {
			return;
		}
		if (!OriginUtil.hasOrigin(defender, MEDUSA_ORIGIN_ID)) {
			return;
		}
		attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
		// Casting to this mixin's own type (not just LivingEntity) to reach the injected field on
		// a *different* instance — plain LivingEntity has no such member as far as javac is
		// concerned; only this mixin class itself declares it.
		float reflected = ((SerpentAegisBlockMixin) (Object) defender).serpentAegis$lastIncomingDamage * REFLECTED_DAMAGE_FRACTION;
		if (reflected > 0.0F) {
			attacker.hurt(defender.damageSources().thorns(defender), reflected);
		}
	}
}
