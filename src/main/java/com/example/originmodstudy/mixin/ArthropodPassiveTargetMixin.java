package com.example.originmodstudy.mixin;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Requirement #8: arthropods (spider, cave spider, silverfish, endermite) are passive toward an
 * Arachne player unless that specific mob has been hit by them. There is no data-driven Origins
 * power for suppressing mob AI targeting (see apace100/origins-fabric#144, still open), so this
 * is the addon's one piece of custom code.
 *
 * {@link TargetGoal#canAttack} is the single choke point both {@code NearestAttackableTargetGoal}
 * (proactive hostility) and other targeting goals call before accepting a target, so cancelling it
 * here blocks acquisition without touching per-mob AI classes individually. Retaliation needs no
 * extra state: vanilla already records {@link LivingEntity#getLastHurtByMob()} whenever a mob takes
 * damage from an attacker, so allowing the check through when that field is the Arachne player who
 * just hit it reproduces "friendly until attacked" for free.
 */
@Mixin(TargetGoal.class)
public abstract class ArthropodPassiveTargetMixin {
	private static final ResourceLocation FRIENDLY_ARTHROPODS_TAG_ID =
		new ResourceLocation("monster_origins", "friendly_arthropods");
	private static final TagKey<EntityType<?>> FRIENDLY_ARTHROPODS =
		TagKey.create(Registries.ENTITY_TYPE, FRIENDLY_ARTHROPODS_TAG_ID);
	private static final ResourceLocation ORIGIN_LAYER_ID = new ResourceLocation("origins", "origin");
	private static final ResourceLocation ARACHNE_ORIGIN_ID = new ResourceLocation("monster_origins", "arachne");

	@Shadow
	@Final
	protected Mob mob;

	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void arachne$suppressFriendlyArthropodTargeting(
		LivingEntity potentialTarget, TargetingConditions targetConditions, CallbackInfoReturnable<Boolean> cir
	) {
		if (!(potentialTarget instanceof Player)) {
			return;
		}
		Player player = (Player) potentialTarget;
		if (!this.mob.getType().is(FRIENDLY_ARTHROPODS)) {
			return;
		}
		if (this.mob.getLastHurtByMob() == player) {
			// This specific mob was just hit by this specific player — let it retaliate.
			return;
		}

		ComponentKey<OriginComponent> originKey = ModComponents.ORIGIN;
		OriginComponent originComponent = originKey.get(player);
		OriginLayer layer = OriginLayers.getLayer(ORIGIN_LAYER_ID);
		if (layer == null || !originComponent.hasOrigin(layer)) {
			return;
		}
		Origin origin = originComponent.getOrigin(layer);
		if (origin.getIdentifier().equals(ARACHNE_ORIGIN_ID)) {
			cir.setReturnValue(false);
		}
	}
}
