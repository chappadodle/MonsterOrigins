package com.example.originmodstudy.client;

import com.example.originmodstudy.entity.ThrownMermaidTrident;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders a flying or landed Living Coral Trident using vanilla's real Trident geometry, oriented
 * along its flight direction — see {@link DirectionalThrownItemRenderer} for the full explanation.
 */
public class ThrownMermaidTridentRenderer extends DirectionalThrownItemRenderer<ThrownMermaidTrident> {
	public ThrownMermaidTridentRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected ResourceLocation getTexture() {
		return MermaidTridentRenderer.TEXTURE;
	}
}
