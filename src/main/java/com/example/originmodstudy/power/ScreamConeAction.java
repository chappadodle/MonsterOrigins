package com.example.originmodstudy.power;

import com.example.originmodstudy.OriginModStudy;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Harpy's Scream (data/arachne/powers/harpy/scream.json): knock back whatever is in a forward
 * cone from the caster, ignoring anything behind them. Registered as its own Apoli
 * {@code EntityActionType} ({@code arachne:cone_knockback}) rather than expressed in JSON,
 * because nothing data-driven can do this — confirmed by reading Apoli's own source:
 * {@code relative_rotation} compares the actor's and target's own independent facing directions
 * to each other, not "is the target positioned in front of the actor"; {@code area_of_effect}'s
 * shapes are only cube/star/sphere, no cone. The knockback direction (radiating outward from the
 * caster's own position) also isn't expressible via the generic {@code add_velocity} action,
 * which only takes a fixed vector.
 */
public final class ScreamConeAction {
	private ScreamConeAction() {
	}

	public static void register() {
		Registry.register(
				ApoliRegistries.ENTITY_ACTION,
				OriginModStudy.id("cone_knockback"),
				new ActionFactory<Entity>(
						OriginModStudy.id("cone_knockback"),
						new SerializableData()
								.add("radius", SerializableDataTypes.DOUBLE, 8.0)
								.add("cone_angle", SerializableDataTypes.DOUBLE, 90.0)
								.add("strength", SerializableDataTypes.FLOAT, 1.2F)
								.add("vertical_strength", SerializableDataTypes.FLOAT, 0.3F),
						ScreamConeAction::apply
				)
		);
	}

	private static void apply(SerializableData.Instance data, Entity entity) {
		if (!(entity instanceof LivingEntity caster) || entity.level().isClientSide()) {
			return;
		}

		double radius = data.getDouble("radius");
		double coneAngleDeg = data.getDouble("cone_angle");
		float strength = data.getFloat("strength");
		float verticalStrength = data.getFloat("vertical_strength");

		double coneCos = Math.cos(Math.toRadians(coneAngleDeg / 2.0));
		Vec3 look = caster.getLookAngle();
		Level level = caster.level();
		AABB box = caster.getBoundingBox().inflate(radius);

		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
				candidate -> candidate != caster && candidate.distanceToSqr(caster) <= radius * radius);

		for (LivingEntity target : nearby) {
			Vec3 toTarget = target.position().subtract(caster.position());
			double dist = toTarget.length();
			if (dist < 1.0e-4) {
				continue;
			}
			Vec3 direction = toTarget.scale(1.0 / dist);
			if (direction.dot(look) < coneCos) {
				// Behind (or too far to the side of) the caster — Scream doesn't reach here.
				continue;
			}
			Vec3 push = direction.scale(strength).add(0, verticalStrength, 0);
			target.setDeltaMovement(target.getDeltaMovement().add(push));
			target.hurtMarked = true;
		}
	}
}
