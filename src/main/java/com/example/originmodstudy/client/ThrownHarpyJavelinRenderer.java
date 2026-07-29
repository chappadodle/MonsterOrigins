package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ThrownJavelin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a flying or landed Harpy Javelin (Storm Trident) using vanilla's real Trident geometry,
 * oriented along its flight direction — see {@link DirectionalThrownItemRenderer} for the full
 * explanation (this used to extend {@code ThrownItemRenderer}, which billboards toward the
 * camera instead, causing the javelin to always render "vertical" regardless of throw direction).
 */
public class ThrownHarpyJavelinRenderer extends DirectionalThrownItemRenderer<ThrownJavelin> {
	public ThrownHarpyJavelinRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected ResourceLocation getTexture() {
		return HarpyJavelinRenderer.TEXTURE;
	}
}
