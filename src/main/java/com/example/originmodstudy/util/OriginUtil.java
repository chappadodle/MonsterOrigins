package com.example.originmodstudy.util;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Same Origins-API lookup {@code ArthropodPassiveTargetMixin} uses, pulled out so weapon items
 * can share it instead of duplicating the {@code ModComponents.ORIGIN}/{@code OriginLayers}
 * boilerplate.
 */
public final class OriginUtil {
	private static final ResourceLocation ORIGIN_LAYER_ID = new ResourceLocation("origins", "origin");

	private OriginUtil() {
	}

	/** True if {@code entity} is a player who has chosen the origin identified by {@code originId}. */
	public static boolean hasOrigin(LivingEntity entity, ResourceLocation originId) {
		if (!(entity instanceof Player)) {
			return false;
		}
		Player player = (Player) entity;
		ComponentKey<OriginComponent> originKey = ModComponents.ORIGIN;
		OriginComponent originComponent = originKey.get(player);
		OriginLayer layer = OriginLayers.getLayer(ORIGIN_LAYER_ID);
		if (layer == null || !originComponent.hasOrigin(layer)) {
			return false;
		}
		Origin origin = originComponent.getOrigin(layer);
		return origin.getIdentifier().equals(originId);
	}

	/** Shared styling for every origin-gated item's tooltip (Fang tiers, Petrifying Trident,
	 * Harpy Javelin) so a player can tell from the tooltip alone whose weapon this really is. */
	public static void addOriginGatedTooltip(List<Component> tooltip, String text) {
		tooltip.add(Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
	}
}
