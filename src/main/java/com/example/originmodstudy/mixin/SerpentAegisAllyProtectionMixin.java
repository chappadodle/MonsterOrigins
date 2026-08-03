package com.example.originmodstudy.mixin;

import com.example.originmodstudy.item.SerpentAegisItem;
import com.example.originmodstudy.util.OriginUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * "A shield that defends more people" — the user's own request, followed by a real check of
 * whether vanilla shields have any kind of spatial hitbox that could just be resized (they don't:
 * {@code isDamageSourceBlocked} is a pure directional dot-product test between the defender's own
 * view vector and the damage source's position, evaluated per-defender, confirmed via decompile —
 * there is no spatial volume anywhere in the mechanic). Built as a genuinely new AOE mechanic
 * instead, per the user's own choice between the options presented: while a Medusa-origin player
 * is actively blocking with the Serpent Aegis, any *other* player within {@link
 * #PROTECTION_RADIUS} takes {@link #DAMAGE_REDUCTION_FRACTION} less damage from anything, with no
 * facing/direction requirement (unlike real shield-blocking) — simpler and covers a group fight
 * regardless of which way Medusa happens to be facing relative to each ally's own attacker.
 *
 * <p>Deliberately scoped to other {@code Player}s only, not every nearby {@code LivingEntity} —
 * "protect more people" reads as protecting allies specifically, and blanket-including any nearby
 * mob risks accidentally softening hits against hostile mobs standing near Medusa too (e.g. one
 * cornered by other players). Medusa herself is unaffected by her own aura (excluded from the
 * nearby-search) — she already gets real shield blocking when facing the right way.
 *
 * <p>Uses {@code @ModifyVariable} on {@code hurt(DamageSource, float)}'s own {@code float}
 * parameter, matched purely by type (single-parameter form, the simplest and safest use of that
 * annotation — see {@code TridentStyleFlatIconMixin}'s own doc for why this project prefers that
 * over declaring extra captured parameters).
 */
@Mixin(LivingEntity.class)
public abstract class SerpentAegisAllyProtectionMixin {
	private static final ResourceLocation MEDUSA_ORIGIN_ID = ResourceLocation.fromNamespaceAndPath("monster_origins", "medusa");
	private static final double PROTECTION_RADIUS = 6.0;
	private static final float DAMAGE_REDUCTION_FRACTION = 0.5F;

	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float serpentAegis$reduceNearbyAllyDamage(float amount) {
		LivingEntity victim = (LivingEntity) (Object) this;
		if (!(victim instanceof Player) || victim.level().isClientSide) {
			return amount;
		}
		AABB area = victim.getBoundingBox().inflate(PROTECTION_RADIUS);
		List<Player> nearbyGuardians = victim.level().getEntitiesOfClass(Player.class, area, nearby ->
				nearby != victim
						&& nearby.isBlocking()
						&& nearby.getUseItem().getItem() instanceof SerpentAegisItem
						&& OriginUtil.hasOrigin(nearby, MEDUSA_ORIGIN_ID)
		);
		if (nearbyGuardians.isEmpty()) {
			return amount;
		}
		return amount * (1.0F - DAMAGE_REDUCTION_FRACTION);
	}
}
