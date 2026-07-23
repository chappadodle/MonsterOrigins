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
 * Siren requirement: sea creatures (including Drowned and Guardian) are friendly toward her
 * unless attacked — same technique and same retaliation exception as
 * {@link ArthropodPassiveTargetMixin}, just a different tag/origin pair. Kept as a separate
 * mixin rather than generalizing the two, matching this project's one-mixin-one-purpose
 * convention.
 *
 * <p>Guardian specifically was verified (decompiled {@code Guardian.registerGoals()}) to acquire
 * its target via a real {@code NearestAttackableTargetGoal} — a {@code TargetGoal} subclass — at
 * {@code targetSelector} priority 1, before its laser-beam {@code GuardianAttackGoal} ever runs.
 * Suppressing acquisition here fully prevents the laser attack too, not just melee.
 */
@Mixin(TargetGoal.class)
public abstract class SeaCreaturePassiveTargetMixin {
	private static final ResourceLocation FRIENDLY_SEA_CREATURES_TAG_ID =
		new ResourceLocation("arachne", "friendly_sea_creatures");
	private static final TagKey<EntityType<?>> FRIENDLY_SEA_CREATURES =
		TagKey.create(Registries.ENTITY_TYPE, FRIENDLY_SEA_CREATURES_TAG_ID);
	private static final ResourceLocation ORIGIN_LAYER_ID = new ResourceLocation("origins", "origin");
	private static final ResourceLocation SIREN_ORIGIN_ID = new ResourceLocation("arachne", "siren");

	@Shadow
	@Final
	protected Mob mob;

	@Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
	private void siren$suppressFriendlySeaCreatureTargeting(
		LivingEntity potentialTarget, TargetingConditions targetConditions, CallbackInfoReturnable<Boolean> cir
	) {
		if (!(potentialTarget instanceof Player)) {
			return;
		}
		Player player = (Player) potentialTarget;
		if (!this.mob.getType().is(FRIENDLY_SEA_CREATURES)) {
			return;
		}
		if (this.mob.getLastHurtByMob() == player) {
			return;
		}

		ComponentKey<OriginComponent> originKey = ModComponents.ORIGIN;
		OriginComponent originComponent = originKey.get(player);
		OriginLayer layer = OriginLayers.getLayer(ORIGIN_LAYER_ID);
		if (layer == null || !originComponent.hasOrigin(layer)) {
			return;
		}
		Origin origin = originComponent.getOrigin(layer);
		if (origin.getIdentifier().equals(SIREN_ORIGIN_ID)) {
			cir.setReturnValue(false);
		}
	}
}
