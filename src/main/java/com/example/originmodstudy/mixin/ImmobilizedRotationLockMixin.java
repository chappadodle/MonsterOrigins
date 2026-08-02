package com.example.originmodstudy.mixin;

import com.example.originmodstudy.effect.ModEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Playtest fix (2026-08-02): while {@link ModEffects#IMMOBILIZED} is present, freeze the entity's
 * rotation in place — no turning to face/attack — on top of whatever movement lock the effect that
 * applied it already provides. Confirmed via a project-wide grep before writing this that nothing
 * in this codebase touched rotation at all until now, so the four (now five, per a playtest
 * follow-up decision) "trap the enemy" sources — Arachne's web bite, the Silk Net Shooter, Medusa's
 * petrify gaze, Medusa's petrifying bite, and the Petrifying Trident — only ever reduced movement
 * speed, leaving a supposedly helpless target free to turn and strike back.
 *
 * <p>Targets {@link LivingEntity}, not {@code Mob}, since several of these sources can land on a
 * player (no entity-type restriction in their targeting conditions). Injects at {@code TAIL} of
 * {@code tick()} — same injection point and reasoning already established by
 * {@code ArachnePoisonLockMixin}: this runs once per entity per tick, after all AI/movement/
 * rotation logic for that tick has already executed, so nothing later in the same tick can undo the
 * override.
 *
 * <p>Rotation is snapshotted once, the first tick the effect is seen, via a non-persistent Fabric
 * attachment (same side-channel-state mechanism {@code ArachnePoisonLock}/{@code TieredHitTracker}
 * already establish the pattern for in this project) — this deliberately freezes the entity facing
 * whatever direction it happened to be looking the instant it got trapped, not some fixed default
 * direction. Every tick after that, the snapshot is force-written back (including the {@code *O}
 * previous-tick fields, to avoid a one-frame interpolated jitter), and the snapshot is cleared once
 * the effect ends so a future re-application starts a fresh freeze-frame instead of reusing a stale
 * one.
 *
 * <p>Caveat, worth surfacing rather than silently under-delivering on: this only locks server-side
 * rotation state — what other entities/players see, and what hit/attack resolution reads. An
 * immobilized <em>player</em> target can still visually free-look with their own camera, since
 * view/camera control is client-authoritative and vanilla has no hook to override it either (a real,
 * general Minecraft limitation, not a gap in this fix). From every other entity's perspective, and
 * for server-side combat resolution, the freeze is real and complete either way.
 */
@Mixin(LivingEntity.class)
public abstract class ImmobilizedRotationLockMixin {
	private static final AttachmentType<float[]> ROTATION_SNAPSHOT = AttachmentRegistry.create(
			new ResourceLocation("monster_origins", "immobilize_rotation_snapshot"));

	@Inject(method = "tick", at = @At("TAIL"))
	private void immobilized$lockRotation(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			return;
		}
		if (!self.hasEffect(ModEffects.IMMOBILIZED)) {
			if (self.hasAttached(ROTATION_SNAPSHOT)) {
				self.removeAttached(ROTATION_SNAPSHOT);
			}
			return;
		}

		float[] snapshot = self.getAttached(ROTATION_SNAPSHOT);
		if (snapshot == null) {
			snapshot = new float[] { self.getYRot(), self.getXRot(), self.getYHeadRot(), self.yBodyRot };
			self.setAttached(ROTATION_SNAPSHOT, snapshot);
		}

		self.setYRot(snapshot[0]);
		self.yRotO = snapshot[0];
		self.setXRot(snapshot[1]);
		self.xRotO = snapshot[1];
		self.setYHeadRot(snapshot[2]);
		self.yHeadRotO = snapshot[2];
		self.setYBodyRot(snapshot[3]);
		self.yBodyRotO = snapshot[3];
	}
}
